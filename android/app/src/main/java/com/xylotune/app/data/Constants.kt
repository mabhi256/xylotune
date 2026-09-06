package com.xylotune.app.data

import androidx.compose.ui.graphics.Color
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.model.NoteEvent
import kotlin.math.roundToInt
import com.xylotune.app.ui.theme.NoteA
import com.xylotune.app.ui.theme.NoteAEdge
import com.xylotune.app.ui.theme.NoteB
import com.xylotune.app.ui.theme.NoteBEdge
import com.xylotune.app.ui.theme.NoteC
import com.xylotune.app.ui.theme.NoteC2
import com.xylotune.app.ui.theme.NoteC2Edge
import com.xylotune.app.ui.theme.NoteCEdge
import com.xylotune.app.ui.theme.NoteD
import com.xylotune.app.ui.theme.NoteDEdge
import com.xylotune.app.ui.theme.NoteE
import com.xylotune.app.ui.theme.NoteEEdge
import com.xylotune.app.ui.theme.NoteF
import com.xylotune.app.ui.theme.NoteFEdge
import com.xylotune.app.ui.theme.NoteG
import com.xylotune.app.ui.theme.NoteGEdge
import com.xylotune.app.ui.theme.WoodA
import com.xylotune.app.ui.theme.WoodAEdge
import com.xylotune.app.ui.theme.WoodB
import com.xylotune.app.ui.theme.WoodBEdge
import com.xylotune.app.ui.theme.WoodC
import com.xylotune.app.ui.theme.WoodC2
import com.xylotune.app.ui.theme.WoodC2Edge
import com.xylotune.app.ui.theme.WoodCEdge
import com.xylotune.app.ui.theme.WoodD
import com.xylotune.app.ui.theme.WoodDEdge
import com.xylotune.app.ui.theme.WoodE
import com.xylotune.app.ui.theme.WoodEEdge
import com.xylotune.app.ui.theme.WoodF
import com.xylotune.app.ui.theme.WoodFEdge
import com.xylotune.app.ui.theme.WoodG
import com.xylotune.app.ui.theme.WoodGEdge

// The 8-note diatonic set, C5-C6 — ported verbatim from index.html's NOTES array. `label`
// overrides `name` for display only (the octave-up C shows as "C", not "C2"). Each note
// carries both materials' fill AND edge colors, matching the web's CSS variable swap on
// body[data-material] — the bars change palette when you switch Metal/Wood, not just
// sound. The edge is a darker shade of the same hue, not a shared neutral outline, so a
// crayon key still reads as "that colour" even along its border (see ui/xylophone/Bar.kt).
data class NoteSpec(
    val name: String,
    val metalColor: Color,
    val metalEdge: Color,
    val woodColor: Color,
    val woodEdge: Color,
    val label: String = name,
) {
    fun colorFor(material: SoundMaterial): Color = if (material == SoundMaterial.Wood) woodColor else metalColor
    fun edgeFor(material: SoundMaterial): Color = if (material == SoundMaterial.Wood) woodEdge else metalEdge
}

val NOTES: List<NoteSpec> = listOf(
    NoteSpec("C", NoteC, NoteCEdge, WoodC, WoodCEdge),
    NoteSpec("D", NoteD, NoteDEdge, WoodD, WoodDEdge),
    NoteSpec("E", NoteE, NoteEEdge, WoodE, WoodEEdge),
    NoteSpec("F", NoteF, NoteFEdge, WoodF, WoodFEdge),
    NoteSpec("G", NoteG, NoteGEdge, WoodG, WoodGEdge),
    NoteSpec("A", NoteA, NoteAEdge, WoodA, WoodAEdge),
    NoteSpec("B", NoteB, NoteBEdge, WoodB, WoodBEdge),
    NoteSpec("C2", NoteC2, NoteC2Edge, WoodC2, WoodC2Edge, label = "C"),
)

// The legacy fixed-tempo grid (150bpm) that every song was captured/stored at before the
// paper-roll redesign's tempo model — kept only as the wire-compatibility path ticksOf()
// falls back to for a note saved before ticks existed.
const val QUARTER_SEC = 0.4
const val REST_UNIT_SEC = QUARTER_SEC / 4

// ---------------------------------------------------------------------------------------
// Tempo model (the paper-roll redesign): ticks at 24 PPQ, the way MIDI/MusicXML store
// duration, plus a song-level bpm and time signature. A sixteenth note is TICKS_PER_DOT
// ticks — exactly one "dot" on the app's pre-existing 0.1s grid, so migrating a legacy
// note is a single multiply (see ticksOf) and a song saved at the legacy fixed 150bpm
// round-trips through the new model bit-for-bit.
const val PPQ = 24
const val TICKS_PER_DOT = PPQ / 4

fun secPerTick(bpm: Int): Double = 60.0 / (bpm * PPQ)

fun ticksToSec(ticks: Int, bpm: Int): Double = ticks * secPerTick(bpm)

// The inverse of ticksToSec, used only to migrate a note that has no `ticks` of its own
// yet (see ticksOf) — never for a genuinely new note, which is always given ticks directly.
fun secToTicks(sec: Double): Int = maxOf(TICKS_PER_DOT, (sec / REST_UNIT_SEC).roundToInt() * TICKS_PER_DOT)

// A note's real duration in ticks, migrating on read from `sec` if this note predates the
// tempo model. Every tick-based call site (roll/sheet rendering, playback, live capture)
// goes through this rather than reading `note.ticks` directly, so no call site can forget
// the fallback and silently misrender an old song as one long zero-width note.
fun ticksOf(note: NoteEvent): Int = note.ticks ?: secToTicks(note.sec)

// One bar's length in ticks for a `meter` of [beatsPerBar, beatUnit] — e.g. [4,4] is 96,
// [6,8] is 72. Used to place the paper roll's bar lines and to group the sheet's notes
// into the same bars.
fun barTicks(meter: List<Int>): Int {
    val (beats, unit) = meter
    return (beats * (4.0 / unit) * PPQ).roundToInt()
}

// Quantizes a raw (fractional) tick position to the nearest multiple of `grid` ticks —
// the one piece of live-capture math that's pure enough to unit-test without a clock.
fun quantizeTicks(rawTicks: Double, grid: Int): Int = (Math.round(rawTicks / grid) * grid).toInt()
