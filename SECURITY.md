# Security Policy & Architecture Guarantees

## Threat Model & Security Posture

OpenCapsule follows a defensive, least-privilege architecture designed to exceed the security standards required by Google Play Protect:

1. **Air-Gapped Operation**
   - The manifest intentionally excludes the `android.permission.INTERNET` permission.
   - Any attempt to add networking libraries or call network sockets will fail at runtime and is blocked at build time by our Gradle task `verifyPlayProtectPrivacy`.

2. **No Dynamic Code Loading (DCL)**
   - No external `.dex`, `.jar`, or `.apk` files are loaded dynamically at runtime.
   - Code execution is 100% deterministic and strictly compiled into the APK.

3. **No Private API Reflection**
   - No Java/Kotlin reflection is used to bypass Android platform restrictions or access non-SDK interfaces.

4. **No Accessibility Abuse**
   - Accessibility services can be leveraged inappropriately to scrape sensitive screen content or automate gestures. OpenCapsule strictly rejects using `AccessibilityService`. Overlays use legitimate `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.

5. **Volatile In-Memory Event Pipeline**
   - All events (`Media`, `Notification`, `Call`, `Timer`) exist only as in-memory Kotlin data classes.
   - Zero sensitive notification payload is committed to persistent databases or flash storage.

---

## Reporting a Security Vulnerability

If you identify a security issue or unexpected privilege escalation, please open an issue or pull request with reproducible steps in the project repository. Because the application is fully open-source and operates offline, all security aspects can be independently audited.
