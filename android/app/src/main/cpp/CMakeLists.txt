cmake_minimum_required(VERSION 3.22.1)
project(xylotune_audio)

find_package(oboe REQUIRED CONFIG)

add_library(xylotune_audio SHARED
    native-lib.cpp
    AudioEngine.cpp
    Mixer.cpp
    SampleBank.cpp
    WavLoader.cpp
)

target_link_libraries(xylotune_audio
    log
    android
    oboe::oboe
)

target_compile_options(xylotune_audio PRIVATE -Wall -Wextra)
