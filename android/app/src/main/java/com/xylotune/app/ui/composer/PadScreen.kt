package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.CursorDirection
import com.xylotune.app.data.PadState
import com.xylotune.app.player.Playback
import com.xylotune.app.ui.theme.AccentDanger

/**
 * Sheet tab: the full multi-line editor — all lines, cursor navigation/delete — with no
 * bars at all (no new notes can be added here; that only happens from the Play tab). Lyric
 * linking lands in M3's LyricCaption.kt, not yet wired in here.
 */
@Composable
fun PadScreen(
    pad: PadState,
    material: SoundMaterial,
    playback: Playback,
    canDeleteSong: Boolean,
    onDeleteSong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sheet", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(enabled = canDeleteSong, onClick = onDeleteSong) {
                Text("Delete song", color = if (canDeleteSong) AccentDanger else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        CursorToolbar(
            onMoveLeft = { pad.moveCursor(CursorDirection.Left) },
            onMoveRight = { pad.moveCursor(CursorDirection.Right) },
            onBackspace = pad::backspace,
            onForwardDelete = pad::forwardDelete,
            onHardDelete = pad::hardDelete,
            onNewLine = pad::newLine,
            modifier = Modifier.padding(vertical = 10.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(pad.lines) { index, line ->
                PadLineRow(
                    line = line,
                    lineIndex = index,
                    cursor = pad.cursor,
                    material = material,
                    playingPos = playback.playingPos,
                    showPlaceholder = pad.isEmpty && index == 0,
                    onPlaceCursor = pad::placeCursor,
                    onSetGapSec = pad::setGapSec,
                    onGrowGap = pad::growGap,
                )
            }
        }
    }
}
