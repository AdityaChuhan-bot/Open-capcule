<div align="center">

# 🌌 OpenCapsule

**Privacy-First Dynamic Island & Contextual Notification Capsule for Android**

[![Build & Release](https://github.com/OWNER/OpenCapsule/actions/workflows/build-apk.yml/badge.svg)](https://github.com/OWNER/OpenCapsule/actions/workflows/build-apk.yml)
[![Latest Release](https://img.shields.io/github/v/release/OWNER/OpenCapsule?color=00e5ff&label=Latest%20APK)](https://github.com/OWNER/OpenCapsule/releases/latest)
[![Android Min SDK](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-green?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Play Protect Offline](https://img.shields.io/badge/Play%20Protect-100%25%20Offline-brightgreen?logo=googleplay&logoColor=white)](PLAY_PROTECT_AUDIT.md)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

<br />

```
  ┌────────────────────────────────────────────────────────┐
  │   ●   OpenCapsule   •   Now Playing: Midnight Echo     │
  └────────────────────────────────────────────────────────┘
```

*An open-source, non-intrusive floating status capsule that transforms your camera punch-hole into an interactive control center.*

[Download Latest APK](https://github.com/OWNER/OpenCapsule/releases/latest) • [Play Protect Audit](PLAY_PROTECT_AUDIT.md) • [Privacy Policy](PRIVACY.md) • [Report Bug](.github/ISSUE_TEMPLATE/bug_report.md)

</div>

---

## ✨ Overview

**OpenCapsule** is an original Android application that brings dynamic contextual notifications to your device's front camera cutout. Built with tactile spring physics and Jetpack Compose, it presents live media playback, active countdown timers, incoming and ongoing phone calls, battery charging states, and audio accessory connections within a compact, fluid status pill.

Designed from inception for legitimate Google Play distribution with **100% Google Play Protect compliance** and zero data egress.

---

## 🎯 Key Features

| Feature | Description | Android API Used |
|---|---|---|
| 🎵 **Live Media Controller** | Album art, title, artist, live audio wave visualizer, interactive seek progress, previous/play/next controls | `MediaSessionManager` & `MediaController` |
| ⏱️ **Timers & Stopwatch** | Live countdown animation, instant `+1 Min` button, pause/resume, and reset actions | Local System Timers |
| 📞 **Call Durations** | Incoming call answering/declining banner and active call elapsed timer | `TelephonyManager` |
| ⚡ **Battery & Charging** | Real-time indicators for AC Fast Charging, USB standard charging, and low battery | `Intent.ACTION_BATTERY_CHANGED` |
| 🔔 **Contextual Alerts** | Minimalist notification pills with configurable sensitive content redaction | `NotificationListenerService` |
| 🎧 **Audio Accessories** | Automatic status banner when Bluetooth headphones or wired earphones connect | System Audio Broadcasts |
| 🎛️ **Notch Studio & Calibrator** | Real-time simulator test bench with punch-hole presets (Center, Left, Right), X/Y offsets, and size controls | In-App Compose Canvas |

---

## 🛡️ Privacy & Play Protect Guarantees

OpenCapsule implements a defensive architecture with verifiable privacy safeguards:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        OPENCAPSULE ARCHITECTURE                        │
│                                                                        │
│  [ Media Sessions ]  [ System Broadcasts ]  [ Notification Listener ] │
│           │                     │                       │              │
│           └─────────────────────┼───────────────────────┘              │
│                                 ▼                                      │
│                ┌──────────────────────────────────┐                    │
│                │  Volatile In-Memory Event Stream │                    │
│                │          (Kotlin StateFlow)      │                    │
│                └────────────────┬─────────────────┘                    │
│                                 │                                      │
│                                 ▼                                      │
│                ┌──────────────────────────────────┐                    │
│                │  Jetpack Compose Floating Window │                    │
│                │  (WindowManager.LayoutParams)    │                    │
│                └──────────────────────────────────┘                    │
│                                                                        │
│   ❌ NO INTERNET PERMISSION   ❌ NO ACCESSIBILITY ABUSE   ❌ NO STORAGE │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Air-Gapped Operation**: OpenCapsule does **NOT** declare `android.permission.INTERNET`. The app is physically incapable of making network connections or sending telemetry off-device.
2. **Build-Time Verification**: A dedicated Gradle task (`:app:verifyPlayProtectPrivacy`) fails the build immediately if networking or accessibility service declarations are introduced.
3. **Volatile In-Memory Processing**: Notification contents and playback metadata exist strictly in volatile memory (`StateFlow`) and are erased when dismissed. No database or disk logs are created.
4. **No Accessibility Abuse**: Overlays are rendered using standard `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` with `FLAG_NOT_FOCUSABLE`.
5. **Foreground Service Transparency**: An ongoing status notification with a one-tap Stop button provides complete user control.

---

## 📥 Download & Installation

### Option 1: Direct APK Download (Recommended)
1. Go to the [**Latest GitHub Release**](https://github.com/OWNER/OpenCapsule/releases/latest).
2. Download **`OpenCapsule-latest.apk`** (or `OpenCapsule-release.apk`).
3. Tap the downloaded file on your Android device to install.
4. **Google Play Protect Notice**: Because this APK is downloaded directly from GitHub rather than through Google Play Store indexing, Play Protect may prompt: *"Play Protect doesn't recognize this app's developer"*. Simply tap **More details** and select **Install anyway**.
5. Launch OpenCapsule and follow the onboarding setup.

### Option 2: Build from Source
```bash
# 1. Clone repository
git clone https://github.com/OWNER/OpenCapsule.git
cd OpenCapsule

# 2. Grant execute permission
chmod +x gradlew

# 3. Run compliance tests
./gradlew :app:testDebugUnitTest

# 4. Assemble release APK
./gradlew :app:assembleRelease
```
The compiled APK will be at `app/build/outputs/apk/release/app-release.apk`.

---

## 🤖 Automated CI / CD Workflow

Every commit pushed to `main` (or version tag `v*`) triggers the automated GitHub Actions workflow (`.github/workflows/build-apk.yml`):
- Sets up JDK 17 & Gradle 9.3.1.
- Executes local Play Protect verification and JVM unit tests.
- Compiles production release-signed and debug APKs using Android v1/v2/v3/v4 signature schemes.
- Generates SHA-256 checksums for download integrity.
- **Automatically publishes the latest APK directly to GitHub Releases** for immediate 1-click download.

---

## 📱 Permissions Breakdown

| Permission | Purpose | Status |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Renders the capsule overlay window around your camera cutout | Required |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Displays active media playback controls and selected notification alerts | Optional |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the overlay active with an ongoing status notification | Required |
| `POST_NOTIFICATIONS` | Displays the foreground service status notification with stop action | Required |
| `VIBRATE` | Provides subtle haptic feedback during interactive expansions | Required |

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, commit conventions, and our PR workflow.

---

## 📄 License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
