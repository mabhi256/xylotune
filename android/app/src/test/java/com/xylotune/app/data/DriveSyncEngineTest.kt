package com.xylotune.app.data

import com.xylotune.app.model.Line
import com.xylotune.app.model.Song
import com.xylotune.app.model.SyncFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported cases for mergeSyncState (index.html lines ~491-507): per-song "latest event (a
// save or a delete) wins," never both a song and a tombstone on the same side for one name.
class DriveSyncEngineTest {

    private fun song(updatedAt: Long) = Song(lines = listOf(Line()), updatedAt = updatedAt)

    @Test
    fun `a local-only song survives a merge with no remote file`() {
        val merged = mergeSyncState(mapOf("Twinkle" to song(100)), emptyMap(), remote = null)
        assertEquals(setOf("Twinkle"), merged.songs.keys)
        assertTrue(merged.tombstones.isEmpty())
    }

    @Test
    fun `a remote-only song is pulled in`() {
        val remote = SyncFile(songs = mapOf("Twinkle" to song(100)))
        val merged = mergeSyncState(emptyMap(), emptyMap(), remote)
        assertEquals(setOf("Twinkle"), merged.songs.keys)
    }

    @Test
    fun `the newer of two conflicting song edits wins`() {
        val remote = SyncFile(songs = mapOf("Twinkle" to song(50)))
        val merged = mergeSyncState(mapOf("Twinkle" to song(100)), emptyMap(), remote)
        assertEquals(100L, merged.songs.getValue("Twinkle").updatedAt)

        val remoteNewer = SyncFile(songs = mapOf("Twinkle" to song(150)))
        val merged2 = mergeSyncState(mapOf("Twinkle" to song(100)), emptyMap(), remoteNewer)
        assertEquals(150L, merged2.songs.getValue("Twinkle").updatedAt)
    }

    @Test
    fun `a later remote delete beats an earlier local song`() {
        val remote = SyncFile(tombstones = mapOf("Twinkle" to 200))
        val merged = mergeSyncState(mapOf("Twinkle" to song(100)), emptyMap(), remote)
        assertFalse(merged.songs.containsKey("Twinkle"))
        assertEquals(200L, merged.tombstones.getValue("Twinkle"))
    }

    @Test
    fun `an earlier local delete loses to a later remote song`() {
        val remote = SyncFile(songs = mapOf("Twinkle" to song(200)))
        val merged = mergeSyncState(emptyMap(), mapOf("Twinkle" to 100), remote)
        assertEquals(200L, merged.songs.getValue("Twinkle").updatedAt)
        assertFalse(merged.tombstones.containsKey("Twinkle"))
    }

    @Test
    fun `a name is never both a song and a tombstone in the result`() {
        val remote = SyncFile(songs = mapOf("A" to song(100)), tombstones = mapOf("B" to 100))
        val merged = mergeSyncState(
            mapOf("A" to song(50), "B" to song(50)),
            mapOf("A" to 10, "B" to 10),
            remote,
        )
        for (name in merged.songs.keys) assertFalse(merged.tombstones.containsKey(name))
        for (name in merged.tombstones.keys) assertFalse(merged.songs.containsKey(name))
    }

    @Test
    fun `both tombstones agreeing on a deletion keep the later timestamp`() {
        val remote = SyncFile(tombstones = mapOf("Old" to 300))
        val merged = mergeSyncState(emptyMap(), mapOf("Old" to 100), remote)
        assertEquals(300L, merged.tombstones.getValue("Old"))
    }

    @Test
    fun `a tombstone dated exactly epoch 0 is treated as absent, matching the web's falsy check`() {
        // index.html reads `localTombstones[name] || -1` — 0 is falsy in JS, so a
        // (never-realistic) epoch-0 tombstone loses to any real remote song, even an old one.
        val remote = SyncFile(songs = mapOf("X" to song(1)))
        val merged = mergeSyncState(emptyMap(), mapOf("X" to 0L), remote)
        assertEquals(1L, merged.songs.getValue("X").updatedAt)
    }
}
