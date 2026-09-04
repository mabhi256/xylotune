#include "SampleBank.h"

#include <android/log.h>

#include <string>
#include <utility>

#include "WavLoader.h"

#define LOG_TAG "XylotuneSampleBank"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace xylotune {

namespace {

constexpr const char *kNoteFileNames[kNoteCount] = {"C", "D", "E", "F", "G", "A", "B", "C2"};
constexpr const char *kMaterialNames[kMaterialCount] = {"metal", "wood"};

}  // namespace

bool SampleBank::load(AAssetManager *assetManager) {
    for (int m = 0; m < kMaterialCount; m++) {
        for (int n = 0; n < kNoteCount; n++) {
            std::string path =
                std::string("samples/") + kMaterialNames[m] + "_" + kNoteFileNames[n] + ".wav";
            AAsset *asset = AAssetManager_open(assetManager, path.c_str(), AASSET_MODE_BUFFER);
            if (asset == nullptr) {
                LOGE("Missing asset: %s", path.c_str());
                return false;
            }
            const void *data = AAsset_getBuffer(asset);
            off_t length = AAsset_getLength(asset);
            WavData wav = parseWav(static_cast<const uint8_t *>(data), static_cast<size_t>(length));
            AAsset_close(asset);

            if (wav.sampleRate == 0) {
                LOGE("Failed to parse WAV: %s", path.c_str());
                return false;
            }
            if (sampleRate_ == 0) {
                sampleRate_ = wav.sampleRate;
            } else if (sampleRate_ != wav.sampleRate) {
                LOGE("Sample rate mismatch in %s: expected %d, got %d", path.c_str(),
                     sampleRate_, wav.sampleRate);
                return false;
            }
            samples_[m][n] = std::move(wav.samples);
        }
    }
    return true;
}

const float *SampleBank::samples(Material material, int noteIndex, size_t *outLength) const {
    const auto &buf = samples_[static_cast<int>(material)][noteIndex];
    *outLength = buf.size();
    return buf.data();
}

}  // namespace xylotune
