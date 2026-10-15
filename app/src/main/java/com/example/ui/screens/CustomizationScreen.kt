package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CapsuleManager
import com.example.model.AnimationSpeed
import com.example.model.AnimationStyle
import com.example.model.CapsuleConfig
import com.example.model.CapsulePosition
import com.example.model.CapsuleThemeMode
import com.example.model.ExpansionBehavior
import com.example.ui.theme.PrimaryBlue

@Composable
fun CustomizationScreen(
  config: CapsuleConfig,
  modifier: Modifier = Modifier
) {
  var showAppFilterDialog by remember { mutableStateOf(false) }

  if (showAppFilterDialog) {
    AppNotificationFilterDialog(
      config = config,
      onDismiss = { showAppFilterDialog = false }
    )
  }
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // -------------------------------------------------------------
    // CUTOUT ALIGNMENT & POSITION
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Camera Cutout Position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
          "Align OpenCapsule directly around your device's front camera hardware notch.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Auto-detect Camera Cutout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
              "Automatically positions capsule around physical camera notch with fallback.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = config.autoAlignCutout,
            onCheckedChange = { isChecked ->
              CapsuleManager.updateConfig { it.copy(autoAlignCutout = isChecked) }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
            modifier = Modifier.testTag("switch_auto_cutout")
          )
        }

        if (!config.autoAlignCutout) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = config.position == CapsulePosition.CENTER,
              onClick = { CapsuleManager.updateConfig { it.copy(position = CapsulePosition.CENTER) } },
              label = { Text("Center") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.position == CapsulePosition.LEFT,
              onClick = { CapsuleManager.updateConfig { it.copy(position = CapsulePosition.LEFT) } },
              label = { Text("Left Hole") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.position == CapsulePosition.RIGHT,
              onClick = { CapsuleManager.updateConfig { it.copy(position = CapsulePosition.RIGHT) } },
              label = { Text("Right Hole") },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Horizontal Offset Slider
        SettingSlider(
          label = "Horizontal Offset (X)",
          valueText = "${config.offsetX} dp",
          value = config.offsetX.toFloat(),
          valueRange = -120f..120f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(offsetX = it.toInt()) } }
        )

        // Vertical Offset Slider
        SettingSlider(
          label = "Vertical Offset (Y)",
          valueText = "${config.offsetY - 12} dp",
          value = (config.offsetY - 12).toFloat(),
          valueRange = -12f..88f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(offsetY = it.toInt() + 12) } }
        )
      }
    }

    // -------------------------------------------------------------
    // DIMENSIONS & SHAPE
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Dimensions & Geometry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        SettingSlider(
          label = "Collapsed Pill Width",
          valueText = "${config.collapsedWidthDp} dp",
          value = config.collapsedWidthDp.toFloat(),
          valueRange = 80f..260f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(collapsedWidthDp = it.toInt()) } }
        )

        SettingSlider(
          label = "Collapsed Pill Height",
          valueText = "${config.collapsedHeightDp} dp",
          value = config.collapsedHeightDp.toFloat(),
          valueRange = 24f..64f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(collapsedHeightDp = it.toInt()) } }
        )

        SettingSlider(
          label = "Expanded Card Width",
          valueText = "${config.expandedWidthDp} dp",
          value = config.expandedWidthDp.toFloat(),
          valueRange = 240f..420f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(expandedWidthDp = it.toInt()) } }
        )

        SettingSlider(
          label = "Expanded Card Height",
          valueText = "${config.expandedHeightDp} dp",
          value = config.expandedHeightDp.toFloat(),
          valueRange = 100f..360f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(expandedHeightDp = it.toInt()) } }
        )

        SettingSlider(
          label = "Corner Radius",
          valueText = "${config.cornerRadiusDp} dp",
          value = config.cornerRadiusDp.toFloat(),
          valueRange = 8f..36f,
          onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(cornerRadiusDp = it.toInt()) } }
        )
      }
    }

    // -------------------------------------------------------------
    // ANIMATION & MOTION DYNAMICS
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Animation, contentDescription = null, tint = PrimaryBlue)
            Text("Motion & Animations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Switch(
            checked = config.animationsEnabled,
            onCheckedChange = { isEnabled ->
              CapsuleManager.updateConfig { it.copy(animationsEnabled = isEnabled) }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
            modifier = Modifier.testTag("switch_enable_animations")
          )
        }

        Text(
          if (config.animationsEnabled)
            "Customizable Material 3 motion physics, speeds, and organic expansion behaviors."
          else
            "Animations disabled for instant snapping, reduced GPU overhead, and maximum performance.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (config.animationsEnabled) {
          // Animation Speed Selector
          Text("Animation Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = config.animationSpeed == AnimationSpeed.SLOW,
              onClick = { CapsuleManager.updateConfig { it.copy(animationSpeed = AnimationSpeed.SLOW) } },
              label = { Text("Slow", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationSpeed == AnimationSpeed.NORMAL,
              onClick = { CapsuleManager.updateConfig { it.copy(animationSpeed = AnimationSpeed.NORMAL) } },
              label = { Text("Normal", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationSpeed == AnimationSpeed.FAST,
              onClick = { CapsuleManager.updateConfig { it.copy(animationSpeed = AnimationSpeed.FAST) } },
              label = { Text("Fast", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationSpeed == AnimationSpeed.SPRINGY,
              onClick = { CapsuleManager.updateConfig { it.copy(animationSpeed = AnimationSpeed.SPRINGY) } },
              label = { Text("Springy", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
          }

          // Motion Style Selector (Spring, Fade, Bounce, Smooth, Minimal)
          Text("Motion Style", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = config.animationStyle == AnimationStyle.SPRING,
              onClick = { CapsuleManager.updateConfig { it.copy(animationStyle = AnimationStyle.SPRING) } },
              label = { Text("Spring", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationStyle == AnimationStyle.FADE,
              onClick = { CapsuleManager.updateConfig { it.copy(animationStyle = AnimationStyle.FADE) } },
              label = { Text("Fade", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationStyle == AnimationStyle.BOUNCE,
              onClick = { CapsuleManager.updateConfig { it.copy(animationStyle = AnimationStyle.BOUNCE) } },
              label = { Text("Bounce", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationStyle == AnimationStyle.SMOOTH,
              onClick = { CapsuleManager.updateConfig { it.copy(animationStyle = AnimationStyle.SMOOTH) } },
              label = { Text("Smooth", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.animationStyle == AnimationStyle.MINIMAL,
              onClick = { CapsuleManager.updateConfig { it.copy(animationStyle = AnimationStyle.MINIMAL) } },
              label = { Text("Minimal", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }

          // Expansion & Collapse Behavior Selector
          Text("Expansion & Reveal Behavior", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = config.expansionBehavior == ExpansionBehavior.CENTER_EXPAND,
              onClick = { CapsuleManager.updateConfig { it.copy(expansionBehavior = ExpansionBehavior.CENTER_EXPAND) } },
              label = { Text("Symmetric", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.expansionBehavior == ExpansionBehavior.DOWNWARD_EXPAND,
              onClick = { CapsuleManager.updateConfig { it.copy(expansionBehavior = ExpansionBehavior.DOWNWARD_EXPAND) } },
              label = { Text("Downward", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = config.expansionBehavior == ExpansionBehavior.POP_EXPAND,
              onClick = { CapsuleManager.updateConfig { it.copy(expansionBehavior = ExpansionBehavior.POP_EXPAND) } },
              label = { Text("Pop", fontSize = 12.sp) },
              modifier = Modifier.weight(1f)
            )
          }

          // Gestures & Interactions
          Text("Gestures & Interaction", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

          ToggleSetting(
            title = "Swipe-Up to Collapse",
            subtitle = "Swipe up on the expanded capsule to smoothly collapse back to minimal pill",
            checked = config.swipeUpToCollapseEnabled,
            onCheckedChange = { checked ->
              CapsuleManager.updateConfig { it.copy(swipeUpToCollapseEnabled = checked) }
            }
          )

          ToggleSetting(
            title = "Tap Outside to Dismiss",
            subtitle = "Tap empty screen area outside the capsule window to collapse it",
            checked = config.tapOutsideToDismissEnabled,
            onCheckedChange = { checked ->
              CapsuleManager.updateConfig { it.copy(tapOutsideToDismissEnabled = checked) }
            }
          )

          ToggleSetting(
            title = "Haptic Touch Feedback",
            subtitle = "Subtle tactile click when expanding, collapsing, or interacting with controls",
            checked = config.hapticFeedbackEnabled,
            onCheckedChange = { checked ->
              CapsuleManager.updateConfig { it.copy(hapticFeedbackEnabled = checked) }
            }
          )

          SettingSlider(
            label = "Auto-Collapse Timeout",
            valueText = "${config.autoCollapseTimeoutSeconds}s",
            value = config.autoCollapseTimeoutSeconds.toFloat(),
            valueRange = 3f..15f,
            onValueChange = { CapsuleManager.updateConfig { cfg -> cfg.copy(autoCollapseTimeoutSeconds = it.toInt()) } }
          )
        }
      }
    }

    // -------------------------------------------------------------
    // PER-APP NOTIFICATION FILTERING
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryBlue)
            Text("Per-App Notification Filtering", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Switch(
            checked = config.notificationFilterEnabled,
            onCheckedChange = { isEnabled ->
              CapsuleManager.updateConfig { it.copy(notificationFilterEnabled = isEnabled) }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
            modifier = Modifier.testTag("switch_notification_filter_card")
          )
        }

        Text(
          if (config.notificationFilterEnabled)
            "Active: Only notifications from checked apps will be processed and displayed in OpenCapsule."
          else
            "Inactive: Notifications from all applications are allowed (default). Turn on to select specific apps.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
          onClick = { showAppFilterDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_manage_allowed_apps"),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (config.notificationFilterEnabled)
              "Manage Allowed Apps (${config.allowedNotificationPackages.size} Selected)"
            else
              "Configure Allowed Apps (${config.allowedNotificationPackages.size} Selected)"
          )
        }
      }
    }

    // -------------------------------------------------------------
    // PRIVACY & BEHAVIOR
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text("Privacy & Display Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        ToggleSetting(
          title = "Notification Previews",
          subtitle = if (config.notificationPreviewsEnabled) "Display message snippet in capsule" else "Redacted to 'New Notification' for privacy",
          checked = config.notificationPreviewsEnabled,
          onCheckedChange = { checked ->
            CapsuleManager.updateConfig { it.copy(notificationPreviewsEnabled = checked) }
          }
        )

        ToggleSetting(
          title = "Always Show Minimal Pill",
          subtitle = "Display subtle camera notch accent even when no active events exist",
          checked = config.alwaysShowWhenIdle,
          onCheckedChange = { checked ->
            CapsuleManager.updateConfig { it.copy(alwaysShowWhenIdle = checked) }
          }
        )
      }
    }

    // -------------------------------------------------------------
    // FEATURE TOGGLES
    // -------------------------------------------------------------
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text("Active Event Modules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        ToggleSetting("Media Playback", "Music tracks, artwork and seek bar", config.featureMedia) { checked ->
          CapsuleManager.updateConfig { it.copy(featureMedia = checked) }
        }
        ToggleSetting("App Notifications", "Selected incoming alerts", config.featureNotifications) { checked ->
          CapsuleManager.updateConfig { it.copy(featureNotifications = checked) }
        }
        ToggleSetting("Phone Calls", "Incoming and ongoing call banner", config.featureCalls) { checked ->
          CapsuleManager.updateConfig { it.copy(featureCalls = checked) }
        }
        ToggleSetting("Timers & Stopwatch", "Live countdowns and stopwatch counters", config.featureTimers) { checked ->
          CapsuleManager.updateConfig { it.copy(featureTimers = checked) }
        }
        ToggleSetting("Battery & Charging", "Plugged in, fast charging and full alerts", config.featureCharging) { checked ->
          CapsuleManager.updateConfig { it.copy(featureCharging = checked) }
        }
        ToggleSetting("Headsets & Audio Devices", "Audio accessory connect alerts", config.featureBluetooth) { checked ->
          CapsuleManager.updateConfig { it.copy(featureBluetooth = checked) }
        }
        ToggleSetting("Downloads", "DownloadManager task completion", config.featureDownloads) { checked ->
          CapsuleManager.updateConfig { it.copy(featureDownloads = checked) }
        }
      }
    }

    // Reset button
    OutlinedButton(
      onClick = {
        CapsuleManager.updateConfig {
          CapsuleConfig(
            isOverlayEnabled = it.isOverlayEnabled,
            isOnboardingCompleted = it.isOnboardingCompleted
          )
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("btn_reset_defaults")
    ) {
      Icon(Icons.Default.RestartAlt, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Reset to Recommended Defaults")
    }
  }
}

@Composable
private fun SettingSlider(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      Text(valueText, fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
    )
  }
}

@Composable
private fun ToggleSetting(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
    )
  }
}
