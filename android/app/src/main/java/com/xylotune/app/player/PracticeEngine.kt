package com.xylotune.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xylotune.app.data.PadState
import com.xylotune.app.data.collectLineTags
import kotlin.math.min
import kotlin.math.round

private data class PracticeEvent(val li: Int, val ni: Int, val noteIndex: Int)

/**
 * Ported from index.html's showPracticeTarget/startPractice/stopPractice/practiceStrike
 * (lines ~1349-1388). Rests have no bar to strike, so they're dropped up front when building
 * the event list — the caller only ever waits on an actual note. Unlike [Playback], a
 * practice strike never writes into [pad] — it only ever reads it to build the event list
 * and resolve lyric-tag highlighting for the current target.
 */
class PracticeEngine(private val pad: PadState) {
    var active: Boolean by mutableStateOf(false)
        private set
    var targetNoteIndex: Int? by mutableStateOf(null)
        private set
    var playingPos: PlayingPos? by mutableStateOf(null)
        private set
    var playingTagKey: String? by mutableStateOf(null)
        private set
    var currentIndex: Int by mutableIntStateOf(0)
        private set
    var mistakes: Int by mutableIntStateOf(0)
        private set

    // 1.0-2.0, ramps by +0.15 (rounded to 2dp) after a mistake-free pass; a mistake just
    // holds it where it is rather than knocking it back down.
    var speed: Double by mutableDoubleStateOf(1.0)
        private set

    private var events: List<PracticeEvent> = emptyList()

    val eventCount: Int get() = events.size

    fun start() {
        events = pad.lines.flatMapIndexed { li, line ->
            line.notes.mapIndexedNotNull { ni, n ->
                if (n.rest) null else n.i?.let { PracticeEvent(li, ni, it) }
            }
        }
        if (events.isEmpty()) return
        active = true
        currentIndex = 0
        mistakes = 0
        speed = 1.0
        showTarget()
    }

    fun stop() {
        active = false
        targetNoteIndex = null
        clearPlayingHighlight()
    }

    /** A wrong bar is feedback-only (the caller still sounds it) and doesn't advance. */
    fun strike(noteIndex: Int) {
        val target = events[currentIndex]
        if (noteIndex != target.noteIndex) {
            mistakes++
            return
        }
        currentIndex++
        if (currentIndex >= events.size) {
            if (mistakes == 0) speed = min(2.0, round((speed + 0.15) * 100) / 100)
            currentIndex = 0
            mistakes = 0
        }
        showTarget()
    }

    private fun showTarget() {
        val ev = events[currentIndex]
        clearPlayingHighlight()
        targetNoteIndex = ev.noteIndex
        playingPos = PlayingPos(ev.li, ev.ni)
        val tag = collectLineTags(pad.lines[ev.li].notes)
            .find { ev.ni >= it.noteIdx && ev.ni <= it.noteIdx + it.noteSpan - 1 }
        if (tag != null) playingTagKey = "${ev.li}-${tag.noteIdx}"
    }

    private fun clearPlayingHighlight() {
        playingPos = null
        playingTagKey = null
    }
}
