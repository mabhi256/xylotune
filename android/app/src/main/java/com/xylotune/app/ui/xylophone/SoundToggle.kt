package com.xylotune.app.ui.xylophone

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextSecondary

// Ported from the web's SoundToggle: a two-way segmented control for the bars' material.
@Composable
fun SoundToggle(material: SoundMaterial, onChange: (SoundMaterial) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .semantics { contentDescription = "Bar material" },
    ) {
        SoundToggleOption("🔔 Metal", SoundMaterial.Metal == material) { onChange(SoundMaterial.Metal) }
        SoundToggleOption("🪵 Wood", SoundMaterial.Wood == material) { onChange(SoundMaterial.Wood) }
    }
}

@Composable
private fun SoundToggleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.White else TextSecondary,
        modifier = Modifier
            .background(if (selected) AccentTeal else SurfaceControl)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}
