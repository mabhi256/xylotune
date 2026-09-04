package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.Cursor
import com.xylotune.app.data.PadLine
import com.xylotune.app.player.PlayingPos
import com.xylotune.app.ui.theme.TextMuted

/**
 * One line of the Sheet tab's editor. Ported from index.html's PadLine — note chips and
 * their trailing gap-dots, interleaved with a blinking caret at the cursor position (lyric
 * captions/linking land in M3's LyricCaption.kt, not here).
 */
@Composable
fun PadLineRow(
    line: PadLine,
    lineIndex: Int,
    cursor: Cursor,
    material: SoundMaterial,
    playingPos: PlayingPos?,
    showPlaceholder: Boolean,
    onPlaceCursor: (lineIndex: Int, pos: Int) -> Unit,
    onSetGapSec: (lineIndex: Int, noteIndex: Int, sec: Double) -> Unit,
    onGrowGap: (lineIndex: Int, noteIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cursor.line == lineIndex && cursor.pos == 0) Caret()
        line.notes.forEachIndexed { ni, note ->
            if (!note.rest) {
                val playing = playingPos?.li == lineIndex && playingPos.ni == ni
                NoteChip(
                    note = note,
                    material = material,
                    playing = playing,
                    onClick = { onPlaceCursor(lineIndex, ni + 1) },
                )
            }
            GapDots(
                note = note,
                onSetSec = { sec -> onSetGapSec(lineIndex, ni, sec) },
                onGrow = { onGrowGap(lineIndex, ni) },
            )
            if (cursor.line == lineIndex && cursor.pos == ni + 1) Caret()
        }
        if (showPlaceholder) {
            Text(
                text = "Tap the bars to write your tune…",
                color = TextMuted,
                fontStyle = FontStyle.Italic,
                fontSize = 12.5.sp,
            )
        }
    }
}
