package com.xylotune.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported cases from index.html's strike()/newLine()/backspace()/forwardDelete()/
// hardDelete()/addDot()/moveCursor() (lines ~1144-1254) and the relevant behaviors
// tests/xylotune.spec.js exercises through the DOM.
class PadStateTest {

    @Test
    fun `strike appends a note and advances the cursor`() {
        val pad = PadState()
        pad.strike(2, nowMs = 0)
        assertEquals(1, pad.lines[0].notes.size)
        assertEquals(2, pad.lines[0].notes[0].i)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `consecutive live strikes capture real elapsed time as the previous note's gap`() {
        val pad = PadState()
        pad.strike(0, nowMs = 1000)
        pad.strike(1, nowMs = 1300)
        assertEquals(0.3, pad.lines[0].notes[0].sec, 1e-9)
    }

    @Test
    fun `live gap is clamped to MIN_LIVE_GAP_SEC and MAX_PAUSE_SEC`() {
        val fast = PadState()
        fast.strike(0, nowMs = 0)
        fast.strike(1, nowMs = 1) // 1ms gap, far below MIN_LIVE_GAP_SEC
        assertEquals(MIN_LIVE_GAP_SEC, fast.lines[0].notes[0].sec, 1e-9)

        val slow = PadState()
        slow.strike(0, nowMs = 0)
        slow.strike(1, nowMs = 5000) // 5s gap, above MAX_PAUSE_SEC but below PHRASE_BREAK_SEC's line-split...
        assertEquals(MAX_PAUSE_SEC, slow.lines[0].notes[0].sec, 1e-9)
    }

    @Test
    fun `a pause beyond PHRASE_BREAK_SEC starts a new line`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.strike(1, nowMs = 2000) // 2s gap > PHRASE_BREAK_SEC (1.2s)
        assertEquals(2, pad.lines.size)
        assertEquals(1, pad.lines[0].notes.size)
        assertEquals(1, pad.lines[1].notes.size)
        assertEquals(MAX_PAUSE_SEC, pad.lines[0].notes[0].sec, 1e-9) // still clamped, despite the split
        assertEquals(Cursor(1, 1), pad.cursor)
    }

    @Test
    fun `editing between two strikes breaks live capture for the next strike`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0) // sec = QUARTER_SEC = 4 * REST_UNIT_SEC (dotCount 4)
        pad.addDot() // any edit nulls lastLiveNote; grows note 0 to 5 dots
        pad.strike(1, nowMs = 50) // would be MIN_LIVE_GAP_SEC-clamped if still "live"
        // addDot already grew note 0's gap; the second strike must not have touched it again.
        assertEquals(5 * REST_UNIT_SEC, pad.lines[0].notes[0].sec, 1e-9)
    }

    @Test
    fun `newLine splits notes at the cursor onto a new line`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.strike(1, nowMs = 100)
        pad.strike(2, nowMs = 200) // cursor now at end of line 0, 3 notes
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
    fun `backspace shrinks a multi-dot gap before deleting the note`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.setGapSec(0, 0, 2 * REST_UNIT_SEC) // exactly 2 dots
        pad.placeCursor(0, 1)
        pad.backspace()
        assertEquals(1, pad.lines[0].notes.size) // note still there, gap just shrank
        assertEquals(1, dotCount(pad.lines[0].notes[0]))
        pad.backspace() // now actually deletes it
        assertTrue(pad.lines[0].notes.isEmpty())
        assertEquals(Cursor(0, 0), pad.cursor)
    }

    @Test
    fun `backspace at line start merges into the previous line`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.newLine()
        pad.strike(1, nowMs = 100)
        pad.placeCursor(1, 0)
        pad.backspace()
        assertEquals(1, pad.lines.size)
        assertEquals(2, pad.lines[0].notes.size)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `forwardDelete shrinks a multi-dot gap before deleting, hardDelete always deletes`() {
        val fwd = PadState()
        fwd.strike(0, nowMs = 0)
        fwd.setGapSec(0, 0, 2 * REST_UNIT_SEC) // exactly 2 dots
        fwd.placeCursor(0, 0)
        fwd.forwardDelete()
        assertEquals(1, fwd.lines[0].notes.size)
        assertEquals(1, dotCount(fwd.lines[0].notes[0]))

        val hard = PadState()
        hard.strike(0, nowMs = 0)
        hard.setGapSec(0, 0, 2 * REST_UNIT_SEC) // 2 dots, but hardDelete skips the shrink step entirely
        hard.placeCursor(0, 0)
        hard.hardDelete()
        assertTrue(hard.lines[0].notes.isEmpty())
    }

    @Test
    fun `moveCursor Right wraps onto the next line, Left wraps back`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.newLine()
        pad.strike(1, nowMs = 100)
        pad.placeCursor(0, 1) // end of line 0
        pad.moveCursor(CursorDirection.Right)
        assertEquals(Cursor(1, 0), pad.cursor)
        pad.moveCursor(CursorDirection.Left)
        assertEquals(Cursor(0, 1), pad.cursor)
    }

    @Test
    fun `hasAnyNotes is false for an empty pad and true after a strike`() {
        val pad = PadState()
        assertFalse(pad.hasAnyNotes())
        pad.strike(0, nowMs = 0)
        assertTrue(pad.hasAnyNotes())
    }

    @Test
    fun `replaceAll and toModel round-trip through the Line model`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.strike(1, nowMs = 100)
        val model = pad.toModel()

        val loaded = PadState()
        loaded.replaceAll(model)
        assertEquals(model, loaded.toModel())
        assertEquals(Cursor(0, 2), loaded.cursor) // lands at the end, like onTuneChange does
    }
}
