package com.xylotune.app.ui.link

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.LyricWord
import com.xylotune.app.data.NOTES
import com.xylotune.app.data.PadLine
import com.xylotune.app.data.collectLineTags
import com.xylotune.app.data.tokenizeLyric
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.PaperEdge
import com.xylotune.app.ui.theme.PaperFill
import com.xylotune.app.ui.theme.TextMuted
import com.xylotune.app.ui.theme.TextPrimary
import com.xylotune.app.ui.xylophone.XylophoneBoard

/**
 * One line at a time, plus the keys — tap a note to arm it, then tap the word it sings.
 * The keys stay purely for reference here: striking one sounds it but never writes into
 * the pad (that only ever happens via live capture on the roll).
 */
@Composable
fun LinkView(
    lines: List<PadLine>,
    material: SoundMaterial,
    lineIndex: Int,
    onLineIndexChange: (Int) -> Unit,
    armedNoteIndex: Int?,
    onArmNote: (Int?) -> Unit,
    onLinkWord: (noteIdx: Int, charStart: Int, charEnd: Int) -> Unit,
    onUnlinkWord: (noteIdx: Int) -> Unit,
    onStrike: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeIndex = lineIndex.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
    val line = lines.getOrNull(safeIndex)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavButton("‹", enabled = safeIndex > 0, onClick = { onLineIndexChange(safeIndex - 1) })
            Text("Line ${safeIndex + 1} of ${lines.size}", color = TextPrimary, fontWeight = FontWeight.Bold)
            NavButton("›", enabled = safeIndex < lines.lastIndex, onClick = { onLineIndexChange(safeIndex + 1) })
        }

        if (line != null) {
            val tags = collectLineTags(line.notes)
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .background(PaperFill)
                    .border(2.dp, PaperEdge),
                verticalAlignment = Alignment.Bottom,
            ) {
                line.notes.forEachIndexed { ni, n ->
                    val armed = armedNoteIndex == ni
                    Column(
                        modifier = Modifier
                            .width(26.dp)
                            .clickable { onArmNote(if (armed) null else ni) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(NOTES[n.i ?: 0].colorFor(material))
                                .then(
                                    if (armed) {
                                        Modifier.border(2.dp, AccentTeal, RoundedCornerShape(5.dp))
                                    } else Modifier,
                                ),
                        )
                        Text("${(n.i ?: 0) + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA89478))
                    }
                }
            }

            val words = tokenizeLyric(line.lyric)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (words.isEmpty()) {
                    Text("No words on this line yet.", color = TextMuted, fontStyle = FontStyle.Italic)
                } else {
                    words.forEach { w: LyricWord ->
                        val tag = tags.find { it.start == w.start && it.end == w.end }
                        Text(
                            text = w.text,
                            color = if (tag != null) TextPrimary else TextMuted,
                            fontStyle = if (tag != null) FontStyle.Normal else FontStyle.Italic,
                            fontWeight = if (tag != null) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clickable {
                                    if (armedNoteIndex != null) onLinkWord(armedNoteIndex, w.start, w.end)
                                    else if (tag != null) onUnlinkWord(tag.noteIdx)
                                },
                        )
                    }
                }
            }
            Text(
                text = if (armedNoteIndex != null) "Now tap the word it sings." else "Tap a note, then tap the word it sings.",
                color = TextMuted,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            XylophoneBoard(material = material, targetIndex = null, onStrike = onStrike, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, TextPrimary, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}
