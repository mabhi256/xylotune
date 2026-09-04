package com.xylotune.app.data

import com.xylotune.app.model.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported cases for collectLineTags/stripLyricTags/invalidateTagsAt/clearTagOverlap/
// linkLyricSelection (index.html lines ~329-363).
class LyricLinkingTest {

    private fun note(i: Int, sec: Double = 0.4) = NoteEvent(i = i, sec = sec)

    @Test
    fun `linkLyricSelection anchors a link on the first note of the span`() {
        val notes = mutableListOf(note(0), note(1), note(2))
        linkLyricSelection(notes, noteStart = 1, noteEnd = 2, charStart = 0, charEnd = 5)
        assertNull(notes[0].lyricStart) // note 0 is outside the span, untouched
        assertEquals(0, notes[1].lyricStart)
        assertEquals(5, notes[1].lyricEnd)
        assertEquals(2, notes[1].lyricSpan) // noteEnd(2) - noteStart(1) + 1
        assertNull(notes[2].lyricStart) // the span's second note carries no tag of its own
    }

    @Test
    fun `collectLineTags reads lyricSpan default of 1 and sorts by char start`() {
        val notes = mutableListOf(
            note(0).copy(lyricStart = 10, lyricEnd = 14),
            note(1).copy(lyricStart = 0, lyricEnd = 4),
        )
        val tags = collectLineTags(notes)
        assertEquals(2, tags.size)
        assertEquals(1, tags[0].noteIdx) // the char-start=0 tag sorts first
        assertEquals(1, tags[0].noteSpan)
        assertEquals(0, tags[1].noteIdx)
    }

    @Test
    fun `invalidateTagsAt strips a tag only when pos falls strictly inside its span`() {
        val notes = mutableListOf(note(0), note(1), note(2), note(3))
        linkLyricSelection(notes, noteStart = 1, noteEnd = 2, charStart = 0, charEnd = 4)

        // pos == anchor index: the anchor itself is about to be replaced by the caller
        // (e.g. a delete at that position), not "inside" the span from this function's view.
        invalidateTagsAt(notes, pos = 1)
        assertEquals(0, notes[1].lyricStart)

        // pos strictly inside (anchor < pos <= anchor+span-1): corrupts the link, must strip.
        invalidateTagsAt(notes, pos = 2)
        assertNull(notes[1].lyricStart)
    }

    @Test
    fun `clearTagOverlap strips a prior link overlapping on either the note range or char range`() {
        val byNoteRange = mutableListOf(note(0), note(1), note(2))
        linkLyricSelection(byNoteRange, noteStart = 0, noteEnd = 1, charStart = 0, charEnd = 3)
        linkLyricSelection(byNoteRange, noteStart = 1, noteEnd = 2, charStart = 10, charEnd = 13)
        assertNull(byNoteRange[0].lyricStart) // overwritten: note ranges [0,1] and [1,2] overlap at note 1

        val byCharRange = mutableListOf(note(0), note(1), note(2), note(3))
        linkLyricSelection(byCharRange, noteStart = 0, noteEnd = 0, charStart = 0, charEnd = 5)
        linkLyricSelection(byCharRange, noteStart = 2, noteEnd = 3, charStart = 3, charEnd = 8)
        assertNull(byCharRange[0].lyricStart) // overwritten: char ranges [0,5) and [3,8) overlap
    }

    @Test
    fun `stripLyricTags clears exactly the requested indices`() {
        val notes = mutableListOf(
            note(0).copy(lyricStart = 0, lyricEnd = 1, lyricSpan = 1),
            note(1).copy(lyricStart = 2, lyricEnd = 3, lyricSpan = 1),
        )
        stripLyricTags(notes, listOf(0))
        assertNull(notes[0].lyricStart)
        assertTrue(notes[1].lyricStart == 2)
    }

    // tokenizeLyric feeds the Sheet tab's tap-to-link word chips (see LyricRow) — the
    // mobile replacement for the web's mouse drag-select, so a tapped word's [start, end)
    // must line up exactly with what linkLyricSelection expects.
    @Test
    fun `tokenizeLyric splits on whitespace and reports exact char offsets`() {
        val words = tokenizeLyric("Twinkle  twinkle little")
        assertEquals(3, words.size)
        assertEquals(LyricWord(0, 7, "Twinkle"), words[0])
        assertEquals(LyricWord(9, 16, "twinkle"), words[1])
        assertEquals(LyricWord(17, 23, "little"), words[2])
        // the offsets must be usable straight in linkLyricSelection
        val notes = mutableListOf(note(0))
        linkLyricSelection(notes, noteStart = 0, noteEnd = 0, charStart = words[1].start, charEnd = words[1].end)
        assertEquals("twinkle", "Twinkle  twinkle little".substring(notes[0].lyricStart!!, notes[0].lyricEnd!!))
    }

    @Test
    fun `tokenizeLyric returns nothing for blank text`() {
        assertTrue(tokenizeLyric("").isEmpty())
        assertTrue(tokenizeLyric("   ").isEmpty())
    }
}
