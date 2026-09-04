package com.xylotune.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.data.DriveSyncEngine
import com.xylotune.app.data.PadState
import com.xylotune.app.data.SongRepository
import com.xylotune.app.data.loadSoundPref
import com.xylotune.app.data.rememberDriveAuthManager
import com.xylotune.app.data.saveSoundPref
import com.xylotune.app.model.Song
import com.xylotune.app.player.Playback
import com.xylotune.app.player.PracticeEngine
import com.xylotune.app.ui.composer.PadScreen
import com.xylotune.app.ui.composer.PlayTab
import com.xylotune.app.ui.dialogs.ActionDialogHost
import com.xylotune.app.ui.dialogs.DialogHost
import kotlinx.coroutines.launch

private enum class Tab { Play, Sheet }

/**
 * Play/Sheet, hoisting the PadState/Playback/DriveSyncEngine every tab shares. No Scaffold,
 * no top app bar or bottom nav: each tab is full-bleed, with its own song/save/delete/
 * tab-switch/Drive controls floating as HUD chips (see PlayTab/PadScreen) — a persistent
 * title bar plus a labeled NavigationBar cost real, fixed dp that a fixed-height board no
 * longer has to share a short landscape viewport with, but every other tab still paid for.
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogHost = remember { DialogHost() }

    var material by remember { mutableStateOf(loadSoundPref(context)) }
    val pad = remember { PadState() }
    val playback = remember {
        Playback(pad = pad, scope = scope, onPlayNote = { noteIndex, _ -> AudioEngine.playNote(noteIndex, material) })
            .also { p -> pad.onBeforeEdit = { p.finish() } }
    }
    val practice = remember { PracticeEngine(pad) }

    val driveAuth = rememberDriveAuthManager()
    val driveSync = remember { DriveSyncEngine(context, driveAuth) }

    var savedSongs by remember { mutableStateOf(SongRepository.loadSongs(context)) }
    var currentSongName by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(Tab.Play) }

    // Mirrors the web's silent-reconnect-on-load effect: only ever fires once, never shows
    // any UI — if the grant no longer holds, this just leaves the button in Disconnected
    // state for the user to reconnect by hand (see DriveAuthManager.trySilent).
    LaunchedEffect(Unit) {
        driveSync.trySilentReconnect { updated -> savedSongs = updated }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (tab) {
            Tab.Play -> PlayTab(
                pad = pad,
                playback = playback,
                practice = practice,
                material = material,
                onMaterialChange = {
                    material = it
                    saveSoundPref(context, it)
                },
                songNames = savedSongs.keys.toList(),
                currentSongName = currentSongName,
                onSelectSong = { name ->
                    if (!practice.active) {
                        savedSongs[name]?.let { song ->
                            playback.finish()
                            pad.replaceAll(song.lines)
                            currentSongName = name
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        saveSong(pad, dialogHost, driveSync, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context)
                    }
                },
                onSwitchToSheet = { tab = Tab.Sheet },
                driveStatus = driveSync.status,
                onDriveClick = {
                    scope.launch { onDriveButtonClick(driveSync, dialogHost) { updated -> savedSongs = updated } }
                },
            )
            Tab.Sheet -> PadScreen(
                pad = pad,
                material = material,
                playback = playback,
                practice = practice,
                canDeleteSong = currentSongName != null,
                onDeleteSong = {
                    scope.launch {
                        deleteSong(pad, playback, dialogHost, driveSync, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context)
                    }
                },
                onSwitchToPlay = { tab = Tab.Play },
                songName = currentSongName,
            )
        }
        ActionDialogHost(dialogHost)
    }
}

// Ported from index.html's onDriveButtonClick (lines ~1553-1564): connected/error means a
// tap disconnects (after confirming); anything else means a tap tries to connect.
private suspend fun onDriveButtonClick(driveSync: DriveSyncEngine, dialogHost: DialogHost, onSongsUpdated: (Map<String, Song>) -> Unit) {
    if (driveSync.status == DriveStatus.Connected || driveSync.status == DriveStatus.Error) {
        val ok = dialogHost.askConfirm("Disconnect Google Drive? Your songs stay saved on this device.", confirmLabel = "Disconnect")
        if (ok) driveSync.disconnect()
        return
    }
    driveSync.connect(onSongsUpdated)
}

// Ported from index.html's saveSong() (lines ~1440-1458): prompt for a name, confirm
// overwrite if it collides with a different existing song, persist locally, then push to
// Drive (fire-and-forget, matching the web's own pushToDrive call).
private suspend fun saveSong(
    pad: PadState,
    dialogHost: DialogHost,
    driveSync: DriveSyncEngine,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: Context,
) {
    if (!pad.hasAnyNotes()) {
        dialogHost.showAlert("Play or write some notes before saving a song.")
        return
    }
    val name = dialogHost.askPrompt("Song name:", currentSongName ?: "") ?: return
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    if (savedSongs.containsKey(trimmed) && trimmed != currentSongName) {
        val ok = dialogHost.askConfirm("\"$trimmed\" already exists. Overwrite it?", confirmLabel = "Overwrite")
        if (!ok) return
    }
    val next = savedSongs + (trimmed to Song(lines = pad.toModel(), updatedAt = System.currentTimeMillis()))
    setSavedSongs(next)
    SongRepository.saveSongs(context, next)
    val tombstones = SongRepository.loadTombstones(context)
    val nextTombstones = if (tombstones.containsKey(trimmed)) tombstones - trimmed else tombstones
    if (nextTombstones !== tombstones) SongRepository.saveTombstones(context, nextTombstones)
    setCurrentSongName(trimmed)
    driveSync.push(next, nextTombstones)
}

// Ported from index.html's deleteSong() (lines ~1459-1477): confirm, persist a tombstone
// (not just remove the key), clear the pad back to empty, then push to Drive.
private suspend fun deleteSong(
    pad: PadState,
    playback: Playback,
    dialogHost: DialogHost,
    driveSync: DriveSyncEngine,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: Context,
) {
    val name = currentSongName ?: return
    val ok = dialogHost.askConfirm("Delete \"$name\"?", confirmLabel = "Delete", danger = true)
    if (!ok) return
    val next = savedSongs - name
    setSavedSongs(next)
    SongRepository.saveSongs(context, next)
    val tombstones = SongRepository.loadTombstones(context) + (name to System.currentTimeMillis())
    SongRepository.saveTombstones(context, tombstones)
    setCurrentSongName(null)
    playback.finish()
    pad.replaceAll(emptyList())
    driveSync.push(next, tombstones)
}
