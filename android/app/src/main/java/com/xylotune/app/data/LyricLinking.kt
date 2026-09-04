package com.xylotune.app.data

import com.xylotune.app.model.NoteEvent

// Ported from index.html's collectLineTags/stripLyricTags/invalidateTagsAt/
// clearTagOverlap/linkLyricSelection (lines ~329-363). A link anchors on a note's own
// lyricStart/lyricEnd (char offsets into that line's lyric text) plus lyricSpan (how many
// consecutive notes, this one included, it covers) — anchoring on the note itself, not a
// separately-tracked index, means the link rides along automatically when notes before it
// are inserted, deleted, or reordered.
//
// The web mutates note objects in place (`n.lyricStart = ...`, `delete n.lyricStart`);
// NoteEvent here is an immutable data class instead, so every "mutation" below is a
// `notes[i] = notes[i].copy(...)` write-back into the caller's mutable list (in practice a
// SnapshotStateList, so Compose observes the change).

data class LyricTag(val start: Int, val end: Int, val noteIdx: Int, val noteSpan: Int)

fun collectLineTags(notes: List<NoteEvent>): List<LyricTag> =
    notes.withIndex()
        .filter { (_, n) -> n.lyricStart != null }
        .map { (ni, n) -> LyricTag(start = n.lyricStart!!, end = n.lyricEnd!!, noteIdx = ni, noteSpan = n.lyricSpan ?: 1) }
        .sortedBy { it.start }

fun stripLyricTags(notes: MutableList<NoteEvent>, indices: Collection<Int>) {
    indices.forEach { i ->
        notes[i] = notes[i].copy(lyricStart = null, lyricEnd = null, lyricSpan = null)
    }
}

// Insertion/removal at `pos` (pre-mutation index) can only corrupt a link whose span
// strictly contains it — before its anchor the link is unaffected (rides with the anchor
// note), and removing the anchor itself already self-heals via collectLineTags.
fun invalidateTagsAt(notes: MutableList<NoteEvent>, pos: Int) {
    val toStrip = collectLineTags(notes)
        .filter { pos > it.noteIdx && pos <= it.noteIdx + it.noteSpan - 1 }
        .map { it.noteIdx }
    stripLyricTags(notes, toStrip)
}

// A new link overwrites any existing link that overlaps it on either axis — the note
// range or the text range — so a partial overwrite can't leave a stale, half-matching link.
fun clearTagOverlap(notes: MutableList<NoteEvent>, noteStart: Int, noteEnd: Int, charStart: Int, charEnd: Int) {
    val toStrip = collectLineTags(notes)
        .filter { t ->
            val noteOverlap = t.noteIdx <= noteEnd && t.noteIdx + t.noteSpan - 1 >= noteStart
            val charOverlap = t.start < charEnd && t.end > charStart
            noteOverlap || charOverlap
        }
        .map { it.noteIdx }
    stripLyricTags(notes, toStrip)
}

fun linkLyricSelection(notes: MutableList<NoteEvent>, noteStart: Int, noteEnd: Int, charStart: Int, charEnd: Int) {
    clearTagOverlap(notes, noteStart, noteEnd, charStart, charEnd)
    notes[noteStart] = notes[noteStart].copy(
        lyricStart = charStart,
        lyricEnd = charEnd,
        lyricSpan = noteEnd - noteStart + 1,
    )
}
