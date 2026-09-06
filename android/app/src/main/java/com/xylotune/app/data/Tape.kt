package com.xylotune.app.data

/**
 * One flattened, line-crossing view of the pad: every note and rest, in order, with the
 * tick it starts on. Shared by [com.xylotune.app.player.Playback] (which schedules against
 * `startTick`) and the paper-roll/sheet rendering (which lays pegs out by the same ticks),
 * so the two can never quietly disagree about where a note sits in time.
 */
data class TapeEvent(val li: Int, val ni: Int, val rest: Boolean, val noteIndex: Int?, val ticks: Int, val startTick: Int)

fun buildTape(lines: List<PadLine>): List<TapeEvent> {
    var acc = 0
    val out = mutableListOf<TapeEvent>()
    lines.forEachIndexed { li, line ->
        line.notes.forEachIndexed { ni, n ->
            val t = ticksOf(n)
            out.add(TapeEvent(li = li, ni = ni, rest = n.rest, noteIndex = n.i, ticks = t, startTick = acc))
            acc += t
        }
    }
    return out
}
