#pragma once

#include <android/asset_manager.h>

#include <array>
#include <vector>

namespace xylotune {

constexpr int kNoteCount = 8;

enum class Material : int { Metal = 0, Wood = 1 };
constexpr int kMaterialCount = 2;

// Owns the 16 decoded samples (8 notes x {metal, wood}) for the lifetime of the process.
// Populated once from nativeInit; read-only from every thread after that (including the
// audio callback thread), so no locking is needed once load() has returned.
class SampleBank {
public:
    // Reads assets/samples/{metal,wood}_{C,D,E,F,G,A,B,C2}.wav via AAssetManager. Returns
    // false (leaving the bank empty) if any asset is missing or fails to parse — the
    // caller decides how to surface that rather than this class asserting/crashing.
    bool load(AAssetManager *assetManager);

    const float *samples(Material material, int noteIndex, size_t *outLength) const;
    int32_t sampleRate() const { return sampleRate_; }

private:
    std::array<std::array<std::vector<float>, kNoteCount>, kMaterialCount> samples_;
    int32_t sampleRate_ = 0;
};

}  // namespace xylotune
