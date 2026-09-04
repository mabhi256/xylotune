package com.xylotune.app.data

import android.content.Context
import com.xylotune.app.audio.SoundMaterial

// Mirrors the web's xyloSound localStorage key 1:1.
private const val KEY = "xyloSound"

fun loadSoundPref(context: Context): SoundMaterial =
    if (LocalStore.read(context, KEY) == "wood") SoundMaterial.Wood else SoundMaterial.Metal

fun saveSoundPref(context: Context, material: SoundMaterial) {
    LocalStore.write(context, KEY, if (material == SoundMaterial.Wood) "wood" else "metal")
}
