package com.xylotune.app.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.data.NOTES
import com.xylotune.app.data.PadLine
import com.xylotune.app.data.barTicks
import com.xylotune.app.data.collectLineTags
import com.xylotune.app.data.ticksOf
import com.xylotune.app.model.NoteEvent
import com.xylotune.app.ui.composer.Caret
import com.xylotune.app.ui.theme.AppBackground
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.NowRed
import com.xylotune.app.ui.theme.PaperEdge
import com.xylotune.app.ui.theme.PaperFill
import com.xylotune.app.ui.theme.TextMuted
import com.xylotune.app.ui.theme.TextPrimary

/**
 * The sheet: the roll, wrapped at its own bar lines into stacked rows instead of scrolling
 * under a fixed head — the same paper and the same pegs, read left to right, top to
 * bottom, which is all a sheet has ever been. For reading and fixing a song rather than
 * playing it; new notes never originate here (see [com.xylotune.app.player.LiveCapture]).
 */
@Composable
fun PaperSheet(
    lines: List<PadLine>,
    meter: List<Int>,
    material: SoundMaterial,
    cursorLine: Int,
    cursorPos: Int,
    hasAnyNotes: Boolean,
    onNoteClick: (li: Int, ni: Int) -> Unit,
    onLyricChange: (li: Int, text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasAnyNotes) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Nothing written yet. Go back to the roll and tap the bars.",
                color = TextMuted,
                fontStyle = FontStyle.Italic,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(lines.size) { li ->
            val line = lines[li]
            SheetLine(
                line = line,
                meter = meter,
                material = material,
                cursorPos = if (li == cursorLine) cursorPos else null,
                onNoteClick = { ni -> onNoteClick(li, ni) },
                onLyricChange = { text -> onLyricChange(li, text) },
            )
        }
    }
}

@Composable
private fun SheetLine(
    line: PadLine,
    meter: List<Int>,
    material: SoundMaterial,
    cursorPos: Int?,
    onNoteClick: (Int) -> Unit,
    onLyricChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            groupIntoBars(line.notes, meter).forEach { indices ->
                BarGroup(line = line, indices = indices, material = material, cursorPos = cursorPos, onNoteClick = onNoteClick)
            }
            if (cursorPos != null && cursorPos >= line.notes.size) {
                Box(modifier = Modifier.padding(horizontal = 4.dp)) { Caret() }
            }
        }
        OutlinedTextField(
            value = line.lyric,
            onValueChange = onLyricChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text("Words for this line…", color = TextMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppBackground,
                unfocusedContainerColor = AppBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = NowRed,
                unfocusedIndicatorColor = BorderSubtle,
            ),
        )
    }
}

@Composable
private fun BarGroup(
    line: PadLine,
    indices: List<Int>,
    material: SoundMaterial,
    cursorPos: Int?,
    onNoteClick: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .height(70.dp)
            .background(PaperFill)
            .border(2.dp, PaperEdge)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        indices.forEach { ni ->
            if (cursorPos == ni) Caret()
            val note = line.notes[ni]
            SmallPegCell(
                note = NOTES[note.i ?: 0],
                noteIndex = note.i ?: 0,
                material = material,
                word = lyricWordFor(line, ni),
                onClick = { onNoteClick(ni) },
            )
        }
    }
}

@Composable
private fun SmallPegCell(note: com.xylotune.app.data.NoteSpec, noteIndex: Int, material: SoundMaterial, word: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(24.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${noteIndex + 1}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFA89478))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(note.colorFor(material)),
        )
        if (word.isNotEmpty()) {
            Text(
                word,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF4E3C1B),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

/** Groups a line's notes into bars by which [barTicks]-wide bar each one starts in. */
private fun groupIntoBars(notes: List<NoteEvent>, meter: List<Int>): List<List<Int>> {
    val per = barTicks(meter)
    val out = mutableListOf<MutableList<Int>>()
    var cum = 0
    notes.forEachIndexed { ni, n ->
        val bar = cum / per
        while (out.size <= bar) out.add(mutableListOf())
        out[bar].add(ni)
        cum += ticksOf(n)
    }
    return out
}

private fun lyricWordFor(line: PadLine, ni: Int): String {
    val tag = collectLineTags(line.notes).find { it.noteIdx == ni } ?: return ""
    return line.lyric.substring(tag.start.coerceIn(0, line.lyric.length), tag.end.coerceIn(0, line.lyric.length))
}
