package com.xylotune.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.xylotune.app.model.Line
import com.xylotune.app.model.NoteEvent

/** Cursor sits before `lines[line].notes[pos]`, like a text caret. */
data class Cursor(val line: Int, val pos: Int)

private data class LastLiveNote(val line: Int, val pos: Int, val timeMs: Long)

// The web mutates one shared `lines` array of plain {notes, lyric} objects; here each line
// is its own small observable unit instead — a per-line SnapshotStateList<NoteEvent> plus
// a mutableStateOf lyric — so a note added to line 3 only invalidates line 3's row, not
// the whole pad. See the plan's note on why a literal "mutate ref + forceUpdate" port would
// jank under fast two-thumb play.
class PadLine(notes: List<NoteEvent> = emptyList(), lyric: String = "") {
    val notes: SnapshotStateList<NoteEvent> = mutableStateListOf(*notes.toTypedArray())
    var lyric: String by mutableStateOf(lyric)

    fun toModel(): Line = Line(notes = notes.toList(), lyric = lyric)

    companion object {
        fun fromModel(line: Line): PadLine = PadLine(line.notes, line.lyric)
    }
}

/**
 * Ported from index.html's pad mutation functions (strike/newLine/backspace/
 * forwardDelete/hardDelete/addDot/moveCursor, lines ~1144-1254). [onBeforeEdit] fires at
 * the top of every mutating function — mirrors the web's `finishPlayback()` call in each of
 * these. It's a settable var, not a constructor param, because the Playback instance it
 * usually points at is itself constructed from this PadState (a real cycle, not just
 * ordering) — the caller wires it up right after building both:
 * `val playback = Playback(pad, ...).also { pad.onBeforeEdit = { it.finish() } }`.
 */
class PadState {
    var onBeforeEdit: () -> Unit = {}
    val lines: SnapshotStateList<PadLine> = mutableStateListOf(PadLine())
    var cursor: Cursor by mutableStateOf(Cursor(0, 0))
        private set

    private var lastLiveNote: LastLiveNote? = null

    val isEmpty: Boolean get() = lines.size == 1 && lines[0].notes.isEmpty()
    fun hasAnyNotes(): Boolean = lines.any { line -> line.notes.any { !it.rest } }

    fun replaceAll(newLines: List<Line>) {
        onBeforeEdit()
        lastLiveNote = null
        lines.clear()
        if (newLines.isEmpty()) {
            lines.add(PadLine())
            cursor = Cursor(0, 0)
        } else {
            newLines.forEach { lines.add(PadLine.fromModel(it)) }
            cursor = Cursor(lines.lastIndex, lines.last().notes.size)
        }
    }

    fun toModel(): List<Line> = lines.map { it.toModel() }

    /** Sounding a bar both plays it and writes it into the pad at the cursor. */
    fun strike(noteIndex: Int, nowMs: Long = System.nanoTime() / 1_000_000) {
        onBeforeEdit()
        val curLine = lines[cursor.line]
        val atEnd = cursor.line == lines.lastIndex && cursor.pos == curLine.notes.size
        val continuesLive = atEnd && lastLiveNote != null &&
            lastLiveNote!!.line == cursor.line && lastLiveNote!!.pos == cursor.pos - 1

        if (continuesLive) {
            val gapSec = (nowMs - lastLiveNote!!.timeMs) / 1000.0
            val idx = cursor.pos - 1
            curLine.notes[idx] = curLine.notes[idx].copy(sec = clamp(gapSec, MIN_LIVE_GAP_SEC, MAX_PAUSE_SEC))
            if (gapSec > PHRASE_BREAK_SEC) {
                lines.add(PadLine())
                cursor = Cursor(lines.lastIndex, 0)
            }
        }

        val targetLineIdx = cursor.line
        val targetLine = lines[targetLineIdx]
        invalidateTagsAt(targetLine.notes, cursor.pos)
        val insertedPos = cursor.pos
        targetLine.notes.add(insertedPos, NoteEvent(i = noteIndex, sec = QUARTER_SEC))
        cursor = cursor.copy(pos = insertedPos + 1)
        lastLiveNote = if (atEnd) LastLiveNote(targetLineIdx, insertedPos, nowMs) else null
    }

    fun newLine() {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[cursor.line]
        invalidateTagsAt(line.notes, cursor.pos)
        val tail = removeTail(line.notes, cursor.pos)
        stripLyricTags(tail, tail.indices)
        lines.add(cursor.line + 1, PadLine(notes = tail))
        cursor = Cursor(cursor.line + 1, 0)
    }

    fun backspace() {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[cursor.line]
        if (cursor.pos > 0) {
            val idx = cursor.pos - 1
            val n = line.notes[idx]
            if (dotCount(n) > 1) {
                line.notes[idx] = n.copy(sec = (dotCount(n) - 1) * REST_UNIT_SEC)
            } else {
                invalidateTagsAt(line.notes, idx)
                line.notes.removeAt(idx)
                cursor = cursor.copy(pos = cursor.pos - 1)
            }
        } else if (cursor.line > 0) {
            val prev = lines[cursor.line - 1]
            val mergePos = prev.notes.size
            stripLyricTags(line.notes, line.notes.indices)
            prev.notes.addAll(line.notes)
            lines.removeAt(cursor.line)
            cursor = Cursor(cursor.line - 1, mergePos)
        }
    }

    fun forwardDelete() {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[cursor.line]
        if (cursor.pos < line.notes.size) {
            val n = line.notes[cursor.pos]
            if (dotCount(n) > 1) {
                line.notes[cursor.pos] = n.copy(sec = (dotCount(n) - 1) * REST_UNIT_SEC)
            } else {
                invalidateTagsAt(line.notes, cursor.pos)
                line.notes.removeAt(cursor.pos)
            }
        } else if (cursor.line < lines.lastIndex) {
            mergeNextLineInto(line)
        }
    }

    fun hardDelete() {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[cursor.line]
        if (cursor.pos < line.notes.size) {
            invalidateTagsAt(line.notes, cursor.pos)
            line.notes.removeAt(cursor.pos)
        } else if (cursor.line < lines.lastIndex) {
            mergeNextLineInto(line)
        }
    }

    fun addDot() {
        if (cursor.pos == 0) return
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[cursor.line]
        val idx = cursor.pos - 1
        val n = line.notes[idx]
        line.notes[idx] = n.copy(sec = (dotCount(n) + 1) * REST_UNIT_SEC)
    }

    fun setGapSec(lineIndex: Int, noteIndex: Int, sec: Double) {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[lineIndex]
        line.notes[noteIndex] = line.notes[noteIndex].copy(sec = sec)
    }

    fun growGap(lineIndex: Int, noteIndex: Int) {
        onBeforeEdit()
        lastLiveNote = null
        val line = lines[lineIndex]
        val n = line.notes[noteIndex]
        line.notes[noteIndex] = n.copy(sec = (dotCount(n) + 1) * REST_UNIT_SEC)
    }

    fun moveCursor(direction: CursorDirection) {
        val len = lines[cursor.line].notes.size
        cursor = when (direction) {
            CursorDirection.Left ->
                if (cursor.pos > 0) cursor.copy(pos = cursor.pos - 1)
                else if (cursor.line > 0) Cursor(cursor.line - 1, lines[cursor.line - 1].notes.size)
                else cursor
            CursorDirection.Right ->
                if (cursor.pos < len) cursor.copy(pos = cursor.pos + 1)
                else if (cursor.line < lines.lastIndex) Cursor(cursor.line + 1, 0)
                else cursor
            CursorDirection.Up ->
                if (cursor.line > 0) Cursor(cursor.line - 1, minOf(cursor.pos, lines[cursor.line - 1].notes.size))
                else cursor.copy(pos = 0)
            CursorDirection.Down ->
                if (cursor.line < lines.lastIndex) Cursor(cursor.line + 1, minOf(cursor.pos, lines[cursor.line + 1].notes.size))
                else cursor.copy(pos = len)
            CursorDirection.Home -> cursor.copy(pos = 0)
            CursorDirection.End -> cursor.copy(pos = len)
        }
    }

    fun placeCursor(lineIndex: Int, pos: Int) {
        cursor = Cursor(lineIndex, pos)
    }

    private fun mergeNextLineInto(line: PadLine) {
        val next = lines[cursor.line + 1]
        stripLyricTags(next.notes, next.notes.indices)
        line.notes.addAll(next.notes)
        lines.removeAt(cursor.line + 1)
    }

    /** Removes and returns notes[fromIndex until size] from `notes`, in original order. */
    private fun removeTail(notes: SnapshotStateList<NoteEvent>, fromIndex: Int): MutableList<NoteEvent> {
        val tail = ArrayDeque<NoteEvent>()
        while (notes.size > fromIndex) {
            tail.addFirst(notes.removeAt(notes.size - 1))
        }
        return tail.toMutableList()
    }
}

enum class CursorDirection { Left, Right, Up, Down, Home, End }
