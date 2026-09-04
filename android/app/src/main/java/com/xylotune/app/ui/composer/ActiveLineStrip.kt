package com.xylotune.app.ui.composer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.PadLine
import com.xylotune.app.player.PlayingPos
import com.xylotune.app.ui.theme.TextMuted

/**
 * Play tab's condensed sheet view: just the one line the cursor (while composing) or the
 * playhead (while playing back) currently sits on — no cursor toolbar, no editing, purely
 * a live preview of what tapping the bars has captured so far. The full multi-line editor
 * lives in the Sheet tab (PadScreen.kt).
 */
@Composable
fun ActiveLineStrip(
    line: PadLine,
    lineIndex: Int,
    material: SoundMaterial,
    playingPos: PlayingPos?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (line.notes.isEmpty()) {
            Text(text = "Tap the bars to write your tune…", color = TextMuted, fontStyle = FontStyle.Italic, fontSize = 12.5.sp)
            return@Row
        }
        line.notes.forEachIndexed { ni, note ->
            if (!note.rest) {
                val playing = playingPos?.li == lineIndex && playingPos.ni == ni
                NoteChip(note = note, material = material, playing = playing, onClick = {})
            }
            GapDots(note = note, onSetSec = {}, onGrow = {})
        }
    }
}
