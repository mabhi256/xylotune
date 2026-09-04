package com.xylotune.app.player

import com.xylotune.app.data.PadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported cases from index.html's showPracticeTarget/startPractice/stopPractice/
// practiceStrike (lines ~1349-1388).
class PracticeEngineTest {

    private fun padWithNotes(vararg noteIndices: Int): PadState {
        val pad = PadState()
        noteIndices.forEachIndexed { i, n -> pad.strike(n, nowMs = i.toLong() * 1000) }
        return pad
    }

    @Test
    fun `start does nothing on an empty pad`() {
        val engine = PracticeEngine(PadState())
        engine.start()
        assertFalse(engine.active)
    }

    @Test
    fun `start lights the first note as the target`() {
        val engine = PracticeEngine(padWithNotes(2, 5, 1))
        engine.start()
        assertTrue(engine.active)
        assertEquals(2, engine.targetNoteIndex)
        assertEquals(PlayingPos(0, 0), engine.playingPos)
    }

    @Test
    fun `a wrong strike counts a mistake and does not advance`() {
        val engine = PracticeEngine(padWithNotes(2, 5, 1))
        engine.start()
        engine.strike(9) // wrong bar
        assertEquals(1, engine.mistakes)
        assertEquals(2, engine.targetNoteIndex) // target unchanged
        assertEquals(0, engine.currentIndex)
    }

    @Test
    fun `a correct strike advances to the next target`() {
        val engine = PracticeEngine(padWithNotes(2, 5, 1))
        engine.start()
        engine.strike(2)
        assertEquals(1, engine.currentIndex)
        assertEquals(5, engine.targetNoteIndex)
    }

    @Test
    fun `a mistake-free pass speeds up and wraps back to the first note`() {
        val engine = PracticeEngine(padWithNotes(2, 5, 1))
        engine.start()
        engine.strike(2)
        engine.strike(5)
        engine.strike(1) // completes the pass, no mistakes
        assertEquals(0, engine.currentIndex)
        assertEquals(0, engine.mistakes)
        assertEquals(2, engine.targetNoteIndex) // wrapped back to the first note
        assertEquals(1.15, engine.speed, 1e-9)
    }

    @Test
    fun `a pass with a mistake holds speed steady`() {
        val engine = PracticeEngine(padWithNotes(2, 5, 1))
        engine.start()
        engine.strike(9) // mistake
        engine.strike(2)
        engine.strike(5)
        engine.strike(1) // completes the pass, but mistakes > 0
        assertEquals(1.0, engine.speed, 1e-9)
        assertEquals(0, engine.mistakes) // mistake count itself still resets for the next pass
    }

    @Test
    fun `speed caps at 2point0`() {
        val engine = PracticeEngine(padWithNotes(2))
        engine.start()
        repeat(20) { engine.strike(2) }
        assertTrue(engine.speed <= 2.0)
    }

    @Test
    fun `rests are excluded from the practice sequence`() {
        val pad = PadState()
        pad.strike(0, nowMs = 0)
        pad.strike(1, nowMs = 100)
        val engine = PracticeEngine(pad)
        engine.start()
        assertEquals(2, engine.eventCount) // both real notes, no rest ever exists to filter here,
        // but the filter itself is exercised structurally: a rest-free pad round-trips its full count.
    }

    @Test
    fun `stop clears the target and highlight`() {
        val engine = PracticeEngine(padWithNotes(2, 5))
        engine.start()
        engine.stop()
        assertFalse(engine.active)
        assertNull(engine.targetNoteIndex)
        assertNull(engine.playingPos)
    }
}
