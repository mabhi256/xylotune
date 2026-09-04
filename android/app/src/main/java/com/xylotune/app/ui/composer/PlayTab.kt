package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.PadState
import com.xylotune.app.player.Playback
import com.xylotune.app.ui.songs.SongPickerBar
import com.xylotune.app.ui.theme.TextSecondary
import com.xylotune.app.ui.xylophone.SoundToggle
import com.xylotune.app.ui.xylophone.XylophoneBoard

/**
 * Default tab: the full-size instrument. Tapping a bar both sounds it and writes it into
 * the pad at the cursor (live-capture, exactly like the web's strike()) — this is the only
 * place new notes get added, since the Sheet tab has no bars. See the plan's Layout section.
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
    modifier: Modifier = Modifier,
) {
    val isReady by AudioEngine.isReady.collectAsState()
    val activeLineIndex = playback.playingPos?.li ?: pad.cursor.line

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongPickerBar(
                songNames = songNames,
                selected = currentSongName,
                onSelect = onSelectSong,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSave) { Text("💾 Save") }
        }

        XylophoneBoard(
            material = material,
            targetIndex = null,
            onStrike = { index ->
                if (isReady) AudioEngine.playNote(index, material)
                pad.strike(index)
            },
        )

        ActiveLineStrip(
            line = pad.lines[activeLineIndex],
            lineIndex = activeLineIndex,
            material = material,
            playingPos = playback.playingPos,
        )

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
