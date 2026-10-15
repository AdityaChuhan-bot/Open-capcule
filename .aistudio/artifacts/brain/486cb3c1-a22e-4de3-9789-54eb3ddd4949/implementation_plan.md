# Dynamic Island Expansion & Unified Play-Safe APK Plan

Resolve Google Play Protect installation blocks, ensure 100% reliable Dynamic Island tap expansion with an interactive Quick Control Center, restore notification listener availability, remove the custom launcher activity, and configure a streamlined single-APK auto-release workflow.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following confirmed decisions from your input guide this implementation:

- **Confirmed Decision 1 (Packaging)**: Single unified APK (`OpenCapsule-release.apk`) containing all capabilities (overlay, notifications, media controls, battery/charging telemetry, and timer hooks) without separate stripped-down variants that cause confusion or missing permissions.
- **Confirmed Decision 2 (Idle Expansion View)**: When the Dynamic Island pill is tapped in idle state (no active music/timer), it will expand into a rich **Quick Control Center** displaying battery percentage/charging state, media playback shortcut, quick silence/sound toggle, and direct settings access.
- **Confirmed Decision 3 (Interaction Model)**: Tap on the capsule to expand with responsive haptic feedback; swipe up or tap outside to collapse back to the compact cutout pill.
- **Confirmed Decision 4 (Play Protect & Launcher Removal)**: Completely delete `LauncherActivity` and the `android.intent.category.HOME` intent filter from `AndroidManifest.xml` (which triggered Play Protect blocks), and eliminate the `core` flavor manifest override that was removing `CapsuleNotificationListenerService`.

---

## 1. Overview & Core Concept

- **What It Does**: OpenCapsule provides a fluid, hardware-aligned Dynamic Island overlay that sits neatly around the front camera cutout on any Android phone. It responds smoothly to taps by expanding into a modern pill card, reacts to active media and notifications, and runs completely offline with zero telemetry.
- **Target Audience / Persona**: Users on physical Android phones wanting native Dynamic Island functionality with seamless installation, zero Play Protect warnings, and zero permission hoops.
- **Key Value**: Direct installation without system blocking, complete notification listener visibility in system settings, and 100% reliable tap-to-expand responsiveness.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Download & Install**: User installs a single clean APK (`OpenCapsule-release.apk`). Android installs it directly without Play Protect blocking because no deceptive launcher (`category.HOME`) or suspicious stripped nodes exist.
2. **Permission Setup**: The setup wizard guides granting `Display over other apps` (Overlay) and `Notification Access` (Notification Listener Service). OpenCapsule is prominently listed in Android's notification access list.
3. **Idle Dynamic Island Interaction**:
   - The minimal camera pill sits around the cutout.
   - User **taps** the capsule: it smoothly expands using Apple-grade spring physics into the **Quick Control Center**.
   - The expanded card shows real-time battery status with charging indicator, current media session state with transport buttons, quick volume/sound mode icon, and an Open App shortcut.
   - User **swipes up** or taps the collapse handle: the island snaps smoothly back into the camera cutout pill.
4. **Active Event Expansion**: When music plays or notifications arrive, tapping expands directly into the full media player or interactive notification card with action buttons.

### Visual Identity & Theme
- **Theme**: Luxury OLED Pitch Black (`#000000`) with subtle translucent specular borders (`Color.White.copy(alpha = 0.12f)`), glowing electric blue accents (`#2563EB`), and M3 dark surfaces.
- **Motion & Physics**: Damped low-bouncy spring curves (`Spring.DampingRatioLowBouncy`, `Spring.StiffnessLow`) providing organic fluid stretching.
- **Touch Responsiveness**: Minimum 48dp touch targets, ripple feedback, and subtle tactile haptics (`HapticFeedbackType.LongPress` / `VibrationEffect.EFFECT_CLICK`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Removal of `LauncherActivity` and `category.HOME`**
  - *Chosen Approach*: Completely remove `LauncherActivity.kt` and its manifest entry.
  - *Why*: Google Play Protect strictly monitors apps sideloaded with `android.intent.category.HOME` because malicious apps masquerade as home launchers. OpenCapsule is an overlay utility, not a home launcher replacement. Removing this immediately clears the Play Protect block.
- **Decision 2: Elimination of `core` Flavor Stripping**
  - *Chosen Approach*: Remove `app/src/core/AndroidManifest.xml` (which removed `CapsuleNotificationListenerService`) and consolidate `build.gradle.kts` into standard debug and release builds.
  - *Why*: The user was only able to install the "core" APK, but "core" had the notification service stripped by manifest merge rules, making notification access impossible. A single full-featured build gives users everything working out of the box.
- **Decision 3: WindowManager Dynamic Resizing & Touch Handling**
  - *Chosen Approach*: In `CapsuleOverlayService`, when `isExpanded` transitions between true and false, update `WindowManager.LayoutParams` width and height dynamically, and fix touch dispatching so `.clickable` is never blocked by conflicting drag gesture recognizers.
  - *Why*: In standard Android overlay windows, a `WRAP_CONTENT` ComposeView animating its child size can suffer from clipped touch bounds or swallowed click events if gesture detectors conflict with click listeners.
- **Decision 4: Streamlined GitHub Actions Workflow**
  - *Chosen Approach*: Build and publish a clean `OpenCapsule-release.apk` and `OpenCapsule-debug.apk` directly to GitHub Releases under `latest`, with direct one-tap download URLs.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Android System                         │
│  [Camera Cutout Insets]    [MediaSession]   [Notifications] │
└──────────────┬───────────────────┬──────────────────┬───────┘
               │                   │                  │
               ▼                   ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    CapsuleManager (Core)                    │
│   - activeEvent: StateFlow<CapsuleEvent?>                   │
│   - isExpanded: MutableStateFlow<Boolean>                   │
│   - config: StateFlow<CapsuleConfig>                        │
│   - toggleExpand(), expandCapsule(), collapseCapsule()      │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│            CapsuleOverlayService (Foreground FGS)           │
│   - Manages WindowManager & TYPE_APPLICATION_OVERLAY        │
│   - Dynamic LayoutParams sizing (Collapsed vs Expanded)    │
│   - Cutout positioning alignment & orientation change       │
└──────────────┬──────────────────────────────────────────────┘
               │ ComposeView
               ▼
┌─────────────────────────────────────────────────────────────┐
│               OpenCapsuleDynamicView (Compose)              │
│   - Animated width, height, corner radius via spring()      │
│   - Unconflicted tap gesture + swipe-up collapse detection  │
│   ├── Collapsed: Minimal camera cutout pill                 │
│   └── Expanded:                                             │
│       ├── Event != null: Media / Notification / Call        │
│       └── Event == null: Quick Control Center               │
│           (Battery & Charging, Media Mini, Tool Shortcuts)  │
└─────────────────────────────────────────────────────────────┘
```

### Component Implementation Steps:
1. **Remove Launcher**: Delete `LauncherActivity.kt` and remove its activity declaration from `app/src/main/AndroidManifest.xml`.
2. **Remove Core Flavor Manifest**: Delete `app/src/core/AndroidManifest.xml` and simplify `build.gradle.kts` to eliminate flavor dimensions.
3. **Fix WindowManager Overlay Sizing & Gestures**:
   - Update `CapsuleOverlayService.kt` to observe `CapsuleManager.isExpanded` and dynamically update WindowManager bounds so expanded clicks always register.
   - Refactor `OpenCapsuleDynamicView.kt` pointer input so clicks and vertical drag collapse don't cancel each other.
4. **Implement Quick Control Center Layout**:
   - In `CapsuleContentLayouts.kt`, replace the static text placeholder with an interactive Control Center widget (Battery level, charging badge, music playback toggle, volume/sound toggle, and app launcher).
5. **Update GitHub Actions Workflow**:
   - Update `.github/workflows/build-apk.yml` to assemble `:app:assembleRelease` and `:app:assembleDebug`, publishing clear single APKs labeled `OpenCapsule-release.apk`.
6. **Verify & Package**: Run `compile_applet` and regenerate `opencapsule_merged.zip`.
