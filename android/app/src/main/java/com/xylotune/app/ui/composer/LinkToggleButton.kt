package com.xylotune.app.ui.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextPrimary

/**
 * Arms/disarms lyric linking. Armed: tap a note (it gets [NoteChip]'s dashed ring), then
 * tap a lyric word to link them — the mobile replacement for the web's click-note-then-
 * drag-select-text gesture (see the plan's Sheet-tab lyrics notes).
 */
@Composable
fun LinkToggleButton(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AccentTeal else SurfaceControl)
            .alpha(if (enabled) 1f else 0.45f)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (active) "Linking armed — tap a note, then a lyric word" else "Link a note to a lyric word"
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🔗", color = if (active) Color.White else TextPrimary, style = MaterialTheme.typography.titleLarge)
    }
}
