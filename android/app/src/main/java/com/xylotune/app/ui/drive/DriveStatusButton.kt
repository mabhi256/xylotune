package com.xylotune.app.ui.drive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.ui.common.HudIconButton

// Ported from index.html's DRIVE_BTN status map (lines ~1703-1709). Connect/disconnect
// confirmation lives in the caller (MainScreen), alongside saveSong/deleteSong's own
// dialogHost usage, so this stays a plain single-callback HUD button like the others.
@Composable
fun DriveStatusButton(status: DriveStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, description) = when (status) {
        DriveStatus.Disconnected -> "☁" to "Connect Google Drive"
        DriveStatus.Connecting -> "⏳" to "Connecting to Google Drive…"
        DriveStatus.Syncing -> "🔄" to "Syncing with Google Drive…"
        DriveStatus.Connected -> "☁✓" to "Connected to Google Drive — tap to disconnect"
        DriveStatus.Error -> "⚠" to "Drive sync error — tap to disconnect, then reconnect to retry"
    }
    val busy = status == DriveStatus.Connecting || status == DriveStatus.Syncing
    HudIconButton(
        icon = icon,
        description = description,
        enabled = !busy,
        onClick = onClick,
        modifier = modifier,
    )
}
