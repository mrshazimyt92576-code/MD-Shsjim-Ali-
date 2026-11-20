# HDR Studios (v2.0.0)

Modern Android 15+ (API 35+) HDR & Dolby Vision–inspired video editing studio built with Kotlin, Jetpack Compose (Liquid Glass UI), Jetpack Media3 (`ExoPlayer` + `Transformer` + custom OpenGL ES `GlEffect`), and local Room persistence.

## Project Highlights
- **Version:** `2.0.0` (`versionCode = 10`)
- **Min SDK:** Android 15 (API 35) • **Target SDK:** 36
- **Direct Access:** Opens straight into the studio with no sign-in required; projects persist privately on-device via Room
- **UI System:** Liquid Glass / Frosted Glass design with Obsidian, Midnight, and System themes
- **Editor Viewing Modes:** Instant **Standard** vs **HDR Enhanced** segmented toggle during playback + draggable Before/After optical split comparison
- **Instant Multi-Language:** English, বাংলা, Español, हिन्दी, العربية (RTL), 日本語
- **Video Pipeline:** Real-time OpenGL ES dynamic range shader + hardware-guarded Media3 Transformer export to `Movies/HDR Studios` with original audio preserved
- **Ready-to-Install APK Included:** `HDR_Studios_v2.0.0.apk` (also inside `apk/HDR_Studios_v2.0.0_debug.apk` and `app/build/outputs/apk/debug/app-debug.apk`)

## Installing the Included APK
1. Extract this ZIP archive.
2. Tap `HDR_Studios_v2.0.0.apk` on your Android 15+ device to install directly.

## Building in Android Studio / Gradle
1. Open this folder in Android Studio.
2. Sync Gradle and run `assembleDebug` (or see `SIGNING_GUIDE.md` and `scripts/setup_release_keystore.sh` for signed release builds).
