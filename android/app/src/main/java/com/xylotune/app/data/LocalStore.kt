package com.xylotune.app.data

import android.content.Context
import java.io.File

// The Android analog of the web app's localStorage.getItem/setItem: one small file per
// key under app-internal storage, wrapped in try/catch exactly like the web wraps every
// localStorage call — a full disk or a first-run missing file is an expected case here,
// not an error to propagate.
object LocalStore {
    fun read(context: Context, key: String): String? =
        try {
            File(context.filesDir, key).takeIf { it.exists() }?.readText()
        } catch (_: Exception) {
            null
        }

    fun write(context: Context, key: String, value: String) {
        try {
            File(context.filesDir, key).writeText(value)
        } catch (_: Exception) {
            // ignore, e.g. storage full — matches the web's silent-ignore on write failure
        }
    }
}
