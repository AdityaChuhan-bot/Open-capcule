# Contributing to OpenCapsule

Thank you for your interest in contributing to **OpenCapsule**! We welcome contributions from the community to improve performance, enhance spring physics animations, and expand contextual notification modules.

---

## 🛡️ Core Principles (Non-Negotiable)

OpenCapsule is built upon strict privacy and security foundations designed for legitimate Play Protect compliance:

1. **Strict Zero-Internet Policy**:
   - `android.permission.INTERNET` must NEVER be added to `AndroidManifest.xml`.
   - All processing MUST be 100% offline and local to the physical device.
2. **No Accessibility Service Abuse**:
   - We do not use or declare an `AccessibilityService`.
   - Overlays use standard `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
3. **No Persistent Notification Logging**:
   - Notification text and media metadata must remain in volatile memory (`StateFlow`) and be immediately dropped on dismissal.
4. **Zero Third-Party Trackers**:
   - No analytics SDKs, advertising libraries, or telemetry frameworks.

---

## 🛠️ Development Setup

### Requirements
- **JDK**: Java 17 (Eclipse Temurin or OpenJDK 17)
- **Android SDK**: Compile SDK 36, Min SDK 26
- **Gradle**: 9.3.1 (managed automatically via `./gradlew`)

### Build & Verify
```bash
# Clone the repository
git clone https://github.com/your-username/OpenCapsule.git
cd OpenCapsule

# Run tests and privacy verification
./gradlew :app:testDebugUnitTest

# Build debug APK
./gradlew :app:assembleDebug
```

Output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📐 Code Style & Conventions

- **Language**: Kotlin 100%. Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **UI Framework**: Modern Jetpack Compose with Material 3 theming.
- **State Management**: MVVM with unidirectional data flow (`StateFlow`, `asStateFlow()`).
- **Commits**: Follow [Conventional Commits](https://www.conventionalcommits.org/) format:
  - `feat: add timer remaining time formatting`
  - `fix: correct punch-hole offset calculation in landscape`
  - `docs: update Play Protect audit report`
  - `test: add unit test for charging broadcast receiver`

---

## 🚀 Pull Request Process

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Commit your changes with clear messages.
3. Ensure all automated tests pass:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
4. Submit a Pull Request targeting the `main` branch.
5. GitHub Actions will automatically verify your build and validate that privacy standards are preserved.
