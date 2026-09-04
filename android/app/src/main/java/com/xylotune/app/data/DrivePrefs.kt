package com.xylotune.app.data

import android.content.Context

// Mirrors the web's xyloDriveConnected localStorage key — whether to attempt a silent
// reconnect on launch. Unlike the web, there's no xyloDriveClientId to mirror: Android's
// AuthorizationClient needs no client ID in the app at all (see DriveAuthManager).
private const val KEY = "xyloDriveConnected"

fun loadDriveConnected(context: Context): Boolean = LocalStore.read(context, KEY) == "1"

fun saveDriveConnected(context: Context, connected: Boolean) {
    LocalStore.write(context, KEY, if (connected) "1" else "0")
}
