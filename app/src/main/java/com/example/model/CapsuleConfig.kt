package com.example.model

enum class CapsulePosition {
  CENTER,
  LEFT,
  RIGHT
}

enum class AnimationSpeed {
  SLOW,
  NORMAL,
  FAST,
  SPRINGY
}

enum class CapsuleThemeMode {
  SYSTEM,
  DARK,
  LIGHT,
  DYNAMIC_MATERIAL_YOU
}

data class CapsuleConfig(
  val isOverlayEnabled: Boolean = false,
  val isOnboardingCompleted: Boolean = false,
  val position: CapsulePosition = CapsulePosition.CENTER,
  val offsetX: Int = 0,
  val offsetY: Int = 12,
  val collapsedWidthDp: Int = 140,
  val collapsedHeightDp: Int = 36,
  val expandedWidthDp: Int = 340,
  val expandedHeightDp: Int = 150,
  val cornerRadiusDp: Int = 22,
  val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
  val themeMode: CapsuleThemeMode = CapsuleThemeMode.DYNAMIC_MATERIAL_YOU,
  val notificationPreviewsEnabled: Boolean = true,
  val autoCollapseTimeoutSeconds: Int = 5,
  val alwaysShowWhenIdle: Boolean = false,

  // Feature Toggles
  val featureNotifications: Boolean = true,
  val featureMedia: Boolean = true,
  val featureCalls: Boolean = true,
  val featureCharging: Boolean = true,
  val featureTimers: Boolean = true,
  val featureBluetooth: Boolean = true,
  val featureDownloads: Boolean = true,
  val featureSystemEvents: Boolean = true,

  // Blocked package names for notifications
  val blockedPackages: Set<String> = emptySet()
)
