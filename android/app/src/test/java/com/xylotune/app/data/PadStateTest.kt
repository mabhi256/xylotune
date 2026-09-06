package com.xylotune.app.data

import com.xylotune.app.model.Line
import com.xylotune.app.model.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported cases from index.html's newLine()/backspace()/forwardDelete()/hardDelete()/
// addDot()/moveCursor() (lines ~1144-1254). The old strike()'s own delta-gap-continuation
// timing moved to LiveCapture (see LiveCaptureTest) — appendLiveNote/adjustLastNoteTicks
// here are its two underlying primitives, exercised directly rather than through a clock.
class PadStateTest {

    @Test
    fun `appendLiveNote appends a note and advances the cursor`() {
        val pad = PadState()
        pad.appendLiveNote(2, ticks = 24)
        assertEquals(1, pad.lines[0].notes.size)
        assertEquals(2, pad.lines[0].notes[0].i)
        assertEquals(24, pad.lines[0].notes[0].ticks)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `adjustLastNoteTicks rewrites only the most recently appended note`() {
        val pad = PadState()
        pad.appendLiveNote(0, ticks = 24)
        pad.appendLiveNote(1, ticks = 24)
        pad.adjustLastNoteTicks(48)
        assertEquals(24, pad.lines[0].notes[0].ticks) // untouched
        assertEquals(48, pad.lines[0].notes[1].ticks)
    }

    @Test
    fun `toModel derives sec from ticks at the pad's own tempo`() {
        val pad = PadState()
        pad.changeBpm(120) // one tick = 60 / (120 * 24) s
        pad.appendLiveNote(0, ticks = 24) // one quarter note
        val sec = pad.toModel()[0].notes[0].sec
        assertEquals(0.5, sec, 1e-9) // a quarter at 120bpm is exactly half a second
    }

    @Test
    fun `newLine splits notes at the cursor onto a new line`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.appendLiveNote(1, 24)
        pad.appendLiveNote(2, 24) // cursor now at end of line 0, 3 notes
        pad.placeCursor(0, 1) // between note 0 and note 1
        pad.newLine()
        assertEquals(2, pad.lines.size)
        assertEquals(1, pad.lines[0].notes.size)
        assertEquals(2, pad.lines[1].notes.size)
        assertEquals(1, pad.lines[1].notes[0].i)
        assertEquals(2, pad.lines[1].notes[1].i)
        assertEquals(Cursor(1, 0), pad.cursor)
    }

    @Test
    fun `backspace shrinks a held note by one grid step before deleting`() {
        val pad = PadState()
        pad.appendLiveNote(0, ticks = 2 * TICKS_PER_DOT) // held two grid steps
        pad.placeCursor(0, 1)
        pad.backspace()
        assertEquals(1, pad.lines[0].notes.size) // note still there, hold just shrank
        assertEquals(TICKS_PER_DOT, pad.lines[0].notes[0].ticks)
        pad.backspace() // now actually deletes it
        assertTrue(pad.lines[0].notes.isEmpty())
        assertEquals(Cursor(0, 0), pad.cursor)
    }

    @Test
    fun `backspace at line start merges into the previous line`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.newLine()
        pad.appendLiveNote(1, 24)
        pad.placeCursor(1, 0)
        pad.backspace()
        assertEquals(1, pad.lines.size)
        assertEquals(2, pad.lines[0].notes.size)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `forwardDelete shrinks a held note before deleting, hardDelete always deletes`() {
        val fwd = PadState()
        fwd.appendLiveNote(0, ticks = 2 * TICKS_PER_DOT)
        fwd.placeCursor(0, 0)
        fwd.forwardDelete()
        assertEquals(1, fwd.lines[0].notes.size)
        assertEquals(TICKS_PER_DOT, fwd.lines[0].notes[0].ticks)

        val hard = PadState()
        hard.appendLiveNote(0, ticks = 2 * TICKS_PER_DOT) // hardDelete skips the shrink step entirely
        hard.placeCursor(0, 0)
        hard.hardDelete()
        assertTrue(hard.lines[0].notes.isEmpty())
    }

    @Test
    fun `addHold lengthens the note before the cursor by one grid step`() {
        val pad = PadState()
        pad.appendLiveNote(0, ticks = TICKS_PER_DOT)
        pad.addHold()
        assertEquals(2 * TICKS_PER_DOT, pad.lines[0].notes[0].ticks)
    }

    @Test
    fun `moveCursor Right wraps onto the next line, Left wraps back`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.newLine()
        pad.appendLiveNote(1, 24)
        pad.placeCursor(0, 1) // end of line 0
        pad.moveCursor(CursorDirection.Right)
        assertEquals(Cursor(1, 0), pad.cursor)
        pad.moveCursor(CursorDirection.Left)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `hasAnyNotes is false for an empty pad and true after a note`() {
        val pad = PadState()
        assertFalse(pad.hasAnyNotes())
        pad.appendLiveNote(0, 24)
        assertTrue(pad.hasAnyNotes())
    }

    @Test
    fun `setLyricText stores the text and strips any existing tags on the line`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.linkLyric(0, noteStart = 0, noteEnd = 0, charStart = 0, charEnd = 2)
        pad.setLyricText(0, "hi there")
        assertEquals("hi there", pad.lines[0].lyric)
        assertNull(pad.lines[0].notes[0].lyricStart) // any edit voids the line's tags
    }

    @Test
    fun `linkLyric anchors a tag on the given note range`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.appendLiveNote(1, 24)
        pad.setLyricText(0, "hi there")
        pad.linkLyric(0, noteStart = 0, noteEnd = 1, charStart = 0, charEnd = 2)
        assertEquals(0, pad.lines[0].notes[0].lyricStart)
        assertEquals(2, pad.lines[0].notes[0].lyricEnd)
        assertEquals(2, pad.lines[0].notes[0].lyricSpan)
    }

    @Test
    fun `unlinkLyric clears exactly the tag anchored at the given note`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.setLyricText(0, "hi")
        pad.linkLyric(0, noteStart = 0, noteEnd = 0, charStart = 0, charEnd = 2)
        pad.unlinkLyric(0, 0)
        assertNull(pad.lines[0].notes[0].lyricStart)
    }

    @Test
    fun `loadSong and toModel round-trip through the Line model at the same tempo`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.appendLiveNote(1, 24)
        val model = pad.toModel() // sec baked in at pad's own bpm (100)

        val loaded = PadState()
        loaded.loadSong(model, songBpm = pad.bpm, songMeter = listOf(3, 4))
        assertEquals(model, loaded.toModel()) // same bpm in and out -> sec matches exactly too
        assertEquals(Cursor(0, 2), loaded.cursor) // lands at the end, like onTuneChange did
    }

    @Test
    fun `loading a song at a different tempo keeps ticks but recomputes sec`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24) // one quarter note
        val model = pad.toModel() // at bpm 100: sec = 0.6

        val loaded = PadState()
        loaded.loadSong(model, songBpm = 90, songMeter = listOf(3, 4))
        assertEquals(90, loaded.bpm)
        assertEquals(listOf(3, 4), loaded.meter)
        assertEquals(24, loaded.lines[0].notes[0].ticks) // duration in ticks is tempo-independent
        assertEquals(60.0 / 90, loaded.toModel()[0].notes[0].sec, 1e-9) // but sec is recomputed at the new tempo
    }

    @Test
    fun `loadSong reads a legacy note lacking ticks by migrating from sec`() {
        val legacy = listOf(Line(notes = listOf(NoteEvent(i = 3, sec = 0.3)))) // 3 legacy dots
        val pad = PadState()
        pad.loadSong(legacy, songBpm = 150, songMeter = listOf(4, 4))
        assertEquals(3 * TICKS_PER_DOT, pad.lines[0].notes[0].ticks)
    }

    @Test
    fun `newSong resets tempo to this app's own defaults, not Song's legacy-decode default`() {
        val pad = PadState()
        pad.appendLiveNote(0, 24)
        pad.changeBpm(180)
        pad.newSong()
        assertTrue(pad.isEmpty)
        assertEquals(100, pad.bpm)
        assertEquals(listOf(4, 4), pad.meter)
    }

    @Test
    fun `setBpm clamps to the 40 to 240 range`() {
        val pad = PadState()
        pad.changeBpm(500)
        assertEquals(240, pad.bpm)
        pad.changeBpm(1)
        assertEquals(40, pad.bpm)
    }
}
