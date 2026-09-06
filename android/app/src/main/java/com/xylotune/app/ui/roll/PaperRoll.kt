package com.xylotune.app.ui.roll

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.NOTES
import com.xylotune.app.data.NoteSpec
import com.xylotune.app.data.TICKS_PER_DOT
import com.xylotune.app.data.TapeEvent
import com.xylotune.app.data.barTicks
import com.xylotune.app.ui.theme.NowRed
import com.xylotune.app.ui.theme.PaperEdge
import com.xylotune.app.ui.theme.PaperFill

private val SIXTEENTH_WIDTH = 22.dp // one sprocket hole; a tick is this / TICKS_PER_DOT
private val PEG_WIDTH = 20.dp
private val PEG_HEIGHT = 15.dp
private val CELL_WIDTH = 26.dp // peg width plus a little breathing room for its number/word
private val SEAM_WIDTH = 10.dp
private val ROLL_HEIGHT = 92.dp
private const val PITCH_STEP_DP = 11f // vertical spread between adjacent bars' pegs

/**
 * The punched paper roll: a manila strip winding past a fixed reading head. Every note is
 * a fixed-width punch at the position it was struck — duration lives entirely in the bare
 * paper *after* a punch, up to the next one, because a struck xylophone bar has no
 * note-off, so a punch has no length of its own, only a position (see the redesign notes
 * on why width-as-duration was wrong).
 *
 * Positions are computed analytically from each event's own tick offset (see [tickOffsets]
 * below), not measured from the laid-out tree — every peg and bar line is placed with a
 * plain [Modifier.offset], so this needs none of the onGloballyPositioned bookkeeping the
 * original web mockup's DOM version did.
 *
 * [headTick] is where the reading head sits, in the tape's own global tick units.
 * [animated]=true eases toward it (composing/practice, which step between discrete
 * positions); false tracks it every recomposition with no easing (live capture and
 * playback, which already interpolate a continuous clock themselves — see LiveCapture and
 * Playback) so the strip never lurches.
 */
@Composable
fun PaperRoll(
    tape: List<TapeEvent>,
    meter: List<Int>,
    material: SoundMaterial,
    headTick: Double,
    highlightIndex: Int?,
    animated: Boolean,
    lyricWordFor: (TapeEvent) -> String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sixteenthPx = with(density) { SIXTEENTH_WIDTH.toPx() }
    val pxPerTick = sixteenthPx / TICKS_PER_DOT
    val seamPx = with(density) { SEAM_WIDTH.toPx() }
    val pegPx = with(density) { PEG_WIDTH.toPx() }

    val startPx = remember(tape, pxPerTick, seamPx) { tickOffsets(tape, pxPerTick, seamPx) }
    val barLineXs = remember(tape, meter, startPx, pxPerTick) { barLineOffsets(tape, meter, startPx, pxPerTick) }

    val rawHeadPx = xForTick(headTick, tape, startPx, pxPerTick) + pegPx / 2f
    val headPx = if (animated) {
        val animatedHead by animateFloatAsState(targetValue = rawHeadPx, animationSpec = tween(260), label = "rollHead")
        animatedHead
    } else {
        rawHeadPx
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(ROLL_HEIGHT)) {
        val viewportPx = with(density) { maxWidth.toPx() }
        val trackOffsetPx = viewportPx / 2f - headPx

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(ROLL_HEIGHT * 0.82f)
                    .background(PaperFill)
                    .border(2.dp, PaperEdge)
                    .drawBehind { drawSprockets(trackOffsetPx, sixteenthPx) },
            )

            if (tape.isEmpty()) {
                androidx.compose.material3.Text(
                    text = "Tap the bars to punch your tune…",
                    color = Color(0xFF8A7448),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(ROLL_HEIGHT * 0.82f).align(Alignment.Center)) {
                    barLineXs.forEach { x ->
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset((x + trackOffsetPx - 1.dp.toPx()).toInt(), 0) }
                                .width(2.dp)
                                .fillMaxHeight(0.72f)
                                .align(Alignment.CenterStart)
                                .background(Color(0x62785E2C)),
                        )
                    }
                    tape.forEachIndexed { idx, ev ->
                        if (!ev.rest && ev.noteIndex != null) {
                            Box(
                                modifier = Modifier
                                    .offset { androidx.compose.ui.unit.IntOffset((startPx[idx] + trackOffsetPx).toInt(), 0) }
                                    .align(Alignment.CenterStart),
                            ) {
                                PegCell(
                                    note = NOTES[ev.noteIndex],
                                    noteIndex = ev.noteIndex,
                                    material = material,
                                    lit = idx == highlightIndex,
                                    word = lyricWordFor(ev),
                                )
                            }
                        }
                    }
                }
            }

            // The reading head: fixed at the viewport's centre, always.
            ReadingHead(modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** Every tape event's own start position in px: a line break adds a fixed seam width,
 *  everything else advances by its own tick count times pxPerTick. */
private fun tickOffsets(tape: List<TapeEvent>, pxPerTick: Float, seamPx: Float): List<Float> {
    var px = 0f
    var lastLine = -1
    return tape.map { ev ->
        if (ev.li != lastLine) {
            if (lastLine != -1) px += seamPx
            lastLine = ev.li
        }
        val here = px
        px += ev.ticks * pxPerTick
        here
    }
}

/** Where a bar line falls (every [barTicks] ticks) in the same px space as [tickOffsets] —
 *  found by walking the same tape, so a note held across a barline still gets a line drawn
 *  through it rather than only between cells. */
private fun barLineOffsets(tape: List<TapeEvent>, meter: List<Int>, startPx: List<Float>, pxPerTick: Float): List<Float> {
    val per = barTicks(meter)
    val out = mutableListOf<Float>()
    var cum = 0
    tape.forEachIndexed { i, ev ->
        val start = cum
        val end = cum + ev.ticks
        var b = ((start / per) + 1) * per
        while (b < end) {
            if (b != 0) out.add(startPx[i] + (b - start).toFloat() * pxPerTick)
            b += per
        }
        cum = end
    }
    return out
}

private fun xForTick(tick: Double, tape: List<TapeEvent>, startPx: List<Float>, pxPerTick: Float): Float {
    if (tape.isEmpty()) return 0f
    var i = tape.lastIndex
    for (k in tape.indices) {
        if (tape[k].startTick > tick) { i = k - 1; break }
    }
    return when {
        i < 0 -> startPx[0] + (tick - tape[0].startTick).toFloat() * pxPerTick
        i >= tape.lastIndex -> startPx[i] + (tick - tape[i].startTick).toFloat() * pxPerTick
        else -> {
            val span = (tape[i + 1].startTick - tape[i].startTick).toDouble()
            val f = if (span > 0) (tick - tape[i].startTick) / span else 0.0
            (startPx[i] + f * (startPx[i + 1] - startPx[i])).toFloat()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSprockets(offsetPx: Float, periodPx: Float) {
    val holeR = periodPx * 0.19f
    val holeColor = Color(0x57906E3A)
    var x = (offsetPx % periodPx).let { if (it > 0) it - periodPx else it }
    while (x < size.width) {
        drawCircle(holeColor, radius = holeR, center = Offset(x, holeR * 1.6f))
        drawCircle(holeColor, radius = holeR, center = Offset(x, size.height - holeR * 1.6f))
        x += periodPx
    }
}

@Composable
private fun PegCell(note: NoteSpec, noteIndex: Int, material: SoundMaterial, lit: Boolean, word: String) {
    Column(modifier = Modifier.width(CELL_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(
            text = "${noteIndex + 1}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (lit) NowRed else Color(0xFFA89478),
        )
        Box(
            modifier = Modifier
                .offset(y = ((7 - noteIndex) * PITCH_STEP_DP).dp)
                .width(PEG_WIDTH)
                .height(PEG_HEIGHT)
                .clip(RoundedCornerShape(6.dp))
                .background(note.colorFor(material)),
        )
        if (word.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = word,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (lit) NowRed else Color(0xFF4E3C1B),
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
            )
        }
    }
}

@Composable
private fun ReadingHead(modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(26.dp).height(ROLL_HEIGHT * 0.92f)) {
        Box(modifier = Modifier.align(Alignment.CenterStart).width(2.6.dp).fillMaxHeight().background(NowRed))
        Box(modifier = Modifier.align(Alignment.CenterEnd).width(2.6.dp).fillMaxHeight().background(NowRed))
    }
}
