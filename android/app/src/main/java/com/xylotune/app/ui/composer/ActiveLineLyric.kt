package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.data.LyricTag
import com.xylotune.app.data.PadLine
import com.xylotune.app.data.collectLineTags

private val LYRIC_MARGIN_HEIGHT = 24.dp

/**
 * Play tab's read-only lyric margin: only the phrase(s) already linked to the active line,
 * highlighted in sync with [playingTagKey] — no tap targets, composing and linking stay on
 * the Sheet tab (see [LyricRow]) so Play doesn't reopen the chrome this app just clawed
 * back from a fixed-height stack. Fixed height on purpose: an untagged line still reserves
 * the margin, so the bars above it don't resize as playback crosses lines with and without
 * words.
 */
@Composable
fun ActiveLineLyric(line: PadLine, lineIndex: Int, playingTagKey: String?, modifier: Modifier = Modifier) {
    val tags: List<LyricTag> = collectLineTags(line.notes)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LYRIC_MARGIN_HEIGHT)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tags.forEach { tag ->
                    val text = line.lyric.substring(
                        tag.start.coerceIn(0, line.lyric.length),
                        tag.end.coerceIn(0, line.lyric.length),
                    )
                    LyricPill(
                        text = text,
                        tagged = true,
                        playing = playingTagKey == "$lineIndex-${tag.noteIdx}",
                    )
                }
            }
        }
    }
}
