package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.CursorDirection
import com.xylotune.app.data.PadState
import com.xylotune.app.data.collectLineTags
import com.xylotune.app.player.Playback
import com.xylotune.app.ui.common.DrawerHandle
import com.xylotune.app.ui.common.DrawerPanel
import com.xylotune.app.ui.common.HUD_CLEARANCE
import com.xylotune.app.ui.common.HudIconButton
import com.xylotune.app.ui.common.HudTextChip

/**
 * Sheet tab: the full multi-line editor, note lines and (in Show/Edit mode) their lyrics,
 * full-bleed like the Play tab. Song name, delete, and the tab switch float as HUD chips;
 * the cursor toolbar and lyric controls live in the same pull-up drawer pattern as Play's
 * transport, so lines keep the screen the way bars do there.
 */
@Composable
fun PadScreen(
    pad: PadState,
    material: SoundMaterial,
    playback: Playback,
    canDeleteSong: Boolean,
    onDeleteSong: () -> Unit,
    onSwitchToPlay: () -> Unit,
    songName: String?,
    modifier: Modifier = Modifier,
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var lyricMode by remember { mutableStateOf(LyricMode.Show) }
    var linkArmed by remember { mutableStateOf(false) }
    var armedNote by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun disarmLink() {
        linkArmed = false
        armedNote = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = HUD_CLEARANCE),
            ) {
                itemsIndexed(pad.lines) { index, line ->
                    val tags = collectLineTags(line.notes)
                    Column {
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
                            linkArmed = linkArmed,
                            armedNoteIndex = armedNote?.takeIf { it.first == index }?.second,
                            onArmNote = { li, ni -> armedNote = if (armedNote == li to ni) null else li to ni },
                        )
                        when (lyricMode) {
                            LyricMode.Show -> LyricRow(
                                lyric = line.lyric,
                                lineIndex = index,
                                tags = tags,
                                playingTagKey = playback.playingTagKey,
                                onWordClick = { start, end, taggedNoteIdx ->
                                    val armed = armedNote
                                    if (linkArmed && armed != null && armed.first == index) {
                                        pad.linkLyric(index, armed.second, armed.second, start, end)
                                        armedNote = null
                                    } else if (!linkArmed && taggedNoteIdx != null) {
                                        pad.unlinkLyric(index, taggedNoteIdx)
                                    }
                                },
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            LyricMode.Edit -> LyricEditField(
                                value = line.lyric,
                                onValueChange = { pad.setLyricText(index, it) },
                            )
                            LyricMode.Off -> Unit
                        }
                    }
                }
            }
            DrawerHandle(open = drawerOpen, onToggle = {
                drawerOpen = !drawerOpen
                if (!drawerOpen) disarmLink()
            })
        }

        HudTextChip(
            text = songName ?: "Untitled",
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudIconButton(icon = "🗑", description = "Delete song", onClick = onDeleteSong, enabled = canDeleteSong)
            HudIconButton(icon = "🎵", description = "Switch to Play tab", onClick = onSwitchToPlay)
        }

        DrawerPanel(open = drawerOpen, modifier = Modifier.align(Alignment.BottomCenter)) {
            CursorToolbar(
                onMoveLeft = { pad.moveCursor(CursorDirection.Left) },
                onMoveRight = { pad.moveCursor(CursorDirection.Right) },
                onBackspace = pad::backspace,
                onForwardDelete = pad::forwardDelete,
                onHardDelete = pad::hardDelete,
                onNewLine = pad::newLine,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LyricModeToggle(
                    mode = lyricMode,
                    onModeChange = { mode ->
                        lyricMode = mode
                        if (mode != LyricMode.Show) disarmLink()
                    },
                )
                LinkToggleButton(
                    active = linkArmed,
                    enabled = lyricMode == LyricMode.Show,
                    onClick = {
                        linkArmed = !linkArmed
                        if (!linkArmed) armedNote = null
                    },
                )
            }
        }
    }
}
