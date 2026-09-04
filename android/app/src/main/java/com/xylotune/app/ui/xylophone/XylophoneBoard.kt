package com.xylotune.app.ui.xylophone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.NOTES
import com.xylotune.app.ui.theme.TextSecondary

private val BAR_GAP = 10.dp
private val BAR_HIT_EXPANSION = 6.dp
private val BOARD_HORIZONTAL_PADDING = 8.dp

/**
 * The playable instrument surface. Owns exactly one shared raw-pointer listener for the
 * whole board (see the plan's Risk Area 1) — true independent two-thumb multitouch is only
 * achievable this way, since Compose's per-composable gesture APIs (clickable,
 * detectTapGestures, per-child pointerInput) hand a pointer's whole down-to-up stream to a
 * single hit-test winner and can cancel a sibling's stream when two land at once.
 *
 * Each [Bar] child stays pointer-input-free; this composable hit-tests presses itself via
 * [MultiTouchBarTracker] and calls [onStrike] directly, independently, for every pointer
 * that lands on a bar — including two that land in the same event batch.
 *
 * The bars row is `weight(1f)` within this composable's own Column, so it fills whatever
 * height `modifier` is given (typically `Modifier.weight(1f)` from the caller's Column) —
 * there is no fixed bar height to overflow a short landscape viewport.
 */
@Composable
fun XylophoneBoard(
    material: SoundMaterial,
    targetIndex: Int?,
    onStrike: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barCount = NOTES.size
    val tracker = remember { MultiTouchBarTracker(barCount) }
    val strikeIds = remember { mutableStateListOf(*IntArray(barCount).toTypedArray()) }

    // Bars sit edge-to-edge with BAR_GAP between them; expanding each bar's hit rect by up
    // to BAR_HIT_EXPANSION (clamped to half that gap) gives real fingers a little
    // forgiveness without ever letting two adjacent bars' expanded rects overlap.
    val density = LocalDensity.current
    val expandPx = with(density) { minOf(BAR_HIT_EXPANSION.toPx(), BAR_GAP.toPx() / 2) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = BOARD_HORIZONTAL_PADDING)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            for (change in event.changes) {
                                if (change.changedToDown()) {
                                    val pos = change.position
                                    val bar = tracker.onPress(change.id.value, pos.x, pos.y)
                                    if (bar != null) {
                                        strikeIds[bar] = strikeIds[bar] + 1
                                        onStrike(bar)
                                    }
                                    change.consume()
                                } else if (change.changedToUp()) {
                                    tracker.onRelease(change.id.value)
                                    change.consume()
                                }
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        ) {
            NOTES.forEachIndexed { index, note ->
                Bar(
                    label = note.label,
                    color = note.colorFor(material),
                    material = material,
                    index = index,
                    isTarget = index == targetIndex,
                    strikeId = strikeIds[index],
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            val b = coordinates.boundsInParent()
                            tracker.setBarRect(
                                index,
                                MultiTouchBarTracker.Rect(
                                    left = b.left - expandPx,
                                    top = b.top,
                                    right = b.right + expandPx,
                                    bottom = b.bottom,
                                ),
                            )
                        },
                )
            }
        }

        // A second row, not a label nested inside Bar: identical fillMaxWidth/padding/gap/
        // weight(1f) math means each number lines up under its bar without needing to
        // convert coordinates across a parent it doesn't share with the pointer listener.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BOARD_HORIZONTAL_PADDING)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        ) {
            NOTES.indices.forEach { index ->
                Text(
                    text = "${index + 1}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
