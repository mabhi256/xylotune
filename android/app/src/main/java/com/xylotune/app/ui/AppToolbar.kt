package com.xylotune.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.icons.AppIcon
import com.xylotune.app.ui.icons.BadgeDot
import com.xylotune.app.ui.icons.IconKind
import com.xylotune.app.ui.theme.NowRed
import com.xylotune.app.ui.theme.TextMuted
import com.xylotune.app.ui.theme.TextPrimary

private val TOOLBAR_HEIGHT = 52.dp
private val ICON_BUTTON_SIZE = 40.dp

/**
 * Three buttons left (Settings, Music, the lyrics-view cycle), the song title centred,
 * three right (Reset, Play/Pause, Material) — the one row every view shares, matching the
 * reference toolbar's layout exactly, with a paper-and-crayon skin instead of its flat
 * blue thin-line one (see the redesign's mockup for why that skin had to change).
 */
@Composable
fun AppToolbar(
    songTitle: String?,
    view: LyricsView,
    playing: Boolean,
    playEnabled: Boolean,
    resetEnabled: Boolean,
    settingsOpen: Boolean,
    musicOpen: Boolean,
    onSettingsClick: () -> Unit,
    onMusicClick: () -> Unit,
    onViewCycle: () -> Unit,
    onReset: () -> Unit,
    onPlayPause: () -> Unit,
    onMaterialToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TOOLBAR_HEIGHT)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolbarIconButton(IconKind.Settings, "Settings", isOn = settingsOpen, onClick = onSettingsClick)
        ToolbarIconButton(IconKind.Music, "Music", isOn = musicOpen, onClick = onMusicClick)
        ToolbarIconButton(
            kind = when (view) {
                LyricsView.Roll -> IconKind.Roll
                LyricsView.Sheet -> IconKind.Sheet
                LyricsView.Link -> IconKind.Link
                LyricsView.Keys -> IconKind.Keys
            },
            description = "View: ${view.name.lowercase()}",
            badged = view != LyricsView.Roll,
            onClick = onViewCycle,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = songTitle ?: "Untitled",
                color = if (songTitle != null) TextPrimary else TextMuted,
                fontWeight = if (songTitle != null) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                fontStyle = if (songTitle == null) FontStyle.Italic else FontStyle.Normal,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ToolbarIconButton(IconKind.Reset, "Wind back to the start", enabled = resetEnabled, onClick = onReset)
        ToolbarIconButton(if (playing) IconKind.Pause else IconKind.Play, if (playing) "Pause" else "Play", enabled = playEnabled, onClick = onPlayPause)
        ToolbarIconButton(IconKind.Material, "Bar material", onClick = onMaterialToggle)
    }
}

@Composable
private fun ToolbarIconButton(
    kind: IconKind,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOn: Boolean = false,
    badged: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(ICON_BUTTON_SIZE)
            .alpha(if (enabled) 1f else 0.32f)
            .clip(RoundedCornerShape(11.dp))
            .then(if (isOn) Modifier.background(NowRed.copy(alpha = 0.16f)) else Modifier)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = kind, tint = TextPrimary, modifier = Modifier.size(24.dp))
        if (badged) {
            BadgeDot(tint = NowRed, modifier = Modifier.size(7.dp).align(Alignment.BottomEnd))
        }
    }
}
