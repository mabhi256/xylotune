package com.xylotune.app.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

// updatedAt is a Long (epoch millis, matching JS Date.now()) — not a Double — so it
// round-trips as a bare integer, never scientific notation.
//
// `bpm`/`meter` are the paper-roll redesign's tempo model, always written (like Line's
// notes/lyric — see the @EncodeDefault note there) so the file states its own tempo rather
// than leaving it implicit. A song saved before these fields existed decodes with bpm=150,
// which reproduces its notes' fixed 0.1s grid exactly (150bpm's sixteenth is 0.1s) — a
// value chosen for lossless migration, not as a real default tempo for a new song (see
// PadState.newSong, which explicitly starts at 100bpm/4:4 instead of relying on this one).
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Song(
    val lines: List<Line>,
    val updatedAt: Long,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val bpm: Int = 150,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val meter: List<Int> = listOf(4, 4),
)
