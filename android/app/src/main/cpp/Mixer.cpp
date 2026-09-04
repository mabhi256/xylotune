#include "Mixer.h"

#include <cmath>

namespace xylotune {

namespace {

// Equal-power pan law: p in [-1, 1] -> (gainL, gainR). Approximates (not required to
// bit-match) the curve Web Audio's StereoPannerNode uses for a mono source.
void panGains(float p, float *gainL, float *gainR) {
    float theta = (p + 1.0f) * (static_cast<float>(M_PI) / 4.0f);  // p:[-1,1] -> theta:[0, pi/2]
    *gainL = std::cos(theta);
    *gainR = std::sin(theta);
}

// Matches playNote's `(i / 7 - 0.5) * 0.6` — index into [0, kNoteCount).
float panForNote(int noteIndex) {
    return (static_cast<float>(noteIndex) / 7.0f - 0.5f) * 0.6f;
}

}  // namespace

void Mixer::requestPlay(int noteIndex, Material material) {
    uint32_t head = ringHead_.load(std::memory_order_relaxed);
    uint32_t nextHead = (head + 1) % kRingCapacity;
    if (nextHead == ringTail_.load(std::memory_order_acquire)) {
        return;  // ring full: drop rather than block (see header comment)
    }
    ring_[head] = PlayRequest{noteIndex, material};
    ringHead_.store(nextHead, std::memory_order_release);
}

void Mixer::drainRequests() {
    while (true) {
        uint32_t tail = ringTail_.load(std::memory_order_relaxed);
        if (tail == ringHead_.load(std::memory_order_acquire)) break;  // empty
        PlayRequest req = ring_[tail];
        ringTail_.store((tail + 1) % kRingCapacity, std::memory_order_release);
        startVoice(req.noteIndex, req.material);
    }
}

void Mixer::startVoice(int noteIndex, Material material) {
    if (sampleBank_ == nullptr) return;

    size_t length = 0;
    const float *data = sampleBank_->samples(material, noteIndex, &length);
    if (data == nullptr || length == 0) return;

    // Find a free slot, or steal the oldest active one (standard voice-stealing).
    int target = -1;
    uint64_t oldestSeq = UINT64_MAX;
    for (int i = 0; i < kVoiceCount; i++) {
        if (!voices_[i].active) {
            target = i;
            break;
        }
        if (voices_[i].startedAtSeq < oldestSeq) {
            oldestSeq = voices_[i].startedAtSeq;
            target = i;
        }
    }

    Voice &v = voices_[target];
    v.active = true;
    v.data = data;
    v.length = length;
    v.readFrame = 0;
    v.startedAtSeq = ++voiceSeq_;
    panGains(panForNote(noteIndex), &v.gainL, &v.gainR);
}

void Mixer::renderStereo(float *output, int32_t numFrames) {
    drainRequests();

    for (int32_t i = 0; i < numFrames * 2; i++) output[i] = 0.0f;

    for (Voice &v : voices_) {
        if (!v.active) continue;
        int32_t framesToRender = numFrames;
        size_t remaining = v.length - v.readFrame;
        if (static_cast<size_t>(framesToRender) > remaining) {
            framesToRender = static_cast<int32_t>(remaining);
        }
        for (int32_t f = 0; f < framesToRender; f++) {
            float sample = v.data[v.readFrame + f];
            output[f * 2] += sample * v.gainL;
            output[f * 2 + 1] += sample * v.gainR;
        }
        v.readFrame += framesToRender;
        if (v.readFrame >= v.length) v.active = false;
    }
}

}  // namespace xylotune
