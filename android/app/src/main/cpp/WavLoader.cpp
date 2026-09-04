#include "WavLoader.h"

#include <cstring>

namespace xylotune {

namespace {

uint32_t readU32(const uint8_t *p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

uint16_t readU16(const uint8_t *p) {
    return static_cast<uint16_t>(p[0]) | (static_cast<uint16_t>(p[1]) << 8);
}

}  // namespace

WavData parseWav(const uint8_t *bytes, size_t length) {
    WavData result;
    if (length < 44) return result;
    if (std::memcmp(bytes, "RIFF", 4) != 0 || std::memcmp(bytes + 8, "WAVE", 4) != 0) {
        return result;
    }

    size_t pos = 12;
    uint16_t numChannels = 0;
    uint32_t sampleRate = 0;
    uint16_t bitsPerSample = 0;
    const uint8_t *dataStart = nullptr;
    uint32_t dataSize = 0;

    while (pos + 8 <= length) {
        char chunkId[5] = {0};
        std::memcpy(chunkId, bytes + pos, 4);
        uint32_t chunkSize = readU32(bytes + pos + 4);
        const uint8_t *chunkData = bytes + pos + 8;
        if (pos + 8 + chunkSize > length) break;

        if (std::memcmp(chunkId, "fmt ", 4) == 0 && chunkSize >= 16) {
            numChannels = readU16(chunkData + 2);
            sampleRate = readU32(chunkData + 4);
            bitsPerSample = readU16(chunkData + 14);
        } else if (std::memcmp(chunkId, "data", 4) == 0) {
            dataStart = chunkData;
            dataSize = chunkSize;
        }

        pos += 8 + chunkSize + (chunkSize % 2);  // chunks are word-aligned
    }

    if (dataStart == nullptr || numChannels != 1 || bitsPerSample != 16 || sampleRate == 0) {
        return result;
    }

    size_t frameCount = dataSize / 2;
    result.samples.resize(frameCount);
    for (size_t i = 0; i < frameCount; i++) {
        int16_t raw = static_cast<int16_t>(readU16(dataStart + i * 2));
        result.samples[i] = static_cast<float>(raw) / 32768.0f;
    }
    result.sampleRate = static_cast<int32_t>(sampleRate);
    return result;
}

}  // namespace xylotune
