package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.PadState
import com.xylotune.app.player.Playback
import com.xylotune.app.ui.common.DrawerHandle
import com.xylotune.app.ui.common.DrawerPanel
import com.xylotune.app.ui.common.HUD_CLEARANCE
import com.xylotune.app.ui.common.HudIconButton
import com.xylotune.app.ui.songs.SongPickerBar
import com.xylotune.app.ui.theme.TextSecondary
import com.xylotune.app.ui.xylophone.SoundToggle
import com.xylotune.app.ui.xylophone.XylophoneBoard

/**
 * Default tab: the full-size instrument, full-bleed. Song picker and save float as HUD
 * chips over the bars instead of a docked title/toolbar row; sound material, transport, the
 * mini note-preview strip, and speed live in a pull-up drawer instead of their own rows —
 * both used to cost fixed-height chrome that, stacked with a fixed-height board, could
 * overflow a short landscape viewport entirely (numbers, the strip, and this whole drawer's
 * worth of controls used to render off-screen). [XylophoneBoard] now takes a `weight(1f)`
 * slot, so the bars claim whatever's left after the lyric margin and the drawer handle,
 * however much or little that is, on any screen.
 *
 * Tapping a bar both sounds it and writes it into the pad at the cursor (live-capture,
 * exactly like the web's strike()) — this is the only place new notes get added, since the
 * Sheet tab has no bars.
 */
@Composable
fun PlayTab(
    pad: PadState,
    playback: Playback,
    material: SoundMaterial,
    onMaterialChange: (SoundMaterial) -> Unit,
    songNames: List<String>,
    currentSongName: String?,
    onSelectSong: (String) -> Unit,
    onSave: () -> Unit,
    onSwitchToSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReady by AudioEngine.isReady.collectAsState()
    val activeLineIndex = playback.playingPos?.li ?: pad.cursor.line
    var drawerOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(HUD_CLEARANCE))
            XylophoneBoard(
                material = material,
                targetIndex = null,
                onStrike = { index ->
                    if (isReady) AudioEngine.playNote(index, material)
                    pad.strike(index)
                },
                modifier = Modifier.weight(1f),
            )
            ActiveLineLyric(
                line = pad.lines[activeLineIndex],
                lineIndex = activeLineIndex,
                playingTagKey = playback.playingTagKey,
            )
            DrawerHandle(open = drawerOpen, onToggle = { drawerOpen = !drawerOpen })
        }

        SongPickerBar(
            songNames = songNames,
            selected = currentSongName,
            onSelect = onSelectSong,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudIconButton(icon = "💾", description = "Save song", onClick = onSave)
            HudIconButton(icon = "📄", description = "Switch to Sheet tab", onClick = onSwitchToSheet)
        }

        DrawerPanel(open = drawerOpen, modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoundToggle(material = material, onChange = onMaterialChange)
                Spacer(modifier = Modifier.width(4.dp))
                val canPlay = pad.hasAnyNotes()
                val playLabel = if (playback.playing) "⏸ Pause" else if (playback.currentIndex > 0) "▶ Resume" else "▶ Play"
                Button(enabled = canPlay, onClick = { if (playback.playing) playback.pause() else playback.startOrResume() }) {
                    Text(playLabel)
                }
                Button(
                    enabled = canPlay,
                    onClick = {
                        playback.finish()
                        playback.startOrResume()
                    },
                ) { Text("↺ Restart") }
            }

            ActiveLineStrip(
                line = pad.lines[activeLineIndex],
                lineIndex = activeLineIndex,
                material = material,
                playingPos = playback.playingPos,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Speed ${playback.speedPercent}%", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = playback.speedPercent.toFloat(),
                    onValueChange = { playback.speedPercent = it.toInt() },
                    valueRange = 50f..200f,
                    steps = 14,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
