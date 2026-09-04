package com.xylotune.app.ui.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xylotune.app.data.REST_UNIT_SEC
import com.xylotune.app.data.dotCount
import com.xylotune.app.model.NoteEvent
import com.xylotune.app.ui.theme.TextMuted

// Ported from index.html's GapDots: the gap after a note/rest, shown as clickable dots
// (one dot = REST_UNIT_SEC). Tap a dot to cut the gap back to that point; tap the trailing
// spacer (past the last dot) to grow it by one.
@Composable
fun GapDots(
    note: NoteEvent,
    onSetSec: (Double) -> Unit,
    onGrow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = dotCount(note)
    Row(modifier = modifier) {
        repeat(count) { d ->
            Text(
                text = "·",
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onSetSec((d + 1) * REST_UNIT_SEC) },
            )
        }
        // The clickable "click past the dots to grow the gap" area the web gives the
        // GapDots span its own trailing padding for.
        Text(
            text = "",
            modifier = Modifier
                .width(10.dp)
                .clickable(onClick = onGrow),
        )
    }
}
