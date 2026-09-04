package com.xylotune.app.util

import kotlinx.serialization.json.Json

// One shared Json instance for every read/write of the songs/tombstones files and the
// Drive sync payload. Every flag here is load-bearing for interop with the web app's own
// JSON.stringify/JSON.parse — see model/Note.kt for the field-by-field reasoning:
//   - explicitNulls = false: an absent field (e.g. a plain note's lyricStart) is omitted
//     entirely, never written as `"lyricStart":null` — the web never writes that key at all.
//   - encodeDefaults = false (the default — left unset on purpose): NoteEvent.rest = false
//     stays un-emitted, matching the web only ever writing `"rest":true` or nothing.
//   - ignoreUnknownKeys = true: the web is the long-lived source of truth for this file
//     format; a future web-only field must not hard-crash the Android decoder.
val XyloJson: Json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}
