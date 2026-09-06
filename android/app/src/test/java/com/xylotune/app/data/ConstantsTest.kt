package com.xylotune.app.data

import com.xylotune.app.model.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Test

// The tempo model's pure math (see Constants.kt): ticks at 24 PPQ, converted to and from
// real seconds at a song's own bpm, plus bar-length and quantization arithmetic. These are
// deliberately free of PadState/clock dependencies, so a timing bug shows up here first.
class ConstantsTest {

    @Test
    fun `one sixteenth at 150bpm is exactly the legacy 0point1s dot`() {
        assertEquals(REST_UNIT_SEC, ticksToSec(TICKS_PER_DOT, bpm = 150), 1e-12)
    }

    @Test
    fun `a quarter note is 24 ticks at any tempo`() {
        assertEquals(0.5, ticksToSec(PPQ, bpm = 120), 1e-9) // a quarter at 120bpm is half a second
        assertEquals(1.0, ticksToSec(PPQ, bpm = 60), 1e-9) // a quarter at 60bpm is one second
    }

    @Test
    fun `secToTicks is the exact inverse of ticksToSec at the legacy 150bpm grid`() {
        val ticks = 4 * TICKS_PER_DOT // 4 legacy dots
        assertEquals(ticks, secToTicks(ticksToSec(ticks, bpm = 150)))
    }

    @Test
    fun `ticksOf prefers a note's own ticks over deriving from sec`() {
        val note = NoteEvent(i = 0, sec = 999.0, ticks = 24) // sec deliberately wrong/stale
        assertEquals(24, ticksOf(note))
    }

    @Test
    fun `ticksOf migrates a legacy note from sec when ticks is absent`() {
        val note = NoteEvent(i = 0, sec = 0.3) // 3 legacy dots
        assertEquals(3 * TICKS_PER_DOT, ticksOf(note))
    }

    @Test
    fun `barTicks matches simple and compound meters`() {
        assertEquals(96, barTicks(listOf(4, 4)))
        assertEquals(72, barTicks(listOf(3, 4)))
        assertEquals(72, barTicks(listOf(6, 8))) // same length as 3/4, different grouping
    }

    @Test
    fun `quantizeTicks rounds to the nearest grid step`() {
        assertEquals(12, quantizeTicks(10.0, grid = 6))
        assertEquals(6, quantizeTicks(8.0, grid = 6)) // 8 is closer to 6 than to 12
        assertEquals(0, quantizeTicks(2.9, grid = 6))
    }
}
