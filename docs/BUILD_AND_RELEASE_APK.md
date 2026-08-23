# Production Android APK Build Guide

## 1. Prerequisites
- JDK 17+
- Android SDK Build-Tools 34+
- Gradle 8.x

## 2. Compiling the Application

### Debug APK Build:
```bash
gradle :app:assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Release APK Build:
```bash
gradle :app:assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if keystore configured).

## 3. Configuration Properties
Ensure `app/build.gradle.kts` specifies:
* `applicationId = "com.aistudio.telecallertracker.v7"`
* `minSdk = 24`
* `targetSdk = 34`
* `versionCode = 1`
* `versionName = "1.0.0"`
