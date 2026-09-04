package com.xylotune.app.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

// notes/lyric have Kotlin default values purely for constructor convenience (an empty new
// line), but the web always writes both keys as plain object-literal properties, even at
// "{notes:[],lyric:''}" — @EncodeDefault(ALWAYS) overrides the shared Json{}'s
// encodeDefaults=false (which exists for NoteEvent's genuinely-optional fields) so these
// two are never silently dropped just because they happen to equal their default.
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Line(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val notes: List<NoteEvent> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val lyric: String = "",
)
