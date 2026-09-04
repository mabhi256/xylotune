#pragma once

#include <cstdint>
#include <vector>

namespace xylotune {

// Parses the specific WAV shape render_samples.py writes: PCM, mono, 16-bit, single
// data chunk. Not a general-purpose WAV reader.
struct WavData {
    std::vector<float> samples;  // normalized to [-1, 1]
    int32_t sampleRate = 0;
};

// Returns an empty WavData (sampleRate == 0) if `bytes` isn't a well-formed WAV matching
// the shape above.
WavData parseWav(const uint8_t *bytes, size_t length);

}  // namespace xylotune
