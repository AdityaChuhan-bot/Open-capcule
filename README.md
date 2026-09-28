 <div align="center">

# 🌌 OpenCapsule

**Android Dynamic Island, launcher, and contextual control hub**

[![Build](https://github.com/AdityaChuhan-bot/Open-/actions/workflows/build-apk.yml/badge.svg)](https://github.com/AdityaChuhan-bot/Open-/actions)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android%20Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An open-source Android project exploring a camera-cutout-aware Dynamic Island experience, with a dedicated home-launcher entry point and planned control centre and notification hub.

</div>

---

## Project status

**Active development / experimental.** OpenCapsule is being expanded from a floating capsule utility into a launcher-based experience.

- A dedicated launcher activity and initial app-grid home screen have been added.
- The existing Studio/configuration activity remains separate.
- A cutout-aware overlay is used for the capsule when shown outside the launcher.
- Build and device validation are ongoing; behavior may vary by Android version and device.
- The launcher has not yet been verified as a complete replacement for the stock launcher on every device.
- Direct APK installation may be blocked by Google Play Protect. A successful CI build does not mean Play Protect will allow installation.

## Current direction

### Launcher home

The launcher entry point is separate from the OpenCapsule Studio screen. The initial home screen includes a clock/date and a grid of launchable apps. OpenCapsule is intended to be selectable as the Android Home app through system default-app settings.

Configurable dock/favorites, app search, real app icons, wallpaper options, and gesture customization remain in development.

### Dynamic Island

The capsule is designed to present compact and expanded contextual content around the front-camera cutout, including:

- Media metadata and playback controls
- Timer/countdown events
- Charging and battery state
- Audio accessory events
- Supported notification and call-related events

The overlay uses Android's application-overlay window capability when displayed over other apps. Positioning and touch behavior depend on display cutout geometry, system window rules, and OEM implementation; physical-device testing is required.

### Planned integrated modules

- **Control Centre:** app shortcuts and supported system controls/panels.
- **Notification Hub:** an in-launcher view for notifications made available through Android's notification-listener access.
- **Launcher customization:** favorites/dock, search, layout and appearance settings.

These modules are planned/in progress and should not be considered complete until implemented and tested.

---

## Privacy and permissions

OpenCapsule is designed to process supported events locally. Permission availability and actual behavior should be checked in the installed build and Android settings.

| Permission / access | Why it is used | Notes |
|---|---|---|
| Display over other apps (SYSTEM_ALERT_WINDOW) | Shows the capsule above other apps | User must grant overlay access for cross-app display. The launcher can show its own in-screen UI without relying on an overlay. |
| Notification access (BIND_NOTIFICATION_LISTENER_SERVICE) | Reads supported notification/media events for the capsule and planned hub | Optional and sensitive. Must be explicitly enabled by the user in Android settings. Some system notifications cannot be dismissed or mirrored. |
| Special-use foreground service | Keeps the overlay service active when enabled | Subject to Android background/foreground-service rules and device behavior. |
| Vibration (VIBRATE) | Optional haptic feedback | Used only for supported interactions. |

The source manifest is intended to avoid unnecessary network access and accessibility-service usage. Review the current manifest and build output for the exact permissions in a particular APK. No claim of Google Play Protect approval or compliance is made here.

---

## Installation and setup

### Install a build

1. Open [GitHub Actions](https://github.com/AdityaChuhan-bot/Open-/actions) or [Releases](https://github.com/AdityaChuhan-bot/Open-/releases).
2. Download an APK artifact/release that matches the build you want.
3. Install it only if Android permits and you trust the source.

**Play Protect:** Some sideloaded builds may be blocked with an “App blocked to protect your device” warning. This differs from an ordinary unknown-developer warning. Do not assume an “Install anyway” option will be available; follow Google's official guidance or use an appropriate distribution/review path. Do not disable device protection to install an unverified build.

### Select OpenCapsule as your Home app

After installing a build that includes the launcher activity:

1. Open Android **Settings**.
2. Find **Default apps** (wording may vary by Android version).
3. Open **Home app**.
4. Select **OpenCapsule**, if it appears.

If it does not appear, the installed build may not include a valid Home activity or may not have installed successfully. The launcher is still under development.

### Enable capsule overlay

To display the capsule over other apps:

1. Open OpenCapsule.
2. Open the relevant permission/setup screen.
3. Grant **Display over other apps** when prompted.
4. Start or enable the capsule service.

The exact menu names may differ by Android version and manufacturer.

### Enable notification features

To receive supported notifications in the capsule, enable OpenCapsule under Android's **Notification access** settings. Notification access is optional and sensitive. Without it, notification-driven features will not work; media or system-event behavior may still be available through other Android APIs.

---

## Build from source

Requirements: Android SDK/Android Studio or a compatible Gradle environment, JDK version supported by the project, and Android SDK platforms required by the Gradle configuration.

```bash
git clone https://github.com/AdityaChuhan-bot/Open-.git
cd Open-
chmod +x gradlew
./gradlew :app:testDebugUnitTest
```

If the full and core product flavors are present, build them with:

```bash
./gradlew :app:assembleFullRelease :app:assembleCoreRelease
```

The **full** variant is intended to include notification-listener functionality. The **core** diagnostic variant omits the notification listener to help isolate installation/security-warning behavior. Confirm generated artifact names and signing setup from the current CI run.

---

## CI builds

GitHub Actions configuration is maintained in the workflow file at .github/workflows/build-apk.yml.

Builds can run tests, assemble configured APK variants, and upload artifacts. A green workflow confirms that configured CI tasks completed; it does **not** guarantee installation on every device, Play Protect acceptance, Google Play approval, or physical-device functionality.

- [View workflow runs](https://github.com/AdityaChuhan-bot/Open-/actions)
- [View releases](https://github.com/AdityaChuhan-bot/Open-/releases)

---

## Contributing

Contributions are welcome. Please review CONTRIBUTING.md, open an issue for bugs or feature proposals, and include Android version/device details when reporting launcher, overlay, or permission behavior.

## License

Licensed under the Apache License, Version 2.0. See LICENSE.
