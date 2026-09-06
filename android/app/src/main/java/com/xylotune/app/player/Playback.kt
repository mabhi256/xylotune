package com.xylotune.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xylotune.app.data.PadState
import com.xylotune.app.data.TapeEvent
import com.xylotune.app.data.buildTape
import com.xylotune.app.data.secPerTick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PlayingPos(val li: Int, val ni: Int)

/**
 * Ported from index.html's buildEvents/step/startOrResume/pausePlayback/finishPlayback,
 * rewritten for the paper-roll redesign: instead of stepping from note to note on a
 * `setTimeout` chain (which re-seats the strip once per note and looks like a jump-cut),
 * this winds a single continuous [currentTick] off a clock — the same "elapsed real time
 * turned into elapsed ticks at the pad's own tempo" shape [LiveCapture] uses for input —
 * so the roll can animate every frame in between, and a note sounds exactly as its tick
 * position crosses the reading head rather than when a timer happens to fire.
 */
class Playback(
    private val pad: PadState,
    private val scope: CoroutineScope,
    private val onPlayNote: (noteIndex: Int, holdMs: Long) -> Unit,
) {
    var playing: Boolean by mutableStateOf(false)
        private set
    var currentTick: Double by mutableDoubleStateOf(0.0)
        private set
    var currentIndex: Int by mutableIntStateOf(0)
        private set

    // The speed slider (50-200%) — intentionally NOT persisted, matching the web, which
    // always resets it to 100 on load. Distinct from PadState.bpm, the song's own tempo.
    var speedPercent: Int by mutableIntStateOf(100)

    private var events: List<TapeEvent> = emptyList()
    private var totalTicks: Int = 0
    private var job: Job? = null
    private var fromTick: Double = 0.0
    private var t0: Long = 0
    private var nextIdx: Int = 0

    val eventCount: Int get() = events.size

    val playingPos: PlayingPos?
        get() = events.getOrNull(currentIndex)?.let { PlayingPos(it.li, it.ni) }

    fun startOrResume() {
        if (playing) return
        if (events.isEmpty()) {
            events = buildTape(pad.lines)
            totalTicks = events.sumOf { it.ticks }
        }
        if (events.isEmpty()) return
        playing = true
        fromTick = currentTick
        t0 = System.nanoTime()
        nextIdx = events.indexOfFirst { it.startTick >= fromTick }.let { if (it < 0) events.size else it }
        job = scope.launch { loop() }
    }

    fun pause() {
        job?.cancel()
        playing = false
    }

    fun finish() {
        job?.cancel()
        playing = false
        currentTick = 0.0
        currentIndex = 0
        events = emptyList() // rebuilt on next start, in case the pad changed meanwhile
    }

    private suspend fun loop() {
        while (playing) {
            val elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0
            val elapsedTicks = elapsedSec / secPerTick(pad.bpm) * (speedPercent / 100.0)
            currentTick = fromTick + elapsedTicks
            while (nextIdx < events.size && currentTick >= events[nextIdx].startTick) {
                val ev = events[nextIdx]
                if (!ev.rest && ev.noteIndex != null) {
                    val durMs = (ev.ticks * secPerTick(pad.bpm) * 1000 / (speedPercent / 100.0)).toLong()
                    onPlayNote(ev.noteIndex, (durMs * 0.9).toLong())
                }
                currentIndex = nextIdx
                nextIdx++
            }
            if (currentTick >= totalTicks) {
                finish()
                return
            }
            delay(16)
        }
    }
}
