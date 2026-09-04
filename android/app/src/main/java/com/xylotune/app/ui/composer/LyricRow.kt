package com.xylotune.app.ui.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.data.LyricTag
import com.xylotune.app.data.LyricWord
import com.xylotune.app.data.tokenizeLyric

private data class LyricChunk(val start: Int, val end: Int, val text: String, val tagNoteIdx: Int?)

// Groups tokenizeLyric's words back up by shared tag, so e.g. "Twinkle twinkle" linked as
// one phrase renders (and is tapped) as one run, not two independently-underlined words.
private fun chunk(text: String, words: List<LyricWord>, tags: List<LyricTag>): List<LyricChunk> {
    fun tagFor(charStart: Int) = tags.find { charStart >= it.start && charStart < it.end }
    val chunks = mutableListOf<LyricChunk>()
    var i = 0
    while (i < words.size) {
        val tag = tagFor(words[i].start)
        if (tag == null) {
            chunks.add(LyricChunk(words[i].start, words[i].end, words[i].text, null))
            i++
            continue
        }
        var j = i + 1
        while (j < words.size && tagFor(words[j].start) == tag) j++
        chunks.add(LyricChunk(words[i].start, words[j - 1].end, text.substring(words[i].start, words[j - 1].end), tag.noteIdx))
        i = j
    }
    return chunks
}

/**
 * Sheet tab, Show mode: the tappable replacement for the web's drag-selectable caption.
 * Untagged words and tagged phrases both render as [LyricPill]s; tapping one reports its
 * exact char range so the caller can either commit an armed note-to-word link or (if it's
 * already tagged and nothing is armed) unlink it — see PadScreen's onWordClick.
 */
@Composable
fun LyricRow(
    lyric: String,
    lineIndex: Int,
    tags: List<LyricTag>,
    playingTagKey: String?,
    onWordClick: (charStart: Int, charEnd: Int, taggedNoteIdx: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = remember(lyric) { tokenizeLyric(lyric) }
    if (words.isEmpty()) return
    val chunks = remember(lyric, tags) { chunk(lyric, words, tags) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chunks.forEach { c ->
            val playing = c.tagNoteIdx != null && playingTagKey == "$lineIndex-${c.tagNoteIdx}"
            LyricPill(
                text = c.text,
                tagged = c.tagNoteIdx != null,
                playing = playing,
                modifier = Modifier.clickable { onWordClick(c.start, c.end, c.tagNoteIdx) },
            )
        }
    }
}
