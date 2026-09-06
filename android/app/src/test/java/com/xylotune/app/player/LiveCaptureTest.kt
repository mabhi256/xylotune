package com.xylotune.app.player

import com.xylotune.app.data.PadState
import com.xylotune.app.data.TICKS_PER_DOT
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

// LiveCapture's own quantization sequencing (see Constants.ConstantsTest for the pure
// quantizeTicks math it calls). A TestScope never runs a launched coroutine until the test
// explicitly drives its scheduler — since nothing here does, start()'s background loop
// (animation + silence timeout) never executes, isolating these assertions to exactly
// punch()/stop()'s own synchronous mutation logic against a hand-advanced fake clock.
class LiveCaptureTest {

    private fun captureAt(pad: PadState, nanosRef: LongArray): LiveCapture =
        LiveCapture(pad, TestScope(), clock = { nanosRef[0] })

    @Test
    fun `the first punch appends a placeholder note without adjusting anything`() {
        val pad = PadState() // bpm defaults to 100
        val nanos = longArrayOf(0)
        val capture = captureAt(pad, nanos)
        capture.punch(3)
        assertEquals(1, pad.lines[0].notes.size)
        assertEquals(3, pad.lines[0].notes[0].i)
        assertEquals(TICKS_PER_DOT, pad.lines[0].notes[0].ticks) // one grid step, the placeholder
        assertEquals(true, capture.recording)
    }

    @Test
    fun `a second punch adjusts the first note to the quantized elapsed ticks`() {
        val pad = PadState()
        pad.changeBpm(100) // one tick = 60 / (100 * 24) s
        val nanos = longArrayOf(0)
        val capture = captureAt(pad, nanos)
        capture.punch(0)
        nanos[0] = (0.25 * 1_000_000_000L).toLong() // 0.25s later
        capture.punch(2)
        // 0.25s at 100bpm is 0.25 / secPerTick(100) = 0.25 / (60/2400) = 10 ticks,
        // which quantizes to the nearest sixteenth (TICKS_PER_DOT = 6): round(10/6)*6 = 12.
        assertEquals(12, pad.lines[0].notes[0].ticks)
        assertEquals(2, pad.lines[0].notes[1].i)
        assertEquals(TICKS_PER_DOT, pad.lines[0].notes[1].ticks) // the new note's own placeholder
    }

    @Test
    fun `a held note quantizes against the tick since the PREVIOUS punch, not since the take began`() {
        val pad = PadState()
        pad.changeBpm(100) // one tick = 0.025s
        val nanos = longArrayOf(0)
        val capture = captureAt(pad, nanos)
        capture.punch(0)
        nanos[0] = (0.15 * 1_000_000_000L).toLong() // 6 ticks since t=0
        capture.punch(1)
        nanos[0] = (0.45 * 1_000_000_000L).toLong() // 18 ticks since t=0, i.e. 12 since the last punch
        capture.punch(2)
        assertEquals(6, pad.lines[0].notes[0].ticks) // gap since t=0: 6 ticks
        assertEquals(12, pad.lines[0].notes[1].ticks) // gap since the PREVIOUS punch (6): 12, not 18
    }

    @Test
    fun `stop finalizes the last note's ring length from the tick it was punched at`() {
        val pad = PadState()
        pad.changeBpm(100)
        val nanos = longArrayOf(0)
        val capture = captureAt(pad, nanos)
        capture.punch(0)
        nanos[0] = (0.30 * 1_000_000_000L).toLong()
        capture.stop()
        assertEquals(false, capture.recording)
        // 0.30s at 100bpm is 12 ticks exactly (secPerTick*12 = 0.3), no rounding needed.
        assertEquals(12, pad.lines[0].notes[0].ticks)
    }

    @Test
    fun `stop on an already-stopped capture is a no-op`() {
        val pad = PadState()
        val capture = captureAt(pad, longArrayOf(0))
        capture.stop() // never started
        assertEquals(0, pad.lines[0].notes.size)
    }
}
