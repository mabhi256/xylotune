package com.xylotune.app.data

import androidx.compose.ui.graphics.Color
import com.xylotune.app.audio.SoundMaterial
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
