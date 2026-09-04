// Root project: no build logic of its own, just declares plugin versions once so
// every module applies them without repeating a version number.
//
// AGP 9+ compiles Kotlin itself (no more org.jetbrains.kotlin.android plugin) and bundles
// KGP 2.2.10 to do it — the Compose compiler and serialization plugins below are pinned to
// that same 2.2.10 so they aren't running against a different Kotlin compiler than AGP uses.
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
}
