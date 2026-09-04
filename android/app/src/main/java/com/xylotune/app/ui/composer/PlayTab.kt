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
import androidx.compose.material3.ButtonDefaults
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
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.data.PadState
import com.xylotune.app.player.Playback
import com.xylotune.app.player.PracticeEngine
import com.xylotune.app.ui.common.DrawerHandle
import com.xylotune.app.ui.common.DrawerPanel
import com.xylotune.app.ui.common.HUD_CLEARANCE
import com.xylotune.app.ui.common.HudIconButton
import com.xylotune.app.ui.drive.DriveStatusButton
import com.xylotune.app.ui.songs.SongPickerBar
import com.xylotune.app.ui.theme.AccentBlue
import com.xylotune.app.ui.theme.AccentDanger
import com.xylotune.app.ui.theme.TextSecondary
import com.xylotune.app.ui.xylophone.SoundToggle
import com.xylotune.app.ui.xylophone.XylophoneBoard

/**
 * Default tab: the full-size instrument, full-bleed. Song picker and save float as HUD
 * chips over the bars instead of a docked title/toolbar row; sound material, transport, the
 * mini note-preview strip, speed, and practice mode live in a pull-up drawer instead of
 * their own rows — both used to cost fixed-height chrome that, stacked with a fixed-height
 * board, could overflow a short landscape viewport entirely. [XylophoneBoard] takes a
 * `weight(1f)` slot, so the bars claim whatever's left after the lyric margin and the
 * drawer handle, however much or little that is, on any screen.
 *
 * Tapping a bar both sounds it and writes it into the pad at the cursor (live-capture,
 * exactly like the web's strike()) — this is the only place new notes get added, since the
 * Sheet tab has no bars. While [practice] is active that flips: a strike sounds the bar and
 * feeds [PracticeEngine.strike] instead of [PadState.strike] — practice never writes into
 * the pad, exactly like the web's practiceStrike.
 */
@Composable
fun PlayTab(
    pad: PadState,
    playback: Playback,
    practice: PracticeEngine,
    material: SoundMaterial,
    onMaterialChange: (SoundMaterial) -> Unit,
    songNames: List<String>,
    currentSongName: String?,
    onSelectSong: (String) -> Unit,
    onSave: () -> Unit,
    onSwitchToSheet: () -> Unit,
    driveStatus: DriveStatus,
    onDriveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReady by AudioEngine.isReady.collectAsState()
    val activeLineIndex = practice.playingPos?.li ?: playback.playingPos?.li ?: pad.cursor.line
    val activePlayingPos = practice.playingPos ?: playback.playingPos
    val activePlayingTagKey = practice.playingTagKey ?: playback.playingTagKey
    var drawerOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(HUD_CLEARANCE))
            XylophoneBoard(
                material = material,
                targetIndex = practice.targetNoteIndex,
                onStrike = { index ->
                    if (isReady) AudioEngine.playNote(index, material)
                    if (practice.active) practice.strike(index) else pad.strike(index)
                },
                modifier = Modifier.weight(1f),
            )
            ActiveLineLyric(
                line = pad.lines[activeLineIndex],
                lineIndex = activeLineIndex,
                playingTagKey = activePlayingTagKey,
            )
            DrawerHandle(open = drawerOpen, onToggle = { drawerOpen = !drawerOpen })
        }

        SongPickerBar(
            songNames = songNames,
            selected = currentSongName,
            onSelect = onSelectSong,
            enabled = !practice.active,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DriveStatusButton(status = driveStatus, onClick = onDriveClick)
            HudIconButton(icon = "💾", description = "Save song", onClick = onSave, enabled = !practice.active)
            HudIconButton(icon = "📄", description = "Switch to Sheet tab", onClick = onSwitchToSheet)
        }

        DrawerPanel(open = drawerOpen, modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoundToggle(material = material, onChange = onMaterialChange)
                Spacer(modifier = Modifier.width(4.dp))
                val canPlay = pad.hasAnyNotes() && !practice.active
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
                Button(
                    enabled = practice.active || pad.hasAnyNotes(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (practice.active) AccentDanger else AccentBlue),
                    onClick = {
                        if (practice.active) {
                            practice.stop()
                        } else {
                            playback.finish()
                            practice.start()
                        }
                    },
                ) { Text(if (practice.active) "⏹ Stop practice" else "🎯 Practice") }
                if (practice.active) {
                    Text(
                        "Note ${practice.currentIndex + 1} of ${practice.eventCount} · Speed ${(practice.speed * 100).toInt()}%",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            ActiveLineStrip(
                line = pad.lines[activeLineIndex],
                lineIndex = activeLineIndex,
                material = material,
                playingPos = activePlayingPos,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Speed ${playback.speedPercent}%", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = playback.speedPercent.toFloat(),
                    onValueChange = { playback.speedPercent = it.toInt() },
                    valueRange = 50f..200f,
                    steps = 14,
                    enabled = !practice.active,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
