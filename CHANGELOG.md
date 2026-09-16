# Changelog

All notable changes to **OpenCapsule** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-09-16

### Added
- **Dynamic Island Capsule Core**:
  - Tactile spring physics animations for collapsed pill and expanded card layouts.
  - Aligns natively around camera punch-holes (Center, Left, Right) with custom X/Y offsets.
- **Contextual Event Modules**:
  - **Media Player**: Track title, artist, live audio wave visualizer, album art, interactive seek progress, and playback transport controls.
  - **Countdown Timers & Stopwatch**: Live countdown, +1 Minute extension button, pause/resume, and reset.
  - **Phone Calls**: Incoming call banner with answer/decline actions and ongoing call elapsed duration.
  - **Battery & Charging**: Real-time feedback for AC fast charging, USB charging, and low battery alerts.
  - **Notification Alerts**: App icons, sender titles, and notification previews with optional privacy redaction.
  - **Headset & Audio Accessories**: Bluetooth and wired connection state notifications.
- **In-App Studio & Customization**:
  - Live simulator test bench for instant visual feedback on animations and alignment.
  - Dimension sliders (collapsed width/height, expanded width/height, corner radius).
  - Spring damping and stiffness sliders.
- **Privacy & Play Protect Compliance**:
  - 100% offline architecture: Manifest contains zero `android.permission.INTERNET`.
  - Zero analytics or tracking SDKs.
  - In-memory event stream: zero disk/database persistence of notification messages.
  - Pre-disclosure dialogs and in-app Permission Audit screen.
  - Automated build-time task `:app:verifyPlayProtectPrivacy`.
- **CI / CD**:
  - Automated GitHub Actions APK build and release publishing on every push to `main` and version tags (`v*`).
