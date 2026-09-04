package com.xylotune.app.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

// The shape of the one Drive file (xylotune-songs.json) both apps read and write. Local
// storage keeps `songs` and `tombstones` as two separate files (mirroring the web's two
// separate localStorage keys) — this combined shape exists only for the Drive upload/
// download body. Both fields always appear in the web's uploaded body, even as `{}` — see
// Line.kt for why @EncodeDefault(ALWAYS) is needed here too.
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SyncFile(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val songs: Map<String, Song> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val tombstones: Map<String, Long> = emptyMap(),
)
