package com.xylotune.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.xylotune.app.model.Line
import com.xylotune.app.model.NoteEvent

/** Cursor sits before `lines[line].notes[pos]`, like a text caret. */
data class Cursor(val line: Int, val pos: Int)

// The web mutates one shared `lines` array of plain {notes, lyric} objects; here each line
// is its own small observable unit instead — a per-line SnapshotStateList<NoteEvent> plus
// a mutableStateOf lyric — so a note added to line 3 only invalidates line 3's row, not
// the whole pad. See the plan's note on why a literal "mutate ref + forceUpdate" port would
// jank under fast two-thumb play.
class PadLine(notes: List<NoteEvent> = emptyList(), lyric: String = "") {
    val notes: SnapshotStateList<NoteEvent> = mutableStateListOf(*notes.toTypedArray())
    var lyric: String by mutableStateOf(lyric)

    fun toModel(bpm: Int): Line = Line(notes = notes.map { it.copy(sec = ticksToSec(ticksOf(it), bpm), ticks = ticksOf(it)) }, lyric = lyric)

    companion object {
        // Every note is given a concrete `ticks` the moment it enters memory — from here
        // on, ticksOf(note) and note.ticks agree, and only serialization ever reads `sec`.
        fun fromModel(line: Line): PadLine = PadLine(line.notes.map { it.copy(ticks = ticksOf(it)) }, line.lyric)
    }
}

/**
 * Ported from index.html's pad mutation functions (newLine/backspace/forwardDelete/
 * hardDelete/addDot/moveCursor, lines ~1144-1254), adapted to the paper-roll redesign's
 * tick-based durations (see data/Constants.kt's ticksOf/TICKS_PER_DOT). [onBeforeEdit]
 * fires at the top of every *external* edit — mirrors the web's `finishPlayback()` call in
 * each of these — so an unrelated action (typing a lyric, moving the cursor, starting a
 * fresh song) cuts short any live take or playback in progress. It's a settable var, not a
 * constructor param, because the Playback instance it usually points at is itself
 * constructed from this PadState (a real cycle, not just ordering) — the caller wires it up
 * right after building both: `val playback = Playback(pad, ...).also { pad.onBeforeEdit = { it.finish() } }`.
 *
 * A new note is never added by any function below — only by [appendLiveNote], the single
 * entry point [com.xylotune.app.player.LiveCapture] uses to punch a struck bar into the pad
 * while the paper roll winds. It's also the one mutator that must *not* call
 * [onBeforeEdit]: that callback stops live capture, and every punch during a take calls
 * appendLiveNote, so wiring it in there made every take cancel itself one strike after
 * starting (the paper never appeared to wind between strikes — see the redesign's roll-
 * freezes-mid-take bug). Everything else here edits, retimes, or removes what's already
 * there, on behalf of something other than the capture session itself.
 */
class PadState {
    var onBeforeEdit: () -> Unit = {}
    val lines: SnapshotStateList<PadLine> = mutableStateListOf(PadLine())
    var cursor: Cursor by mutableStateOf(Cursor(0, 0))
        private set

    // A song's own tempo, distinct from Playback's speedPercent (a practice-only, never-
    // persisted multiplier). Defaults match PadState.newSong()'s explicit reset, not
    // Song's decode-time default of 150 — that 150 exists purely to replay a pre-tempo
    // song's fixed grid correctly, and would be a strange tempo to hand a fresh song.
    var bpm: Int by mutableIntStateOf(100)
        private set
    var meter: List<Int> by mutableStateOf(listOf(4, 4))
        private set

    val isEmpty: Boolean get() = lines.size == 1 && lines[0].notes.isEmpty()
    fun hasAnyNotes(): Boolean = lines.any { line -> line.notes.any { !it.rest } }

    fun changeBpm(value: Int) {
        bpm = value.coerceIn(40, 240)
    }

    fun changeMeter(value: List<Int>) {
        meter = value
    }

    /** Loads a saved song, its own tempo and all. */
    fun loadSong(newLines: List<Line>, songBpm: Int, songMeter: List<Int>) {
        onBeforeEdit()
        replaceLines(newLines)
        bpm = songBpm.coerceIn(40, 240)
        meter = songMeter
    }

    /** Clears the pad for a fresh tune, at this app's own new-song defaults. */
    fun newSong() {
        onBeforeEdit()
        replaceLines(emptyList())
        bpm = 100
        meter = listOf(4, 4)
    }

    private fun replaceLines(newLines: List<Line>) {
        lines.clear()
        if (newLines.isEmpty()) {
            lines.add(PadLine())
            cursor = Cursor(0, 0)
        } else {
            newLines.forEach { lines.add(PadLine.fromModel(it)) }
            cursor = Cursor(lines.lastIndex, lines.last().notes.size)
        }
    }

    fun toModel(): List<Line> = lines.map { it.toModel(bpm) }

    /**
     * Appends a freshly-struck note at the very end of the pad, at a placeholder duration
     * — [adjustLastNoteTicks] fixes up whatever note was *previously* last once the next
     * strike (or the take's closing silence) reveals how long it actually rang. Always
     * appends at the end, never at the cursor: live capture only ever writes forward.
     *
     * Deliberately skips [onBeforeEdit]: this call *is* the live-capture session writing to
     * itself, not an outside edit interrupting one — see the class doc for why calling it
     * here would stop the very take it's part of.
     */
    fun appendLiveNote(noteIndex: Int, ticks: Int) {
        val lineIdx = lines.lastIndex
        val line = lines[lineIdx]
        line.notes.add(NoteEvent(i = noteIndex, ticks = ticks, sec = ticksToSec(ticks, bpm)))
        cursor = Cursor(lineIdx, line.notes.size)
    }

    /** Rewrites the duration of the pad's current last note, in ticks. */
    fun adjustLastNoteTicks(ticks: Int) {
        val line = lines[lines.lastIndex]
        if (line.notes.isEmpty()) return
        val idx = line.notes.lastIndex
        line.notes[idx] = line.notes[idx].copy(ticks = ticks, sec = ticksToSec(ticks, bpm))
    }

    fun newLine() {
        onBeforeEdit()
        val line = lines[cursor.line]
        invalidateTagsAt(line.notes, cursor.pos)
        val tail = removeTail(line.notes, cursor.pos)
        stripLyricTags(tail, tail.indices)
        lines.add(cursor.line + 1, PadLine(notes = tail))
        cursor = Cursor(cursor.line + 1, 0)
    }

    fun backspace() {
        onBeforeEdit()
        val line = lines[cursor.line]
        if (cursor.pos > 0) {
            val idx = cursor.pos - 1
            val n = line.notes[idx]
            val ticks = ticksOf(n)
            if (ticks > TICKS_PER_DOT) {
                line.notes[idx] = n.copy(ticks = ticks - TICKS_PER_DOT)
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
        val line = lines[cursor.line]
        if (cursor.pos < line.notes.size) {
            val n = line.notes[cursor.pos]
            val ticks = ticksOf(n)
            if (ticks > TICKS_PER_DOT) {
                line.notes[cursor.pos] = n.copy(ticks = ticks - TICKS_PER_DOT)
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
        val line = lines[cursor.line]
        if (cursor.pos < line.notes.size) {
            invalidateTagsAt(line.notes, cursor.pos)
            line.notes.removeAt(cursor.pos)
        } else if (cursor.line < lines.lastIndex) {
            mergeNextLineInto(line)
        }
    }

    /** Lengthens the note before the cursor by one grid step — the sheet's "+ hold". */
    fun addHold() {
        if (cursor.pos == 0) return
        onBeforeEdit()
        val line = lines[cursor.line]
        val idx = cursor.pos - 1
        val n = line.notes[idx]
        line.notes[idx] = n.copy(ticks = ticksOf(n) + TICKS_PER_DOT)
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

    // Ported from index.html's LyricCaption handleInput: free-typed text can't be safely
    // re-mapped onto the old char offsets, so any edit voids that line's links outright
    // rather than risk a stale link pointing at the wrong word.
    fun setLyricText(lineIndex: Int, text: String) {
        onBeforeEdit()
        val line = lines[lineIndex]
        line.lyric = text
        stripLyricTags(line.notes, line.notes.indices)
    }

    /** Links notes[noteStart..noteEnd] on lineIndex to lyric[charStart, charEnd). */
    fun linkLyric(lineIndex: Int, noteStart: Int, noteEnd: Int, charStart: Int, charEnd: Int) {
        onBeforeEdit()
        linkLyricSelection(lines[lineIndex].notes, noteStart, noteEnd, charStart, charEnd)
    }

    fun unlinkLyric(lineIndex: Int, noteIdx: Int) {
        onBeforeEdit()
        stripLyricTags(lines[lineIndex].notes, listOf(noteIdx))
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
