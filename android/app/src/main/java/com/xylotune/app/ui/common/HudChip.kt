package com.xylotune.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextPrimary

/** Vertical space to reserve above full-bleed content so it clears the floating HUD row. */
val HUD_CLEARANCE = 56.dp

/** A read-only text label in the same floating-chip language as [HudIconButton]. */
@Composable
fun HudTextChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextPrimary,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceControl)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

/**
 * A single small icon action, floating over the full-bleed keys/lines in the Play and Sheet
 * tabs' top corners — the "everything is a tiny chip, nothing docks a whole row" pattern
 * that lets the bars/lines own the rest of the screen (see the plan's Option B layout).
 */
@Composable
fun HudIconButton(
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceControl)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = icon, fontSize = 17.sp)
    }
}
