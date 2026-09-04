package com.xylotune.app.data

import android.content.Context
import com.xylotune.app.model.Song
import com.xylotune.app.util.XyloJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

// Mirrors the web's xyloSongs/xyloTombstones localStorage keys 1:1 — two local JSON files,
// not one combined blob, so this stays a direct parallel to loadSavedSongs/
// persistSavedSongs/loadTombstones/persistTombstones in index.html.
private const val SONGS_KEY = "xyloSongs"
private const val TOMBSTONES_KEY = "xyloTombstones"

object SongRepository {
    fun loadSongs(context: Context): Map<String, Song> {
        val raw = LocalStore.read(context, SONGS_KEY) ?: return emptyMap()
        return runCatching { XyloJson.decodeFromString<Map<String, Song>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveSongs(context: Context, songs: Map<String, Song>) {
        LocalStore.write(context, SONGS_KEY, XyloJson.encodeToString(songs))
    }

    fun loadTombstones(context: Context): Map<String, Long> {
        val raw = LocalStore.read(context, TOMBSTONES_KEY) ?: return emptyMap()
        return runCatching { XyloJson.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveTombstones(context: Context, tombstones: Map<String, Long>) {
        LocalStore.write(context, TOMBSTONES_KEY, XyloJson.encodeToString(tombstones))
    }
}
