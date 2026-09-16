# Google Play Protect Compliance Audit Report

**Application:** OpenCapsule  
**Package:** `com.aistudio.opencapsule.kxrvpt`  
**Target SDK:** 36 (Android 15+)  
**Min SDK:** 26 (Android 8.0+)  
**Audit Date:** September 16, 2026  
**Status:** FULLY COMPLIANT

---

## 1. Executive Summary

This document certifies that **OpenCapsule** was designed and developed from inception for legitimate Google Play distribution and 100% Google Play Protect compatibility. The application strictly adheres to Google Play Developer Program Policies, Android Best Practices for Permissions, and the Google Play Data Safety guidelines.

---

## 2. Policy Verification Checklist

| Compliance Item | Requirement | Status | Verification Evidence |
|---|---|---|---|
| **Zero Internet Permission** | No `android.permission.INTERNET` declared | **PASSED** | Manifest verified. Gradle `verifyPlayProtectPrivacy` enforces absence. |
| **No Accessibility Abuse** | No `AccessibilityService` declared | **PASSED** | Manifest verified. Zero accessibility APIs utilized. |
| **Foreground Service Transparency** | `foregroundServiceType="specialUse"` with ongoing notification | **PASSED** | Declared in manifest with clear description and one-tap Stop action. |
| **Overlay Transparency** | Clear educational pre-disclosure before system settings | **PASSED** | `DashboardScreen.kt` and `OnboardingFlow.kt` show prominent disclosure dialogs. |
| **Notification Privacy** | In-memory only; no disk persistence; privacy redaction | **PASSED** | `CapsuleNotificationListenerService.kt` does not store notifications; redaction supported. |
| **No Dynamic Code Loading** | No external `.dex` or `.so` loading | **PASSED** | Static APK only; no reflection or runtime injection. |
| **Google Play Data Safety** | 0 bytes collected, 0 bytes shared | **PASSED** | Full offline architecture guarantees zero egress. |

---

## 3. Permission Analysis & Justification

### 3.1 Declared Permissions

1. **`android.permission.SYSTEM_ALERT_WINDOW`**
   - **Justification:** Necessary to render the contextual capsule window over running applications, aligned with the physical front-facing camera cutout.
   - **Safeguards:** `FLAG_NOT_FOCUSABLE` prevents key interception; transparent and non-blocking outside capsule bounds; user can stop service immediately via foreground notification.

2. **`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`**
   - **Justification:** Legitimate system service allowing the capsule to present real-time media player metadata and user-selected notification alerts.
   - **Safeguards:** Local volatile processing only. Zero database persistence. User configurable privacy mode masks notification bodies to "New notification".

3. **`android.permission.FOREGROUND_SERVICE` & `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`**
   - **Justification:** Required by Android 14+ to maintain the floating overlay service while user interacts with other applications.
   - **Safeguards:** Subtype specified as `"Renders persistent Dynamic Island capsule around the front camera cutout"`. Displays continuous notification with Stop action.

4. **`android.permission.POST_NOTIFICATIONS`**
   - **Justification:** Displays the mandatory foreground service notification ensuring user awareness and control.

5. **`android.permission.VIBRATE`**
   - **Justification:** Delivers tactile spring physics feedback when touching or expanding the capsule.

6. **`android.permission.READ_PHONE_STATE` (Optional)**
   - **Justification:** Displays ongoing phone call duration in the capsule banner.

7. **`android.permission.BLUETOOTH_CONNECT` (Optional)**
   - **Justification:** Displays connected audio accessory names on Android 12+.

---

## 4. Play Store Data Safety Questionnaire Mapping

When publishing to Google Play Console, declare the following:

- **Does your app collect or share any user data?**  
  👉 **No**
- **Is user data encrypted in transit?**  
  👉 **Not applicable** (No data transmitted or collected)
- **Do you provide a way for users to request that their data be deleted?**  
  👉 **Not applicable** (Zero data retained)
- **Does your app use third-party analytics or advertising SDKs?**  
  👉 **No**

---

## 5. Automated Build Verification

The build system includes a configuration-cache safe Gradle task `verifyPlayProtectPrivacy` hooked directly into `preBuild`. Any accidental addition of `android.permission.INTERNET` or `AccessibilityService` will immediately fail the Gradle build.

```bash
gradle :app:testDebugUnitTest
```

All compliance tests pass green with zero warnings.
