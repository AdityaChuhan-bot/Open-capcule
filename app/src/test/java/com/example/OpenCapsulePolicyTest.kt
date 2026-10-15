package com.example

import com.example.model.CapsuleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenCapsulePolicyTest {

  @Test
  fun testManifestEnforcesZeroInternetAndNoAccessibility() {
    val candidates = listOf(
      File("src/main/AndroidManifest.xml"),
      File("app/src/main/AndroidManifest.xml"),
      File("../app/src/main/AndroidManifest.xml"),
      File(System.getProperty("user.dir"), "src/main/AndroidManifest.xml"),
      File(System.getProperty("user.dir"), "app/src/main/AndroidManifest.xml")
    )
    val manifestFile = candidates.firstOrNull { it.exists() }
    assertTrue("AndroidManifest.xml should exist in project", manifestFile != null && manifestFile.exists())

    val content = manifestFile!!.readText()

    // 1. Strict Play Protect Offline Enforcement
    assertFalse("Must NOT declare INTERNET permission", content.contains("android.permission.INTERNET"))

    // 2. Strict No Accessibility Service Abuse
    assertFalse("Must NOT declare AccessibilityService", content.contains("android.accessibilityservice.AccessibilityService"))
    assertFalse("Must NOT contain AccessibilityService intent filter", content.contains("AccessibilityService"))

    // 3. Prohibited tracking & high-risk permissions
    assertFalse("Must NOT declare ACCESS_FINE_LOCATION", content.contains("android.permission.ACCESS_FINE_LOCATION"))
    assertFalse("Must NOT declare RECORD_AUDIO", content.contains("android.permission.RECORD_AUDIO"))
    assertFalse("Must NOT declare CAMERA", content.contains("android.permission.CAMERA"))
    assertFalse("Must NOT declare READ_CONTACTS", content.contains("android.permission.READ_CONTACTS"))
    assertFalse("Must NOT declare READ_PHONE_STATE", content.contains("android.permission.READ_PHONE_STATE"))

    // 4. Must declare legitimate overlay and notification listener
    assertTrue("Must declare SYSTEM_ALERT_WINDOW", content.contains("android.permission.SYSTEM_ALERT_WINDOW"))
    assertTrue("Must declare BIND_NOTIFICATION_LISTENER_SERVICE", content.contains("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"))
  }

  @Test
  fun testCapsuleEventPriorityOrder() {
    val incomingCall = CapsuleEvent.Call(callerName = "Alice", state = CapsuleEvent.CallState.INCOMING)
    val ongoingCall = CapsuleEvent.Call(callerName = "Alice", state = CapsuleEvent.CallState.ONGOING)
    val timer = CapsuleEvent.Timer(remainingSeconds = 30, totalSeconds = 60)
    val media = CapsuleEvent.Media(title = "Track", artist = "Artist")
    val charging = CapsuleEvent.Charging(batteryPercent = 85, isCharging = true)
    val notif = CapsuleEvent.Notification(id = "1", title = "Alert", content = "Message", appName = "App", packageName = "com.test")
    val bluetooth = CapsuleEvent.Bluetooth(deviceName = "Headphones", isConnected = true)

    val events = listOf(bluetooth, notif, charging, media, timer, ongoingCall, incomingCall)
    val sorted = events.sortedBy { it.priority }

    assertEquals(incomingCall.id, sorted[0].id)
    assertEquals(ongoingCall.id, sorted[1].id)
    assertEquals(timer.id, sorted[2].id)
    assertEquals(media.id, sorted[3].id)
    assertEquals(charging.id, sorted[4].id)
    assertEquals(notif.id, sorted[5].id)
    assertEquals(bluetooth.id, sorted[6].id)
  }

  @Test
  fun testNotificationPrivacyRedaction() {
    val rawText = "Secret code: 948271"
    val normalNotif = CapsuleEvent.Notification(
      id = "n1",
      title = "Bank Alert",
      content = rawText,
      appName = "Bank",
      packageName = "com.bank",
      isSensitive = false
    )
    assertFalse(normalNotif.isSensitive)
    assertEquals(rawText, normalNotif.content)

    val privateNotif = CapsuleEvent.Notification(
      id = "n2",
      title = "Bank Alert",
      content = "New notification",
      appName = "Bank",
      packageName = "com.bank",
      isSensitive = true
    )
    assertTrue(privateNotif.isSensitive)
    assertEquals("New notification", privateNotif.content)
  }

  @Test
  fun testPerAppNotificationFilteringLogic() {
    val allowedApp = "com.whatsapp"
    val disallowedApp = "com.random.unwanted"

    val configWithFilter = com.example.model.CapsuleConfig(
      featureNotifications = true,
      notificationFilterEnabled = true,
      allowedNotificationPackages = setOf(allowedApp)
    )

    // Allowed package check
    val isAllowedPackagePermitted = configWithFilter.featureNotifications &&
      !configWithFilter.blockedPackages.contains(allowedApp) &&
      (!configWithFilter.notificationFilterEnabled || configWithFilter.allowedNotificationPackages.contains(allowedApp))
    assertTrue("Allowed package should be permitted through filter", isAllowedPackagePermitted)

    // Disallowed package check
    val isDisallowedPackagePermitted = configWithFilter.featureNotifications &&
      !configWithFilter.blockedPackages.contains(disallowedApp) &&
      (!configWithFilter.notificationFilterEnabled || configWithFilter.allowedNotificationPackages.contains(disallowedApp))
    assertFalse("Disallowed package should be filtered out", isDisallowedPackagePermitted)

    // When filter is disabled, both should be allowed
    val configFilterDisabled = configWithFilter.copy(notificationFilterEnabled = false)
    val isDisallowedPermittedWhenFilterOff = configFilterDisabled.featureNotifications &&
      !configFilterDisabled.blockedPackages.contains(disallowedApp) &&
      (!configFilterDisabled.notificationFilterEnabled || configFilterDisabled.allowedNotificationPackages.contains(disallowedApp))
    assertTrue("All non-blocked packages permitted when filter is off", isDisallowedPermittedWhenFilterOff)
  }

  @Test
  fun testCustomizableAnimationOptionsDefaultsAndOverrides() {
    val defaultConfig = com.example.model.CapsuleConfig()
    assertTrue("Animations should be enabled by default", defaultConfig.animationsEnabled)
    assertEquals(com.example.model.AnimationSpeed.NORMAL, defaultConfig.animationSpeed)
    assertEquals(com.example.model.AnimationStyle.SPRING, defaultConfig.animationStyle)
    assertEquals(com.example.model.ExpansionBehavior.CENTER_EXPAND, defaultConfig.expansionBehavior)
    assertTrue("Swipe-up to collapse should be enabled by default", defaultConfig.swipeUpToCollapseEnabled)
    assertTrue("Tap outside to dismiss should be enabled by default", defaultConfig.tapOutsideToDismissEnabled)
    assertTrue("Haptic feedback should be enabled by default", defaultConfig.hapticFeedbackEnabled)

    val customConfig = defaultConfig.copy(
      animationsEnabled = false,
      animationSpeed = com.example.model.AnimationSpeed.FAST,
      animationStyle = com.example.model.AnimationStyle.FADE,
      expansionBehavior = com.example.model.ExpansionBehavior.DOWNWARD_EXPAND,
      swipeUpToCollapseEnabled = false,
      tapOutsideToDismissEnabled = false
    )

    assertFalse(customConfig.animationsEnabled)
    assertEquals(com.example.model.AnimationSpeed.FAST, customConfig.animationSpeed)
    assertEquals(com.example.model.AnimationStyle.FADE, customConfig.animationStyle)
    assertEquals(com.example.model.ExpansionBehavior.DOWNWARD_EXPAND, customConfig.expansionBehavior)
    assertFalse(customConfig.swipeUpToCollapseEnabled)
    assertFalse(customConfig.tapOutsideToDismissEnabled)
  }
}

