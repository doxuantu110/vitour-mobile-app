// ─────────────────────────────────────────────────────────────────────────────
// Project-level build.gradle.kts
//
// PURPOSE: Declare plugins that apply to ALL modules (app + any future libs).
//          Repositories for dependency resolution live in settings.gradle.kts.
//
// RULE: Never put module-specific deps here. Only plugin declarations.
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    // Android Gradle Plugin — compiles, packages, signs the APK/AAB
    alias(libs.plugins.android.application) apply false

    // Google Services plugin — processes google-services.json for Firebase + Maps.
    // Must be applied AFTER android.application in every module that uses Firebase.
    alias(libs.plugins.google.services) apply false
}