package com.xylotune.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xylotune.app.data.PPQ
import com.xylotune.app.data.PadState
import com.xylotune.app.data.quantizeTicks
import com.xylotune.app.data.secPerTick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// How long the strip may wind in silence before a take closes on its own — long enough
// that a real pause between phrases never cuts a song short, short enough that a child
// who wanders off still gets back a song instead of an open-ended recording.
private const val SILENCE_TIMEOUT_SEC = 4.0

/**
 * The rolling live-capture engine: the paper strip starts winding the moment the first bar
 * is struck (at the pad's own tempo, see [PadState.bpm]), and every strike after that is
 * quantized to the grid position the strip has actually reached — not to the raw gap since
 * the last strike, which is why a captured tune keeps its rhythm rather than drifting.
 *
 * Mirrors [Playback]'s "expose a continuous tick position for the UI to render" shape, but
 * for input instead of output: [currentTick] is read by the paper roll to animate the wind
 * live while a take is in progress, exactly as [Playback.currentTick] drives it during
 * playback.
 */
class LiveCapture(
    private val pad: PadState,
    private val scope: CoroutineScope,
    // Injectable so a test can drive the quantization math against a fake clock without
    // needing the background coroutine loop below to ever actually run — see LiveCaptureTest.
    private val clock: () -> Long = System::nanoTime,
) {
    var recording: Boolean by mutableStateOf(false)
        private set
    var currentTick: Double by mutableDoubleStateOf(0.0)
        private set

    private var job: Job? = null
    private var t0: Long = 0
    private var lastStart: Double = 0.0

    // A sixteenth in simple meter, an eighth-triplet in compound (6/8, 9/8, 12/8) — the
    // same distinction the input grid needs on the roll and the sheet.
    private fun inputGrid(): Int = if (pad.meter.getOrElse(1) { 4 } == 8) 8 else com.xylotune.app.data.TICKS_PER_DOT

    private fun rawTick(): Double = (clock() - t0) / 1_000_000_000.0 / secPerTick(pad.bpm)

    /** Called once per bar strike while composing — see [com.xylotune.app.data.PadState]. */
    fun punch(noteIndex: Int) {
        val grid = inputGrid()
        if (!recording) {
            start()
            pad.appendLiveNote(noteIndex, grid)
            lastStart = 0.0
        } else {
            val at = quantizeTicks(rawTick(), grid).toDouble()
            pad.adjustLastNoteTicks(maxOf(grid, (at - lastStart).roundToInt()))
            pad.appendLiveNote(noteIndex, grid)
            lastStart = at
        }
    }

    private fun start() {
        recording = true
        t0 = clock()
        lastStart = 0.0
        job = scope.launch {
            while (recording) {
                currentTick = rawTick()
                if (currentTick - lastStart > SILENCE_TIMEOUT_SEC / secPerTick(pad.bpm)) {
                    stop()
                    break
                }
                delay(16)
            }
        }
    }

    /** Finalizes the take: the last note rings until here, quantized like every other gap. */
    fun stop() {
        if (!recording) return
        recording = false
        job?.cancel()
        val grid = inputGrid()
        val held = quantizeTicks(rawTick() - lastStart, grid)
        pad.adjustLastNoteTicks(held.coerceIn(grid, PPQ * 4))
    }
}
