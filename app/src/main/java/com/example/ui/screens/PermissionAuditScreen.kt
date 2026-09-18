package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.PrimaryBlue

data class AuditPermissionItem(
  val name: String,
  val technicalId: String,
  val purpose: String,
  val feature: String,
  val isOptional: Boolean,
  val checkGranted: (Context) -> Boolean,
  val openSettingsAction: (Context) -> Unit
)

@Composable
fun PermissionAuditScreen(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var resumeTrigger by remember { mutableIntStateOf(0) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        resumeTrigger++
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val permissions = remember {
    listOf(
      AuditPermissionItem(
        name = "Display Over Other Apps",
        technicalId = "android.permission.SYSTEM_ALERT_WINDOW",
        purpose = "Allows OpenCapsule to draw the floating pill window over active apps around your camera notch.",
        feature = "Capsule Overlay Window",
        isOptional = false,
        checkGranted = { Settings.canDrawOverlays(it) },
        openSettingsAction = { ctx ->
          ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }
      ),
      AuditPermissionItem(
        name = "Notification Listener Access",
        technicalId = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        purpose = "Receives incoming notifications and media playback locally to show them in the capsule. Zero data is stored or transmitted.",
        feature = "Notification Alerts & Media Controls",
        isOptional = true,
        checkGranted = { ctx ->
          val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
          flat != null && flat.contains(ctx.packageName)
        },
        openSettingsAction = { ctx ->
          ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
      ),
      AuditPermissionItem(
        name = "Post Notifications",
        technicalId = "android.permission.POST_NOTIFICATIONS",
        purpose = "Maintains a transparent foreground status notification with a one-tap stop button while overlay is running.",
        feature = "Service Transparency & Control",
        isOptional = false,
        checkGranted = { ctx ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
          } else true
        },
        openSettingsAction = { ctx ->
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
          }
          ctx.startActivity(intent)
        }
      ),
      AuditPermissionItem(
        name = "Haptic Vibration",
        technicalId = "android.permission.VIBRATE",
        purpose = "Provides subtle tactile feedback when expanding or tapping controls.",
        feature = "Interactive Spring Physics",
        isOptional = false,
        checkGranted = { true },
        openSettingsAction = {}
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(PrimaryBlue.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text("Permission Transparency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            "OpenCapsule uses the strict minimum set of legitimate Android APIs required to draw the capsule and monitor active playback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Permission List
    Text("Declared Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

    permissions.forEach { item ->
      val isGranted = remember(resumeTrigger, item) { item.checkGranted(context) }

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text(item.technicalId, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (isGranted) BatteryGreen.copy(alpha = 0.15f) else AlertAmber.copy(alpha = 0.15f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (isGranted) "Granted" else if (item.isOptional) "Optional" else "Required",
                color = if (isGranted) BatteryGreen else AlertAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(item.purpose, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Feature: ${item.feature}",
              fontSize = 11.sp,
              color = PrimaryBlue,
              fontWeight = FontWeight.Medium
            )

            if (!isGranted && item.technicalId != "android.permission.VIBRATE") {
              Button(
                onClick = { item.openSettingsAction(context) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black),
                modifier = Modifier.testTag("btn_audit_grant_${item.name.replace(" ", "_")}")
              ) {
                Text("Configure", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    // Explicitly Excluded / Forbidden Permissions
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Explicitly Forbidden & Never Requested", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text(
          "OpenCapsule enforces a strict zero-telemetry architecture. The following sensitive capabilities are intentionally omitted from our manifest:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val excluded = listOf(
          "android.permission.INTERNET (App is 100% Offline)",
          "android.accessibilityservice.AccessibilityService (Never abused)",
          "android.permission.ACCESS_FINE_LOCATION (Never tracked)",
          "android.permission.READ_CONTACTS (Never accessed)",
          "android.permission.RECORD_AUDIO (Microphone never used)",
          "android.permission.CAMERA (Camera hardware never accessed)",
          "android.permission.READ_EXTERNAL_STORAGE (Storage never accessed)"
        )

        excluded.forEach { perm ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Block, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(perm, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}
