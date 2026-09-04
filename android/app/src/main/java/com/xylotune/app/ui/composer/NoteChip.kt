package com.xylotune.app.ui.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.NOTES
import com.xylotune.app.model.NoteEvent

// Ported from index.html's Chip — always called for a non-rest note (n.i is non-null).
// `armed` (lyric-linking's note selection, a dashed ring) and `playing` (a solid ring) are
// independent so both a currently-sounding note and a note staged for linking stay
// visually distinct even if they land on the same chip.
@Composable
fun NoteChip(
    note: NoteEvent,
    material: SoundMaterial,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    armed: Boolean = false,
) {
    val noteIndex = note.i ?: return
    val color = NOTES[noteIndex].colorFor(material)
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .then(if (playing) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
            .then(if (armed) Modifier.dashedRing(Color.White, 6.dp) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "${noteIndex + 1}", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.dashedRing(color: Color, cornerRadius: Dp, strokeWidth: Dp = 2.dp) =
    drawBehind {
        val stroke = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke.width / 2, stroke.width / 2),
            size = size.copy(width = size.width - stroke.width, height = size.height - stroke.width),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = stroke,
        )
    }
