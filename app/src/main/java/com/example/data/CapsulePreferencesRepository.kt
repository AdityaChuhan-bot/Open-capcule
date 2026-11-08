package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AnimationSpeed
import com.example.model.AnimationStyle
import com.example.model.CapsuleConfig
import com.example.model.CapsulePosition
import com.example.model.CapsuleThemeMode
import com.example.model.ExpansionBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opencapsule_preferences")

class CapsulePreferencesRepository(private val context: Context) {

  private object Keys {
    val IS_OVERLAY_ENABLED = booleanPreferencesKey("is_overlay_enabled")
    val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    val AUTO_ALIGN_CUTOUT = booleanPreferencesKey("auto_align_cutout")
    val POSITION = stringPreferencesKey("position")
    val OFFSET_X = intPreferencesKey("offset_x")
    val OFFSET_Y = intPreferencesKey("offset_y")
    val COLLAPSED_WIDTH = intPreferencesKey("collapsed_width")
    val COLLAPSED_HEIGHT = intPreferencesKey("collapsed_height")
    val COMPACT_IDLE_NOTCH = booleanPreferencesKey("compact_idle_notch")
    val IDLE_WIDTH = intPreferencesKey("idle_width")
    val IDLE_HEIGHT = intPreferencesKey("idle_height")
    val EXPANDED_WIDTH = intPreferencesKey("expanded_width")
    val EXPANDED_HEIGHT = intPreferencesKey("expanded_height")
    val EXPANDED_OFFSET_Y_EXTRA = intPreferencesKey("expanded_offset_y_extra")
    val CORNER_RADIUS = intPreferencesKey("corner_radius")

    // Animation preferences
    val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
    val ANIMATION_SPEED = stringPreferencesKey("animation_speed")
    val ANIMATION_STYLE = stringPreferencesKey("animation_style")
    val EXPANSION_BEHAVIOR = stringPreferencesKey("expansion_behavior")
    val SWIPE_UP_COLLAPSE = booleanPreferencesKey("swipe_up_collapse")
    val TAP_OUTSIDE_DISMISS = booleanPreferencesKey("tap_outside_dismiss")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIF_PREVIEWS = booleanPreferencesKey("notif_previews")
    val AUTO_COLLAPSE_TIMEOUT = intPreferencesKey("auto_collapse_timeout")
    val ALWAYS_SHOW_IDLE = booleanPreferencesKey("always_show_idle")

    val FEAT_NOTIF = booleanPreferencesKey("feat_notif")
    val FEAT_MEDIA = booleanPreferencesKey("feat_media")
    val FEAT_CHARGING = booleanPreferencesKey("feat_charging")


    // Per-app filtering
    val NOTIF_FILTER_ENABLED = booleanPreferencesKey("notif_filter_enabled")
    val ALLOWED_NOTIF_PACKAGES = stringSetPreferencesKey("allowed_notif_packages")
    val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
  }

  val configFlow: Flow<CapsuleConfig> = context.dataStore.data.map { prefs ->
    CapsuleConfig(
      isOverlayEnabled = prefs[Keys.IS_OVERLAY_ENABLED] ?: false,
      isOnboardingCompleted = prefs[Keys.IS_ONBOARDING_COMPLETED] ?: false,
      autoAlignCutout = prefs[Keys.AUTO_ALIGN_CUTOUT] ?: true,
      position = try {
        CapsulePosition.valueOf(prefs[Keys.POSITION] ?: CapsulePosition.CENTER.name)
      } catch (e: Exception) {
        CapsulePosition.CENTER
      },
      offsetX = prefs[Keys.OFFSET_X] ?: 0,
      offsetY = prefs[Keys.OFFSET_Y] ?: 12,
      collapsedWidthDp = prefs[Keys.COLLAPSED_WIDTH] ?: 140,
      collapsedHeightDp = prefs[Keys.COLLAPSED_HEIGHT] ?: 36,
      compactIdleNotch = prefs[Keys.COMPACT_IDLE_NOTCH] ?: true,
      idleWidthDp = prefs[Keys.IDLE_WIDTH] ?: 36,
      idleHeightDp = prefs[Keys.IDLE_HEIGHT] ?: 36,
      expandedWidthDp = prefs[Keys.EXPANDED_WIDTH] ?: 340,
      expandedHeightDp = prefs[Keys.EXPANDED_HEIGHT] ?: 150,
      expandedOffsetYExtraDp = prefs[Keys.EXPANDED_OFFSET_Y_EXTRA] ?: 20,
      cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 22,

      animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
      animationSpeed = try {
        AnimationSpeed.valueOf(prefs[Keys.ANIMATION_SPEED] ?: AnimationSpeed.NORMAL.name)
      } catch (e: Exception) {
        AnimationSpeed.NORMAL
      },
      animationStyle = try {
        AnimationStyle.valueOf(prefs[Keys.ANIMATION_STYLE] ?: AnimationStyle.SPRING.name)
      } catch (e: Exception) {
        AnimationStyle.SPRING
      },
      expansionBehavior = try {
        ExpansionBehavior.valueOf(prefs[Keys.EXPANSION_BEHAVIOR] ?: ExpansionBehavior.CENTER_EXPAND.name)
      } catch (e: Exception) {
        ExpansionBehavior.CENTER_EXPAND
      },
      swipeUpToCollapseEnabled = prefs[Keys.SWIPE_UP_COLLAPSE] ?: true,
      tapOutsideToDismissEnabled = prefs[Keys.TAP_OUTSIDE_DISMISS] ?: true,
      hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,

      themeMode = try {
        CapsuleThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: CapsuleThemeMode.DYNAMIC_MATERIAL_YOU.name)
      } catch (e: Exception) {
        CapsuleThemeMode.DYNAMIC_MATERIAL_YOU
      },
      notificationPreviewsEnabled = prefs[Keys.NOTIF_PREVIEWS] ?: true,
      autoCollapseTimeoutSeconds = prefs[Keys.AUTO_COLLAPSE_TIMEOUT] ?: 5,
      alwaysShowWhenIdle = prefs[Keys.ALWAYS_SHOW_IDLE] ?: false,

      featureNotifications = prefs[Keys.FEAT_NOTIF] ?: true,
      featureMedia = prefs[Keys.FEAT_MEDIA] ?: true,
      featureCharging = prefs[Keys.FEAT_CHARGING] ?: true,

      notificationFilterEnabled = prefs[Keys.NOTIF_FILTER_ENABLED] ?: false,
      allowedNotificationPackages = prefs[Keys.ALLOWED_NOTIF_PACKAGES] ?: emptySet(),
      blockedPackages = prefs[Keys.BLOCKED_PACKAGES] ?: emptySet()
    )

  }

  suspend fun updateConfig(update: (CapsuleConfig) -> CapsuleConfig) {
    context.dataStore.edit { prefs ->
      val current = CapsuleConfig(
        isOverlayEnabled = prefs[Keys.IS_OVERLAY_ENABLED] ?: false,
        isOnboardingCompleted = prefs[Keys.IS_ONBOARDING_COMPLETED] ?: false,
        autoAlignCutout = prefs[Keys.AUTO_ALIGN_CUTOUT] ?: true,
        position = try {
          CapsulePosition.valueOf(prefs[Keys.POSITION] ?: CapsulePosition.CENTER.name)
        } catch (e: Exception) {
          CapsulePosition.CENTER
        },
        offsetX = prefs[Keys.OFFSET_X] ?: 0,
        offsetY = prefs[Keys.OFFSET_Y] ?: 12,
        collapsedWidthDp = prefs[Keys.COLLAPSED_WIDTH] ?: 140,
        collapsedHeightDp = prefs[Keys.COLLAPSED_HEIGHT] ?: 36,
        compactIdleNotch = prefs[Keys.COMPACT_IDLE_NOTCH] ?: true,
        idleWidthDp = prefs[Keys.IDLE_WIDTH] ?: 36,
        idleHeightDp = prefs[Keys.IDLE_HEIGHT] ?: 36,
        expandedWidthDp = prefs[Keys.EXPANDED_WIDTH] ?: 340,
        expandedHeightDp = prefs[Keys.EXPANDED_HEIGHT] ?: 150,
        expandedOffsetYExtraDp = prefs[Keys.EXPANDED_OFFSET_Y_EXTRA] ?: 20,
        cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 22,

        animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
        animationSpeed = try {
          AnimationSpeed.valueOf(prefs[Keys.ANIMATION_SPEED] ?: AnimationSpeed.NORMAL.name)
        } catch (e: Exception) {
          AnimationSpeed.NORMAL
        },
        animationStyle = try {
          AnimationStyle.valueOf(prefs[Keys.ANIMATION_STYLE] ?: AnimationStyle.SPRING.name)
        } catch (e: Exception) {
          AnimationStyle.SPRING
        },
        expansionBehavior = try {
          ExpansionBehavior.valueOf(prefs[Keys.EXPANSION_BEHAVIOR] ?: ExpansionBehavior.CENTER_EXPAND.name)
        } catch (e: Exception) {
          ExpansionBehavior.CENTER_EXPAND
        },
        swipeUpToCollapseEnabled = prefs[Keys.SWIPE_UP_COLLAPSE] ?: true,
        tapOutsideToDismissEnabled = prefs[Keys.TAP_OUTSIDE_DISMISS] ?: true,
        hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,

        themeMode = try {
          CapsuleThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: CapsuleThemeMode.DYNAMIC_MATERIAL_YOU.name)
        } catch (e: Exception) {
          CapsuleThemeMode.DYNAMIC_MATERIAL_YOU
        },
        notificationPreviewsEnabled = prefs[Keys.NOTIF_PREVIEWS] ?: true,
        autoCollapseTimeoutSeconds = prefs[Keys.AUTO_COLLAPSE_TIMEOUT] ?: 5,
        alwaysShowWhenIdle = prefs[Keys.ALWAYS_SHOW_IDLE] ?: false,

        featureNotifications = prefs[Keys.FEAT_NOTIF] ?: true,
        featureMedia = prefs[Keys.FEAT_MEDIA] ?: true,
        featureCharging = prefs[Keys.FEAT_CHARGING] ?: true,

        notificationFilterEnabled = prefs[Keys.NOTIF_FILTER_ENABLED] ?: false,
        allowedNotificationPackages = prefs[Keys.ALLOWED_NOTIF_PACKAGES] ?: emptySet(),
        blockedPackages = prefs[Keys.BLOCKED_PACKAGES] ?: emptySet()
      )

      val newConfig = update(current)

      prefs[Keys.IS_OVERLAY_ENABLED] = newConfig.isOverlayEnabled
      prefs[Keys.IS_ONBOARDING_COMPLETED] = newConfig.isOnboardingCompleted
      prefs[Keys.AUTO_ALIGN_CUTOUT] = newConfig.autoAlignCutout
      prefs[Keys.POSITION] = newConfig.position.name
      prefs[Keys.OFFSET_X] = newConfig.offsetX
      prefs[Keys.OFFSET_Y] = newConfig.offsetY
      prefs[Keys.COLLAPSED_WIDTH] = newConfig.collapsedWidthDp
      prefs[Keys.COLLAPSED_HEIGHT] = newConfig.collapsedHeightDp
      prefs[Keys.COMPACT_IDLE_NOTCH] = newConfig.compactIdleNotch
      prefs[Keys.IDLE_WIDTH] = newConfig.idleWidthDp
      prefs[Keys.IDLE_HEIGHT] = newConfig.idleHeightDp
      prefs[Keys.EXPANDED_WIDTH] = newConfig.expandedWidthDp
      prefs[Keys.EXPANDED_HEIGHT] = newConfig.expandedHeightDp
      prefs[Keys.EXPANDED_OFFSET_Y_EXTRA] = newConfig.expandedOffsetYExtraDp
      prefs[Keys.CORNER_RADIUS] = newConfig.cornerRadiusDp

      prefs[Keys.ANIMATIONS_ENABLED] = newConfig.animationsEnabled
      prefs[Keys.ANIMATION_SPEED] = newConfig.animationSpeed.name
      prefs[Keys.ANIMATION_STYLE] = newConfig.animationStyle.name
      prefs[Keys.EXPANSION_BEHAVIOR] = newConfig.expansionBehavior.name
      prefs[Keys.SWIPE_UP_COLLAPSE] = newConfig.swipeUpToCollapseEnabled
      prefs[Keys.TAP_OUTSIDE_DISMISS] = newConfig.tapOutsideToDismissEnabled
      prefs[Keys.HAPTIC_FEEDBACK] = newConfig.hapticFeedbackEnabled

      prefs[Keys.THEME_MODE] = newConfig.themeMode.name
      prefs[Keys.NOTIF_PREVIEWS] = newConfig.notificationPreviewsEnabled
      prefs[Keys.AUTO_COLLAPSE_TIMEOUT] = newConfig.autoCollapseTimeoutSeconds
      prefs[Keys.ALWAYS_SHOW_IDLE] = newConfig.alwaysShowWhenIdle

      prefs[Keys.FEAT_NOTIF] = newConfig.featureNotifications
      prefs[Keys.FEAT_MEDIA] = newConfig.featureMedia
      prefs[Keys.FEAT_CHARGING] = newConfig.featureCharging


      prefs[Keys.NOTIF_FILTER_ENABLED] = newConfig.notificationFilterEnabled
      prefs[Keys.ALLOWED_NOTIF_PACKAGES] = newConfig.allowedNotificationPackages
      prefs[Keys.BLOCKED_PACKAGES] = newConfig.blockedPackages
    }
  }
}
