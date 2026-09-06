package com.xylotune.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AppBackground
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.NowRed
import com.xylotune.app.ui.theme.TextMuted
import com.xylotune.app.ui.theme.TextPrimary

/** The saved-songs picker. Selecting a song hands control to the caller, which loads it
 * and starts practice — this composable only ever lists names and reports a tap. */
@Composable
fun MusicMenu(
    songNames: List<String>,
    currentSongName: String?,
    onSelectSong: (String) -> Unit,
    onNewSong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppBackground)
            .border(2.5.dp, TextPrimary, RoundedCornerShape(14.dp))
            .padding(8.dp),
    ) {
        songNames.sorted().forEach { name ->
            Text(
                text = name,
                color = if (name == currentSongName) NowRed else TextPrimary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelectSong(name) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        if (songNames.isNotEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(2.dp)
                    .background(BorderSubtle),
            )
        }
        Text(
            text = "New song",
            color = TextPrimary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onNewSong)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        )
        Text(
            text = if (songNames.isEmpty()) "Nothing saved yet — write a tune and save it." else "Selecting a song starts practice.",
            color = TextMuted,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
