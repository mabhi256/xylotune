package com.xylotune.app.data

import androidx.compose.ui.graphics.Color
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.model.NoteEvent
import kotlin.math.roundToInt
import com.xylotune.app.ui.theme.NoteA
import com.xylotune.app.ui.theme.NoteB
import com.xylotune.app.ui.theme.NoteC
import com.xylotune.app.ui.theme.NoteC2
import com.xylotune.app.ui.theme.NoteD
import com.xylotune.app.ui.theme.NoteE
import com.xylotune.app.ui.theme.NoteF
import com.xylotune.app.ui.theme.NoteG
import com.xylotune.app.ui.theme.WoodA
import com.xylotune.app.ui.theme.WoodB
import com.xylotune.app.ui.theme.WoodC
import com.xylotune.app.ui.theme.WoodC2
import com.xylotune.app.ui.theme.WoodD
import com.xylotune.app.ui.theme.WoodE
import com.xylotune.app.ui.theme.WoodF
import com.xylotune.app.ui.theme.WoodG

// The 8-note diatonic set, C5-C6 — ported verbatim from index.html's NOTES array. `label`
// overrides `name` for display only (the octave-up C shows as "C", not "C2"). Each note
// carries both materials' colors, matching the web's CSS variable swap on
// body[data-material] — the bars change palette when you switch Metal/Wood, not just sound.
data class NoteSpec(val name: String, val metalColor: Color, val woodColor: Color, val label: String = name) {
    fun colorFor(material: SoundMaterial): Color = if (material == SoundMaterial.Wood) woodColor else metalColor
}

val NOTES: List<NoteSpec> = listOf(
    NoteSpec("C", NoteC, WoodC),
    NoteSpec("D", NoteD, WoodD),
    NoteSpec("E", NoteE, WoodE),
    NoteSpec("F", NoteF, WoodF),
    NoteSpec("G", NoteG, WoodG),
    NoteSpec("A", NoteA, WoodA),
    NoteSpec("B", NoteB, WoodB),
    NoteSpec("C2", NoteC2, WoodC2, label = "C"),
)

// The pad's data model: lines of notes, edited like text. `sec` is seconds to hold before
// the next event starts — captured from real elapsed time when a note is played live, a
// fixed default otherwise. Ported verbatim from index.html's constants.
const val QUARTER_SEC = 0.4
const val REST_UNIT_SEC = QUARTER_SEC / 4
const val MIN_LIVE_GAP_SEC = 0.06
const val PHRASE_BREAK_SEC = 1.2
const val MAX_PAUSE_SEC = 1.6

fun clamp(v: Double, lo: Double, hi: Double): Double = v.coerceIn(lo, hi)

fun dotCount(note: NoteEvent): Int = maxOf(1, (note.sec / REST_UNIT_SEC).roundToInt())
