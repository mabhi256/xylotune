// Thin JNI shims only — no logic lives here. Every extern "C" function here must have a
// matching `external fun` in AudioEngine.kt with an exactly matching signature: a
// mismatch compiles fine on both sides and only fails at runtime (UnsatisfiedLinkError),
// which is why this surface is kept deliberately small (see the plan's Risk Area 2).

#include <android/asset_manager_jni.h>
#include <jni.h>

#include "AudioEngine.h"

namespace {
xylotune::AudioEngine gEngine;  // one instance for the process; never destroyed
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_xylotune_app_audio_AudioEngine_nativeInit(JNIEnv *env, jobject /* thiz */,
                                                     jobject assetManager) {
    AAssetManager *am = AAssetManager_fromJava(env, assetManager);
    return gEngine.init(am) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_xylotune_app_audio_AudioEngine_nativeOnStart(JNIEnv * /* env */, jobject /* thiz */) {
    gEngine.onStart();
}

extern "C" JNIEXPORT void JNICALL
Java_com_xylotune_app_audio_AudioEngine_nativeOnStop(JNIEnv * /* env */, jobject /* thiz */) {
    gEngine.onStop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_xylotune_app_audio_AudioEngine_nativePlayNote(JNIEnv * /* env */, jobject /* thiz */,
                                                         jint noteIndex, jint material) {
    gEngine.playNote(noteIndex, static_cast<xylotune::Material>(material));
}
