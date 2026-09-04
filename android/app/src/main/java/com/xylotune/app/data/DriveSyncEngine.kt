package com.xylotune.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xylotune.app.model.Song
import com.xylotune.app.model.SyncFile

enum class DriveStatus { Disconnected, Connecting, Syncing, Connected, Error }

data class MergedSyncState(val songs: Map<String, Song>, val tombstones: Map<String, Long>)

// Ported from index.html's mergeSyncState (lines ~491-507): per-song "latest event (a save
// or a delete) wins." A name can only ever be a live song on one side and a tombstone on
// the other (or both/neither), never both a song and a tombstone on the same side, so
// there's no ambiguity to break.
fun mergeSyncState(localSongs: Map<String, Song>, localTombstones: Map<String, Long>, remote: SyncFile?): MergedSyncState {
    val remoteSongs = remote?.songs ?: emptyMap()
    val remoteTombstones = remote?.tombstones ?: emptyMap()
    val names = localSongs.keys + remoteSongs.keys + localTombstones.keys + remoteTombstones.keys

    val songs = mutableMapOf<String, Song>()
    val tombstones = mutableMapOf<String, Long>()
    for (name in names) {
        val lu = localSongs[name]?.updatedAt ?: -1L
        val ru = remoteSongs[name]?.updatedAt ?: -1L
        val ld = localTombstones[name].jsTruthyOr(-1L)
        val rd = remoteTombstones[name].jsTruthyOr(-1L)
        val latest = maxOf(lu, ru, ld, rd)
        if (latest < 0) continue
        if (latest == ld || latest == rd) {
            tombstones[name] = maxOf(ld, rd)
        } else {
            songs[name] = if (latest == lu) localSongs.getValue(name) else remoteSongs.getValue(name)
        }
    }
    return MergedSyncState(songs, tombstones)
}

// Mirrors JS's `value || fallback`: index.html reads `localTombstones[name] || -1`, where a
// tombstone dated exactly the epoch (0) is falsy in JS and falls through to -1, same as a
// missing entry. Reproduced verbatim (not "fixed") for byte-for-byte interop with the web's
// own merge output — this can only ever matter for a timestamp no real device produces.
private fun Long?.jsTruthyOr(fallback: Long): Long = if (this != null && this != 0L) this else fallback

/**
 * Ported from index.html's connectDrive/syncWithDrive/pushToDrive (lines ~483-552). One
 * mechanism-level difference from the web: there's no pasted Client ID to manage or store —
 * see DriveAuthManager for why Android's AuthorizationClient needs none.
 */
class DriveSyncEngine(
    private val context: Context,
    private val auth: DriveAuthManager,
    private val api: DriveApi = DriveApi(),
) {
    var status: DriveStatus by mutableStateOf(DriveStatus.Disconnected)
        private set

    private var accessToken: String? = null
    private var driveFileId: String? = null

    /** Called once on launch — mirrors the web's silent-reconnect-on-load effect; never shows UI. */
    suspend fun trySilentReconnect(onSongsUpdated: (Map<String, Song>) -> Unit) {
        if (!loadDriveConnected(context)) return
        val token = auth.trySilent()
        if (token == null) {
            saveDriveConnected(context, false)
            return
        }
        accessToken = token
        status = DriveStatus.Syncing
        try {
            syncNow(onSongsUpdated)
            status = DriveStatus.Connected
        } catch (e: Exception) {
            status = DriveStatus.Disconnected
            saveDriveConnected(context, false)
            accessToken = null
        }
    }

    /** Explicit user tap on "Connect Drive" — may show a consent screen. */
    suspend fun connect(onSongsUpdated: (Map<String, Song>) -> Unit) {
        status = DriveStatus.Connecting
        try {
            accessToken = auth.authorizeInteractive()
            saveDriveConnected(context, true)
            status = DriveStatus.Syncing
            syncNow(onSongsUpdated)
            status = DriveStatus.Connected
        } catch (e: Exception) {
            status = DriveStatus.Disconnected
            saveDriveConnected(context, false)
            accessToken = null
        }
    }

    fun disconnect() {
        saveDriveConnected(context, false)
        accessToken = null
        driveFileId = null
        status = DriveStatus.Disconnected
    }

    /** Pulls the remote file (if any), merges it with local state, writes the result back both ways. */
    private suspend fun syncNow(onSongsUpdated: (Map<String, Song>) -> Unit) {
        val fileId = driveFileId ?: withTokenRetry { token -> api.findSyncFileId(token) }
        val remote = fileId?.let { id -> withTokenRetry { token -> api.downloadSyncFile(id, token) } }
        val localSongs = SongRepository.loadSongs(context)
        val localTombstones = SongRepository.loadTombstones(context)
        val merged = mergeSyncState(localSongs, localTombstones, remote)
        SongRepository.saveSongs(context, merged.songs)
        SongRepository.saveTombstones(context, merged.tombstones)
        onSongsUpdated(merged.songs)
        driveFileId = withTokenRetry { token -> api.uploadSyncFile(fileId, SyncFile(merged.songs, merged.tombstones), token) }
    }

    /**
     * Fire-and-forget upload after a local save/delete — local storage is already the
     * source of truth by the time this runs, so a failure here just leaves Drive stale
     * until the next successful sync, exactly like the web's pushToDrive.
     */
    suspend fun push(songs: Map<String, Song>, tombstones: Map<String, Long>) {
        if (!loadDriveConnected(context)) return
        if (accessToken == null) accessToken = auth.trySilent()
        val token = accessToken
        if (token == null) {
            status = DriveStatus.Error
            return
        }
        try {
            status = DriveStatus.Syncing
            val fileId = driveFileId ?: withTokenRetry { t -> api.findSyncFileId(t) }
            driveFileId = withTokenRetry { t -> api.uploadSyncFile(fileId, SyncFile(songs, tombstones), t) }
            status = DriveStatus.Connected
        } catch (e: Exception) {
            status = DriveStatus.Error
        }
    }

    // One retry on a 401, via a silent (no-UI) token refresh — covers the token simply
    // expiring mid-session, mirroring the web's driveFetch retry.
    private suspend fun <T> withTokenRetry(block: suspend (String) -> T): T {
        val token = accessToken ?: error("Not authorized")
        return try {
            block(token)
        } catch (e: DriveApiException) {
            if (e.statusCode == 401) {
                val refreshed = auth.trySilent() ?: throw e
                accessToken = refreshed
                block(refreshed)
            } else {
                throw e
            }
        }
    }
}
