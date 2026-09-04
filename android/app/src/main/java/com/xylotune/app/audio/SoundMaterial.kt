package com.xylotune.app.audio

// Ordinal must match the native Material enum in Mixer.h exactly (Metal=0, Wood=1) — it's
// passed across JNI as a plain jint.
enum class SoundMaterial { Metal, Wood }
