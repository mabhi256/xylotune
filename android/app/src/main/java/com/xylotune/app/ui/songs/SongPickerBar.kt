package com.xylotune.app.ui.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.SurfaceControl
import com.xylotune.app.ui.theme.TextPrimary

// Ported from index.html's "Pick a tune…" <select> — a plain (not "exposed") DropdownMenu
// anchored to a button, which stays API-stable across Compose Material3 versions.
@Composable
fun SongPickerBar(
    songNames: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Text(
            text = selected ?: "Pick a tune…",
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.45f)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .background(SurfaceControl, RoundedCornerShape(10.dp))
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            songNames.sorted().forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                )
            }
        }
    }
}
