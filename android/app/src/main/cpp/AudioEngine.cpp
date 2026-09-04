#include "AudioEngine.h"

#include <android/log.h>

#include <memory>

#define LOG_TAG "XylotuneAudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace xylotune {

bool AudioEngine::init(AAssetManager *assetManager) {
    if (!sampleBank_.load(assetManager)) {
        LOGE("SampleBank failed to load");
        return false;
    }
    mixer_.setSampleBank(&sampleBank_);
    return true;
}

namespace {

std::shared_ptr<oboe::AudioStream> openStream(oboe::SharingMode sharingMode,
                                               int32_t sampleRate,
                                               oboe::AudioStreamDataCallback *callback) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(sharingMode)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Stereo)
        ->setSampleRate(sampleRate)
        ->setDataCallback(callback);

    std::shared_ptr<oboe::AudioStream> stream;
    oboe::Result result = builder.openStream(stream);
    if (result != oboe::Result::OK) {
        LOGW("openStream(%s) failed: %s", oboe::convertToText(sharingMode),
             oboe::convertToText(result));
        return nullptr;
    }
    return stream;
}

}  // namespace

void AudioEngine::onStart() {
    if (stream_ != nullptr) return;  // already open (e.g. duplicate lifecycle callback)

    // Exclusive mode gives the lowest latency but isn't available on every device/config;
    // fall back to Shared (still using LowLatency performance mode, so still good) rather
    // than leaving the instrument silent when Exclusive is refused.
    stream_ = openStream(oboe::SharingMode::Exclusive, sampleBank_.sampleRate(), this);
    if (stream_ == nullptr) {
        stream_ = openStream(oboe::SharingMode::Shared, sampleBank_.sampleRate(), this);
    }
    if (stream_ == nullptr) {
        LOGE("Failed to open an audio stream in either sharing mode");
        return;
    }

    oboe::Result result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
    }
}

void AudioEngine::onStop() {
    if (stream_ == nullptr) return;
    stream_->stop();
    stream_->close();
    stream_ = nullptr;
}

void AudioEngine::playNote(int noteIndex, Material material) {
    if (noteIndex < 0 || noteIndex >= kNoteCount) {
        LOGW("playNote: noteIndex %d out of range", noteIndex);
        return;
    }
    mixer_.requestPlay(noteIndex, material);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *stream, void *audioData,
                                                    int32_t numFrames) {
    (void)stream;
    mixer_.renderStereo(static_cast<float *>(audioData), numFrames);
    return oboe::DataCallbackResult::Continue;
}

}  // namespace xylotune
