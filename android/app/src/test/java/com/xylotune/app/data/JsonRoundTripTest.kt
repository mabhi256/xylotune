package com.xylotune.app.data

import com.xylotune.app.model.Line
import com.xylotune.app.model.NoteEvent
import com.xylotune.app.model.Song
import com.xylotune.app.model.SyncFile
import com.xylotune.app.util.XyloJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

// Fixtures below are written exactly as index.html's own JSON.stringify would produce
// them — compiling successfully proves nothing about wire format, only these exact-string
// assertions catch a field-type or encodeDefaults/explicitNulls regression that would
// silently break interop with the web app's Drive file.
class JsonRoundTripTest {

    @Test
    fun `plain note omits rest and lyric-link keys entirely`() {
        val json = """{"i":3,"sec":0.4}"""
        val note = XyloJson.decodeFromString<NoteEvent>(json)
        assertEquals(NoteEvent(i = 3, sec = 0.4), note)
        assertEquals(json, XyloJson.encodeToString(note))
    }

    @Test
    fun `rest note omits i but keeps explicit rest true`() {
        val json = """{"rest":true,"sec":0.4}"""
        val note = XyloJson.decodeFromString<NoteEvent>(json)
        assertEquals(NoteEvent(rest = true, sec = 0.4), note)
        assertEquals(json, XyloJson.encodeToString(note))
    }

    @Test
    fun `note with a lyric link round-trips every offset field`() {
        val json = """{"i":5,"sec":0.8,"lyricStart":0,"lyricEnd":4,"lyricSpan":2}"""
        val note = XyloJson.decodeFromString<NoteEvent>(json)
        assertEquals(NoteEvent(i = 5, sec = 0.8, lyricStart = 0, lyricEnd = 4, lyricSpan = 2), note)
        assertEquals(json, XyloJson.encodeToString(note))
    }

    @Test
    fun `line round-trips notes and lyric text`() {
        val json = """{"notes":[{"i":2,"sec":0.4},{"i":5,"sec":0.8,"lyricStart":0,"lyricEnd":4,"lyricSpan":2}],"lyric":"hello there"}"""
        val line = XyloJson.decodeFromString<Line>(json)
        assertEquals(2, line.notes.size)
        assertEquals("hello there", line.lyric)
        assertEquals(json, XyloJson.encodeToString(line))
    }

    @Test
    fun `song updatedAt is a bare integer, never scientific notation`() {
        val json = """{"lines":[{"notes":[{"i":0,"sec":0.4}],"lyric":""}],"updatedAt":1737400000000}"""
        val song = XyloJson.decodeFromString<Song>(json)
        assertEquals(1737400000000L, song.updatedAt)
        assertEquals(json, XyloJson.encodeToString(song))
    }

    @Test
    fun `sync file round-trips songs and tombstones together`() {
        val json =
            """{"songs":{"Twinkle":{"lines":[{"notes":[{"i":0,"sec":0.4}],"lyric":""}],"updatedAt":100}},""" +
                """"tombstones":{"OldSong":50}}"""
        val sync = XyloJson.decodeFromString<SyncFile>(json)
        assertEquals(setOf("Twinkle"), sync.songs.keys)
        assertEquals(setOf("OldSong"), sync.tombstones.keys)
        assertEquals(json, XyloJson.encodeToString(sync))
    }

    @Test
    fun `an empty new line still emits both notes and lyric keys`() {
        val json = """{"notes":[],"lyric":""}"""
        val line = XyloJson.decodeFromString<Line>(json)
        assertEquals(Line(), line)
        assertEquals(json, XyloJson.encodeToString(line))
    }

    @Test
    fun `an empty sync file still emits both songs and tombstones keys`() {
        val json = """{"songs":{},"tombstones":{}}"""
        val sync = XyloJson.decodeFromString<SyncFile>(json)
        assertEquals(SyncFile(), sync)
        assertEquals(json, XyloJson.encodeToString(sync))
    }

    @Test
    fun `unknown keys from a future web-only field are ignored, not fatal`() {
        val json = """{"i":1,"sec":0.4,"someFutureWebField":"x"}"""
        val note = XyloJson.decodeFromString<NoteEvent>(json)
        assertEquals(NoteEvent(i = 1, sec = 0.4), note)
    }
}
