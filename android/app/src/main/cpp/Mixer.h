#pragma once

#include <atomic>
#include <array>
#include <cstdint>

#include "SampleBank.h"

namespace xylotune {

// Deliberately has zero dependency on Oboe or any Android API, so it can be exercised by
// a plain host-side test (see the plan's M1 note on GoogleTest coverage) despite there
// being no device/emulator available while this app was built.
//
// Threading contract: requestPlay() is called from the UI/JNI thread (the producer) and
// must never block, lock, or allocate. renderStereo() is called from the audio callback
// thread (the sole consumer) at real-time priority and must also never block, lock, or
// allocate — voices live in a fixed-size pool, and the ring buffer between the two
// threads is lock-free (single-producer/single-consumer).
class Mixer {
public:
    static constexpr int kVoiceCount = 24;
    static constexpr int kRingCapacity = 32;  // power of two; see requestPlay()

    void setSampleBank(const SampleBank *bank) { sampleBank_ = bank; }

    // Producer side. Drops the request (rather than blocking) if the ring is full — see
    // the plan's note on why that's an acceptable, documented trade-off, not a bug.
    void requestPlay(int noteIndex, Material material);

    // Consumer side: drains the ring buffer, starts/steals voices, and additively mixes
    // every active voice into `output` (interleaved stereo, `numFrames` frames).
    void renderStereo(float *output, int32_t numFrames);

private:
    struct PlayRequest {
        int noteIndex;
        Material material;
    };

    struct Voice {
        bool active = false;
        const float *data = nullptr;
        size_t length = 0;
        size_t readFrame = 0;
        float gainL = 1.0f;
        float gainR = 1.0f;
        uint64_t startedAtSeq = 0;  // for voice-stealing: steal the oldest
    };

    void drainRequests();
    void startVoice(int noteIndex, Material material);

    const SampleBank *sampleBank_ = nullptr;
    std::array<Voice, kVoiceCount> voices_;
    uint64_t voiceSeq_ = 0;

    // Lock-free SPSC ring buffer.
    std::array<PlayRequest, kRingCapacity> ring_;
    std::atomic<uint32_t> ringHead_{0};  // next slot the producer writes
    std::atomic<uint32_t> ringTail_{0};  // next slot the consumer reads
};

}  // namespace xylotune
