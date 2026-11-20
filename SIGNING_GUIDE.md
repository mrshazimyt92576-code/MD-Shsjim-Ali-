# HDR Studios — Signing & Keystore Configuration Guide

This guide explains how to generate a valid release upload keystore and build signed APKs/AABs cleanly for Android 15+ devices without triggering unnecessary Play Protect / untrusted-build security warnings.

## 1. Debug Builds
- Debug builds are automatically signed using the pre-configured `debugConfig` in `app/build.gradle.kts` (`debug.keystore`).
- Do **not** ship debug builds to production stores.

## 2. Generate a Release Upload Keystore
Run the helper script or `keytool` command below to create an RSA-2048/4096 PKCS12 upload keystore outside version control:

```bash
bash scripts/setup_release_keystore.sh
```

Or manually:
```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12
```

## 3. Configure Environment Variables (CI / Local Release)
`app/build.gradle.kts` reads release credentials from environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) so secrets are never hardcoded in source control:

```bash
export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="<your-store-password>"
export KEY_PASSWORD="<your-key-password>"

gradle :app:assembleRelease
# Or App Bundle for Google Play:
gradle :app:bundleRelease
```

## 4. Register Release SHA-1 / SHA-256 with Firebase
After generating your release keystore, extract its fingerprints and add them in Firebase Console > Project Settings > Your Android App so Google Sign-In works on release builds:

```bash
keytool -list -v -keystore my-upload-key.jks -alias upload
```
