package com.xylotune.app.ui.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.ui.theme.TextPrimary

// Ported from index.html's DRIVE_BTN status map (lines ~1703-1709), restyled as a row in
// the Settings sheet now that the HUD chips it used to float among are gone. Connect/
// disconnect confirmation stays with the caller's dialogHost, same as before.
@Composable
fun DriveRow(status: DriveStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, label) = when (status) {
        DriveStatus.Disconnected -> "☁" to "Connect Google Drive"
        DriveStatus.Connecting -> "⏳" to "Connecting…"
        DriveStatus.Syncing -> "🔄" to "Syncing…"
        DriveStatus.Connected -> "☁✓" to "Google Drive · connected"
        DriveStatus.Error -> "⚠" to "Drive sync error — tap to reconnect"
    }
    val busy = status == DriveStatus.Connecting || status == DriveStatus.Syncing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (busy) 0.5f else 1f)
            .clip(RoundedCornerShape(9.dp))
            .then(if (!busy) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
