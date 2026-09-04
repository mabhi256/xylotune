package com.xylotune.app.ui.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.TextLyric
import com.xylotune.app.ui.theme.TextPrimary

/**
 * One run of lyric text. Ported verbatim from index.html's CSS: an untagged word is plain
 * muted italic; `.lyric-tagged` gets a teal underline; `.lyric-tagged.playing` fills solid
 * teal with white text instead. Shared by [LyricRow] (Sheet, tappable) and
 * [ActiveLineLyric] (Play, read-only) so both tabs render a link identically.
 */
@Composable
fun LyricPill(text: String, tagged: Boolean, playing: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = when {
            playing -> Color.White
            tagged -> TextPrimary
            else -> TextLyric
        },
        fontStyle = if (tagged) FontStyle.Normal else FontStyle.Italic,
        fontWeight = if (tagged) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .then(
                when {
                    playing -> Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentTeal)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                    tagged -> Modifier.drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(AccentTeal, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                    }
                    else -> Modifier
                },
            )
            .padding(vertical = 2.dp),
    )
}
