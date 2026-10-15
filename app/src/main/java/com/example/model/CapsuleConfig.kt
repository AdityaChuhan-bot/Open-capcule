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

enum class AnimationStyle {
  SPRING,
  FADE,
  BOUNCE,
  SMOOTH,
  MINIMAL
}

enum class ExpansionBehavior {
  CENTER_EXPAND,
  DOWNWARD_EXPAND,
  POP_EXPAND
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
  val autoAlignCutout: Boolean = true,
  val position: CapsulePosition = CapsulePosition.CENTER,
  val offsetX: Int = 0,
  val offsetY: Int = 12,
  val collapsedWidthDp: Int = 140,
  val collapsedHeightDp: Int = 36,
  val expandedWidthDp: Int = 340,
  val expandedHeightDp: Int = 150,
  val cornerRadiusDp: Int = 22,

  // Customizable Animation Options
  val animationsEnabled: Boolean = true,
  val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
  val animationStyle: AnimationStyle = AnimationStyle.SPRING,
  val expansionBehavior: ExpansionBehavior = ExpansionBehavior.CENTER_EXPAND,
  val swipeUpToCollapseEnabled: Boolean = true,
  val tapOutsideToDismissEnabled: Boolean = true,
  val hapticFeedbackEnabled: Boolean = true,

  val themeMode: CapsuleThemeMode = CapsuleThemeMode.DYNAMIC_MATERIAL_YOU,
  val notificationPreviewsEnabled: Boolean = true,
  val consumeNotifications: Boolean = true,
  val autoCollapseTimeoutSeconds: Int = 5,

  // The overlay service owns the persistent idle state. This setting remains user-configurable,
  // but enabling the overlay from the dashboard also enables the idle pill explicitly.
  val alwaysShowWhenIdle: Boolean = true,

  val featureNotifications: Boolean = true,
  val featureMedia: Boolean = true,
  val featureCalls: Boolean = true,
  val featureCharging: Boolean = true,
  val featureTimers: Boolean = true,
  val featureBluetooth: Boolean = true,
  val featureDownloads: Boolean = true,
  val featureSystemEvents: Boolean = true,

  // Per-app notification filtering
  val notificationFilterEnabled: Boolean = false,
  val allowedNotificationPackages: Set<String> = emptySet(),
  val blockedPackages: Set<String> = emptySet()
)

