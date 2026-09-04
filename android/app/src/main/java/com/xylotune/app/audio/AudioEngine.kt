package com.xylotune.app.audio

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

// One long-lived native engine for the whole process — a Kotlin object, never constructed
// or garbage-collected, so there's never ambiguity about which native instance owns the
// Oboe stream. See the plan's Risk Area 2 for the full threading/lifecycle design.
object AudioEngine {
    init {
        System.loadLibrary("xylotune_audio")
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // Decodes the 16 bundled samples. Called once from XylotuneApp; safe to call off the
    // main thread since it's a blocking JNI call.
    suspend fun init(assets: AssetManager) {
        val ok = withContext(Dispatchers.Default) { nativeInit(assets) }
        _isReady.value = ok
    }

    // Tied to process foreground/background via ProcessLifecycleOwner (XylotuneApp), not
    // the Activity lifecycle — see the plan for why.
    fun onStart() = nativeOnStart()
    fun onStop() = nativeOnStop()

    // Never blocks — safe to call rapidly from the UI thread on every bar strike.
    fun playNote(noteIndex: Int, material: SoundMaterial) {
        if (!_isReady.value) return
        nativePlayNote(noteIndex, material.ordinal)
    }

    private external fun nativeInit(assetManager: AssetManager): Boolean
    private external fun nativeOnStart()
    private external fun nativeOnStop()
    private external fun nativePlayNote(noteIndex: Int, material: Int)
}
