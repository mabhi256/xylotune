#pragma once

#include <android/asset_manager.h>
#include <oboe/Oboe.h>

#include "Mixer.h"
#include "SampleBank.h"

namespace xylotune {

// One instance for the whole process, owned by the Kotlin `AudioEngine` object (which is
// never GC'd/reconstructed — see native-lib.cpp). Not copyable/movable; there is exactly
// one of these.
class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    // Decodes the 16 bundled samples. Safe to call once, off the main thread, before any
    // other method. Returns false if any asset is missing/malformed.
    bool init(AAssetManager *assetManager);

    // Opens and starts the Oboe stream. Tied to process foreground/background (see
    // XylotuneApp's ProcessLifecycleOwner wiring), not the Activity lifecycle.
    void onStart();

    // Stops and closes the Oboe stream, releasing the low-latency HAL slot.
    void onStop();

    // Never blocks/locks/allocates — pushes into Mixer's lock-free ring buffer. Safe to
    // call rapidly from the UI thread.
    void playNote(int noteIndex, Material material);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData,
                                           int32_t numFrames) override;

private:
    SampleBank sampleBank_;
    Mixer mixer_;
    std::shared_ptr<oboe::AudioStream> stream_;
};

}  // namespace xylotune
