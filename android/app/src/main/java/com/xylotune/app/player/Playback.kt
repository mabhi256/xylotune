package com.xylotune.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xylotune.app.data.PadState
import com.xylotune.app.data.collectLineTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class PlayingPos(val li: Int, val ni: Int)

private data class PlaybackEvent(val li: Int, val ni: Int, val rest: Boolean, val noteIndex: Int?, val sec: Double)

/**
 * Ported from index.html's buildEvents/step/startOrResume/pausePlayback/finishPlayback
 * (lines ~1306-1347). The web schedules each step via `setTimeout(step, durMs)`; this uses
 * a cancellable coroutine loop with `delay(durMs)` instead — same "wait, then advance"
 * shape, idiomatic for Kotlin.
 */
class Playback(
    private val pad: PadState,
    private val scope: CoroutineScope,
    private val onPlayNote: (noteIndex: Int, holdMs: Long) -> Unit,
) {
    var playing: Boolean by mutableStateOf(false)
        private set
    var playingPos: PlayingPos? by mutableStateOf(null)
        private set
    var playingTagKey: String? by mutableStateOf(null)
        private set

    // The speed slider (50-200%) — intentionally NOT persisted, matching the web, which
    // always resets it to 100 on load.
    var speedPercent: Int by mutableIntStateOf(100)

    private var events: List<PlaybackEvent> = emptyList()
    private var idx: Int = 0
    private var job: Job? = null

    val currentIndex: Int get() = idx
    val eventCount: Int get() = events.size

    fun startOrResume() {
        if (playing) return
        if (idx == 0) events = buildEvents()
        if (events.isEmpty()) return
        playing = true
        job = scope.launch { runLoop() }
    }

    fun pause() {
        job?.cancel()
        playing = false
    }

    fun finish() {
        job?.cancel()
        playing = false
        idx = 0
        clearPlayingHighlight()
    }

    private suspend fun runLoop() {
        while (idx < events.size) {
            val ev = events[idx]
            val speed = speedPercent / 100.0
            val durMs = (ev.sec * 1000.0 / speed).roundToLong()
            if (!ev.rest && ev.noteIndex != null) onPlayNote(ev.noteIndex, (durMs * 0.9).roundToLong())
            clearPlayingHighlight()
            playingPos = PlayingPos(ev.li, ev.ni)
            val tag = collectLineTags(pad.lines[ev.li].notes)
                .find { ev.ni >= it.noteIdx && ev.ni <= it.noteIdx + it.noteSpan - 1 }
            if (tag != null) playingTagKey = "${ev.li}-${tag.noteIdx}"
            idx++
            delay(durMs.coerceAtLeast(0))
        }
        finish()
    }

    private fun clearPlayingHighlight() {
        playingPos = null
        playingTagKey = null
    }

    private fun buildEvents(): List<PlaybackEvent> =
        pad.lines.flatMapIndexed { li, line ->
            line.notes.mapIndexed { ni, n -> PlaybackEvent(li, ni, n.rest, n.i, n.sec) }
        }
}
