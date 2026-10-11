package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.CapsuleManager
import com.example.core.DemoEventType
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.model.CapsulePosition
import com.example.service.CapsuleOverlayService
import com.example.ui.capsule.OpenCapsuleDynamicView
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.CapsuleDarkBg
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryIndigo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  config: CapsuleConfig,
  activeEvent: CapsuleEvent?,
  isExpanded: Boolean,
  onOpenAudit: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showOverlayDisclosure by remember { mutableStateOf(false) }
  var showNotifDisclosure by remember { mutableStateOf(false) }

  val lifecycleOwner = LocalLifecycleOwner.current
  var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
  var isNotifListenerGranted by remember {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    mutableStateOf(flat != null && flat.contains(context.packageName))
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val overlayNow = Settings.canDrawOverlays(context)
        isOverlayGranted = overlayNow
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isNotifListenerGranted = flat != null && flat.contains(context.packageName)

        // If user returned with overlay permission granted, ensure service is running and config is active
        if (overlayNow && !config.isOverlayEnabled) {
          CapsuleManager.updateConfig { it.copy(isOverlayEnabled = true) }
          CapsuleOverlayService.startService(context)
        } else if (overlayNow && config.isOverlayEnabled) {
          CapsuleOverlayService.startService(context)
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // -------------------------------------------------------------
    // LIVE INTERACTIVE CAPSULE STUDIO / CUTOUT VIEWPORT
    // -------------------------------------------------------------
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("interactive_studio_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Live Capsule Studio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (config.autoAlignCutout) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF10B981).copy(alpha = 0.15f))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Auto-Cutout",
                  color = Color(0xFF10B981),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryBlue.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (isExpanded) "Expanded" else "Collapsed",
                color = PrimaryBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Text(
          text = "Tap or drag capsule to interact with real spring physics",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Mock Screen / Camera cutout viewport
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.TopCenter
        ) {
          // Simulated camera punch-hole cutout
          val effectivePosition = if (config.autoAlignCutout) CapsulePosition.CENTER else config.position
          val cutoutAlignment = when (effectivePosition) {
            CapsulePosition.CENTER -> Alignment.TopCenter
            CapsulePosition.LEFT -> Alignment.TopStart
            CapsulePosition.RIGHT -> Alignment.TopEnd
          }
          Box(
            modifier = Modifier
              .padding(
                top = 10.dp,
                start = if (effectivePosition == CapsulePosition.LEFT) 32.dp else 0.dp,
                end = if (effectivePosition == CapsulePosition.RIGHT) 32.dp else 0.dp
              )
              .align(cutoutAlignment)
          ) {
            // Cutout lens circle
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF000000))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
          }

          // The Dynamic Capsule itself
          Box(
            modifier = Modifier
              .padding(
                top = config.offsetY.dp,
                start = if (effectivePosition == CapsulePosition.LEFT) config.offsetX.dp else 0.dp,
                end = if (effectivePosition == CapsulePosition.RIGHT) config.offsetX.dp else 0.dp
              )
              .align(cutoutAlignment)
          ) {
            OpenCapsuleDynamicView(
              event = activeEvent,
              isExpanded = isExpanded,
              config = config,
              onToggleExpand = { CapsuleManager.toggleExpand() },
              onCollapse = { CapsuleManager.collapseCapsule() }
            )
          }
        }
      }
    }

    // -------------------------------------------------------------
    // SYSTEM PERMISSION & SERVICE STATUS
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
        Text(
          text = "System Overlay & Protection",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        // Overlay toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Display Over Other Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              text = if (isOverlayGranted) "Overlay permission active" else "Requires SYSTEM_ALERT_WINDOW",
              style = MaterialTheme.typography.bodySmall,
              color = if (isOverlayGranted) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Switch(
            checked = config.isOverlayEnabled && isOverlayGranted,
            onCheckedChange = { enable ->
              if (enable) {
                if (Settings.canDrawOverlays(context)) {
                  CapsuleManager.updateConfig { it.copy(isOverlayEnabled = true) }
                  CapsuleOverlayService.startService(context)
                } else {
                  showOverlayDisclosure = true
                }
              } else {
                CapsuleManager.updateConfig { it.copy(isOverlayEnabled = false) }
                CapsuleOverlayService.stopService(context)
              }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
          )
        }

        // Notification Listener toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Notification Alerts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              text = if (isNotifListenerGranted) "Listener active (100% offline)" else "Requires Notification Access",
              style = MaterialTheme.typography.bodySmall,
              color = if (isNotifListenerGranted) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = {
              if (!isNotifListenerGranted) {
                showNotifDisclosure = true
              } else {
                // Open listener settings so user can manage
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isNotifListenerGranted) PrimaryBlue.copy(alpha = 0.2f) else PrimaryBlue,
              contentColor = if (isNotifListenerGranted) PrimaryBlue else Color.Black
            )
          ) {
            Text(if (isNotifListenerGranted) "Configured" else "Enable", fontSize = 12.sp)
          }
        }

        // Idle Notch Pill toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Always Show Idle Pill", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              text = if (config.alwaysShowWhenIdle) "Notch pill visible constantly" else "Only appears during active events",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Switch(
            checked = config.alwaysShowWhenIdle,
            onCheckedChange = { checked ->
              CapsuleManager.updateConfig { it.copy(alwaysShowWhenIdle = checked) }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
          )
        }
      }
    }

    // -------------------------------------------------------------
    // LIVE EVENT SIMULATOR LAB
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Interactive Event Triggers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = { CapsuleManager.clearAllEvents() }) {
            Text("Clear", color = MaterialTheme.colorScheme.error)
          }
        }

        Text(
          text = "Test how OpenCapsule seamlessly arbitrates between media, incoming calls, timers, and alerts:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TriggerChip(label = "Music Track", icon = Icons.Default.MusicNote) {
            CapsuleManager.triggerDemo(DemoEventType.MEDIA)
          }
          TriggerChip(label = "Tea Timer", icon = Icons.Default.HourglassBottom) {
            CapsuleManager.triggerDemo(DemoEventType.TIMER)
          }
          TriggerChip(label = "Stopwatch", icon = Icons.Default.PlayArrow) {
            CapsuleManager.triggerDemo(DemoEventType.STOPWATCH)
          }
          TriggerChip(label = "Incoming Call", icon = Icons.Default.Call) {
            CapsuleManager.triggerDemo(DemoEventType.CALL_INCOMING)
          }
          TriggerChip(label = "Ongoing Call", icon = Icons.Default.Call) {
            CapsuleManager.triggerDemo(DemoEventType.CALL_ONGOING)
          }
          TriggerChip(label = "Charging 84%", icon = Icons.Default.BatteryChargingFull) {
            CapsuleManager.triggerDemo(DemoEventType.CHARGING)
          }
          TriggerChip(label = "Notification", icon = Icons.Default.Notifications) {
            CapsuleManager.triggerDemo(DemoEventType.NOTIFICATION)
          }
          TriggerChip(label = "Headphones", icon = Icons.Default.Bluetooth) {
            CapsuleManager.triggerDemo(DemoEventType.BLUETOOTH)
          }
          TriggerChip(label = "Download 68%", icon = Icons.Default.Download) {
            CapsuleManager.triggerDemo(DemoEventType.DOWNLOAD)
          }
          TriggerChip(label = "Screenshot", icon = Icons.Default.CameraAlt) {
            CapsuleManager.triggerDemo(DemoEventType.SCREENSHOT)
          }
          TriggerChip(label = "Ringer Vibrate", icon = Icons.Default.Vibration) {
            CapsuleManager.triggerDemo(DemoEventType.RINGER)
          }
        }
      }
    }
  }

  // -------------------------------------------------------------
  // REQUIRED EDUCATIONAL DISCLOSURE DIALOGS
  // -------------------------------------------------------------

  // Overlay Disclosure
  if (showOverlayDisclosure) {
    AlertDialog(
      onDismissRequest = { showOverlayDisclosure = false },
      title = { Text(context.getString(R.string.overlay_disclosure_title)) },
      text = { Text(context.getString(R.string.overlay_disclosure_body)) },
      confirmButton = {
        Button(
          onClick = {
            showOverlayDisclosure = false
            val intent = Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black)
        ) {
          Text(context.getString(R.string.btn_enable_overlay))
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showOverlayDisclosure = false }) {
          Text(context.getString(R.string.btn_not_now))
        }
      }
    )
  }

  // Notification Disclosure
  if (showNotifDisclosure) {
    AlertDialog(
      onDismissRequest = { showNotifDisclosure = false },
      title = { Text(context.getString(R.string.notification_disclosure_title)) },
      text = { Text(context.getString(R.string.notification_disclosure_body)) },
      confirmButton = {
        Button(
          onClick = {
            showNotifDisclosure = false
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black)
        ) {
          Text(context.getString(R.string.btn_allow_notification_access))
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showNotifDisclosure = false }) {
          Text(context.getString(R.string.btn_not_now))
        }
      }
    )
  }
}

@Composable
private fun TriggerChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  FilledTonalButton(
    onClick = onClick,
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
    Spacer(modifier = Modifier.width(6.dp))
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}
