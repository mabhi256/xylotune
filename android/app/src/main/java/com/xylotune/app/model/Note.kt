package com.xylotune.app.model

import kotlinx.serialization.Serializable

// One flat data class for both a played note and a rest — deliberately NOT a sealed
// interface with subtypes. kotlinx.serialization's polymorphic serialization would inject
// a "type" discriminator key the web's plain {i,sec}/{rest:true,sec} objects don't have
// and can't read; a single flat shape round-trips through JSON exactly as the web's own
// JS objects do (see util/JsonConfig.kt for the Json{} flags this depends on).
//
// Field types matter for interop, not just values: `i`/lyricStart/lyricEnd/lyricSpan are
// Int (the web writes bare integers), `sec` is Double. This class carries no `updatedAt`
// itself — that lives one level up, on Song.
//
// `ticks` (24 per quarter note, MIDI-style) is the paper-roll redesign's tempo-relative
// duration, read by every mutation and rendering path added for that redesign; `sec`
// stays alongside it purely for wire compatibility with a reader that predates ticks (the
// original index.html, or an older Android build). A note built in memory always carries
// a real `ticks` value — see [com.xylotune.app.data.ticksOf] for the one place that reads
// `sec` as a fallback, when decoding a file saved before this field existed.
@Serializable
data class NoteEvent(
    val i: Int? = null,
    val rest: Boolean = false,
    val sec: Double,
    val ticks: Int? = null,
    val lyricStart: Int? = null,
    val lyricEnd: Int? = null,
    val lyricSpan: Int? = null,
)
