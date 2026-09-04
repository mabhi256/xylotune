package com.xylotune.app.model

import kotlinx.serialization.Serializable

// updatedAt is a Long (epoch millis, matching JS Date.now()) — not a Double — so it
// round-trips as a bare integer, never scientific notation.
@Serializable
data class Song(val lines: List<Line>, val updatedAt: Long)
