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
@Serializable
data class NoteEvent(
    val i: Int? = null,
    val rest: Boolean = false,
    val sec: Double,
    val lyricStart: Int? = null,
    val lyricEnd: Int? = null,
    val lyricSpan: Int? = null,
)
