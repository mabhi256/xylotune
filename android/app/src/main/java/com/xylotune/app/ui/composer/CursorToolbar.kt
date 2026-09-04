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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextPrimary

/**
 * Touch replacement for the web's keyboard-only editing shortcuts (arrows/Backspace/
 * Delete/Shift+Delete/Enter — see the plan's Risk Area 3). Only shown in the Sheet tab,
 * where the full multi-line editor and its cursor live.
 *
 * `⌦` is a dual-purpose button: a short press is forward-delete (shrink the gap, then
 * delete), a long press is hard-delete (always fully removes the note+dots) — both target
 * "after the cursor," the same thing Delete/Shift+Delete do on the web, so pairing them on
 * one button is accurate, not just economical.
 */
@Composable
fun CursorToolbar(
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onBackspace: () -> Unit,
    onForwardDelete: () -> Unit,
    onHardDelete: () -> Unit,
    onNewLine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ToolbarButton("‹", "Move cursor left", onMoveLeft)
        ToolbarButton("›", "Move cursor right", onMoveRight)
        ToolbarButton("⌫", "Backspace", onBackspace)
        ToolbarButton(
            label = "⌦",
            description = "Delete forward; hold to delete the whole note",
            onClick = onForwardDelete,
            onLongClick = onHardDelete,
        )
        ToolbarButton("⏎", "New line", onNewLine)
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceControl)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
    }
}
