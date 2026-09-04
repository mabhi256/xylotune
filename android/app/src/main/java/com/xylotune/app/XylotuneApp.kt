package com.xylotune.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.xylotune.app.audio.AudioEngine
import kotlinx.coroutines.launch

class XylotuneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Decode the 16 bundled samples first, THEN start observing process
        // foreground/background — registering the observer only after init() finishes
        // guarantees its synthesized onStart() (fired immediately since the process is
        // already started) never races ahead of sample decoding to open a stream with an
        // as-yet-unpopulated SampleBank.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            AudioEngine.init(assets)
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = AudioEngine.onStart()
                override fun onStop(owner: LifecycleOwner) = AudioEngine.onStop()
            })
        }
    }
}
