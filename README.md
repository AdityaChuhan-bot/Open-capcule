# OpenCapsule

**Privacy-First Dynamic Island & Contextual Notification Capsule for Android**

OpenCapsule is an original, production-quality Android application inspired by the interaction model of floating status capsules. It brings contextual alerts, live media controls, active countdown timers, ongoing call summaries, and system status indicators directly around your front camera cutout—crafted with fluid spring physics and a strict **privacy-first, offline-only** architecture.

Designed from day one for legitimate Google Play distribution and 100% Play Protect compliance.

---

## Key Features

- **Live Media Controls**: Displays active music playback with album artwork, interactive seek progress, and responsive previous/play/pause/next controls via Android's legitimate `MediaSession` APIs.
- **Countdown Timers & Stopwatch**: Real-time timer alerts, +1 minute extension, and pause/reset controls.
- **Phone Call Presentations**: Displays incoming call alerts and elapsed duration for ongoing calls.
- **Battery & Charging Telemetry**: Instant feedback on AC Fast Charging, USB standard charging, and low battery alerts.
- **Contextual Notifications**: Minimal notification pills with configurable sensitive content redaction and per-app filtering.
- **Audio Accessory Status**: Bluetooth and wired headset connection events.
- **Interactive Live Studio**: In-app simulator test bench with live camera notch alignment and customizable offsets.
- **Comprehensive Notch Customization**: Full control over punch-hole position (Center, Left, Right), X/Y offsets, collapsed/expanded dimensions, corner radius, and spring animation damping.

---

## Strict Privacy & Play Protect Safeguards

OpenCapsule is architected to guarantee user privacy:

1. **Zero Internet Access**: No `android.permission.INTERNET` is declared in the manifest. The application cannot transmit data over the network or establish sockets.
2. **Zero Third-Party SDKs & Zero Tracking**: No Firebase Analytics, no advertising libraries, no telemetry SDKs, no behavioral tracking.
3. **Volatile Memory Only**: Notification alerts and playback metadata are kept in volatile memory only. No permanent logging, database caching, or disk persistence of notification text.
4. **No Accessibility Service Abuse**: OpenCapsule does not use or request an `AccessibilityService`. Overlays are rendered using standard `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
5. **No Private API Reflection**: Operates exclusively with official, public Android SDK APIs.

---

## Permissions & Data Safety

| Permission | Purpose | Optional |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Renders the capsule above active apps around the camera cutout | No |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Monitors selected notifications and active MediaSessions locally | Yes |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the overlay service active with an ongoing status notification | No |
| `POST_NOTIFICATIONS` | Displays the required foreground service notification with stop action | No |
| `VIBRATE` | Provides subtle tactile feedback on capsule interactions | No |
| `READ_PHONE_STATE` | Detects ongoing call duration | Yes |
| `BLUETOOTH_CONNECT` | Displays audio accessory connection status on Android 12+ | Yes |

---

## Build & Test

```bash
# Verify privacy policy & compile
gradle assembleDebug

# Run unit and compliance tests
gradle :app:testDebugUnitTest
```

---

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
