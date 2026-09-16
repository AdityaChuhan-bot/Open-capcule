# OpenCapsule Privacy Policy

**Effective Date:** January 1, 2026  
**Last Updated:** September 16, 2026

OpenCapsule ("the Application") is developed as an open-source, privacy-first Android application. This Privacy Policy explains our zero-data collection practices and architectural security guarantees.

---

## 1. Summary of Core Principles

- **No Internet Access:** OpenCapsule does not declare `android.permission.INTERNET` in its manifest. The application has no technical capability to communicate over any network or transmit data off the device.
- **No Data Collection:** We do not collect, store, sell, monetize, or transmit personal data, device identifiers, IP addresses, or telemetry.
- **No Third-Party SDKs:** The application contains zero advertising frameworks, zero analytic trackers, and zero external software development kits.

---

## 2. Notification Processing

When the user explicitly grants Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`):
- OpenCapsule receives notification callbacks locally on the device to render incoming alerts within the capsule UI.
- Notification text, sender names, and metadata are held purely in temporary memory (`StateFlow`) and discarded immediately when dismissed or replaced.
- No notification history is written to local storage, SQLite databases, Room databases, or cache files.
- Users can enable the **Privacy Redaction** setting to mask sensitive content, replacing message bodies with a generic *"New notification"* label.
- Users can revoke Notification Access at any time through Android System Settings.

---

## 3. Media Metadata & Session Controls

OpenCapsule utilizes the public `MediaSessionManager` and `MediaController` APIs to display song titles, artist names, playback progress, and album artwork:
- Album art and track titles are read in real-time from active media sessions and rendered in memory.
- No listening habits, music history, or media identifiers are cataloged or tracked.

---

## 4. Overlay & System State

- **System Alert Window (`SYSTEM_ALERT_WINDOW`):** Used solely to position and draw the dynamic capsule UI around the front camera cutout. OpenCapsule does not use this permission to intercept touches intended for other apps, capture screen content, or monitor keystrokes.
- **Battery & Charging:** Reads system broadcasts (`ACTION_BATTERY_CHANGED`) locally to show charging percentages and charging speeds.
- **Bluetooth & Headset State:** Detects local wired and wireless audio connections to display connection banners. No location scanning is performed.

---

## 5. Data Deletion

Because OpenCapsule collects zero persistent data and maintains no remote servers, uninstalling the application or revoking permissions immediately removes all operational state from the device.

---

## 6. Contact & Audits

For questions or security audits regarding this policy, please review the public source code or consult `PLAY_PROTECT_AUDIT.md`.
