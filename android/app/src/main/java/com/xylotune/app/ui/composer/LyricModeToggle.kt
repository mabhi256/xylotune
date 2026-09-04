package com.xylotune.app.ui.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextSecondary

/** Off / Show / Edit — see [LyricMode]. */
@Composable
fun LyricModeToggle(mode: LyricMode, onModeChange: (LyricMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp)),
    ) {
        LyricMode.entries.forEach { m ->
            val selected = m == mode
            Text(
                text = m.label,
                color = if (selected) Color.White else TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .background(if (selected) AccentTeal else SurfaceControl)
                    .clickable { onModeChange(m) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}
