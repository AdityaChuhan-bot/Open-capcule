package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun DataSafetyScreen(
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Play Protect Badge
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("data_safety_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(BatteryGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BatteryGreen)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text("Google Play Protect Compliance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("100% Offline • Zero Tracking • Zero Remote Telemetry", fontSize = 12.sp, color = BatteryGreen)
          }
        }

        Text(
          "OpenCapsule was built from day one adhering strictly to Google Play Developer Program policies and Android security principles. No hidden APIs, no reflection into private runtime classes, and no accessibility service abuse.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Google Play Data Safety Summary
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Play Store Data Safety Declaration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        SafetyRow("Data Collected", "None. Zero data is collected from your device.")
        SafetyRow("Data Shared", "None. No third-party servers, no analytics, no partners.")
        SafetyRow("Security Practices", "All operations run in local memory. No database logs.")
        SafetyRow("Data Deletion", "No account required. Revoking permissions or uninstalling removes everything immediately.")
      }
    }

    // In-App Privacy Policy
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
          Icon(Icons.Default.Policy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("OpenCapsule Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Text(
          text = """
1. Local-Only Processing
OpenCapsule operates entirely on your physical device. The application does not declare the android.permission.INTERNET permission in its manifest and is physically incapable of connecting to the internet or making external network requests.

2. Notification Data Handling
When the user explicitly grants Notification Access, incoming notification data is processed strictly in temporary volatile memory to render active alerts inside the capsule. Notification text is never persisted to disk, database, or external files.

3. Media Session Controls
OpenCapsule reads media metadata (song title, artist, playback status, album art) via the standard Android MediaSession API solely to display playback controls.

4. Zero Analytics and Zero Advertising
OpenCapsule contains no advertising SDKs, no behavioral tracking frameworks, no device fingerprinting tools, and no telemetry services.

5. User Control & Revocation
You can disable individual features, customize sensitive notification redaction, or stop the overlay service at any time from the app settings or Android System Settings.

6. 100% Free & Zero External APIs
OpenCapsule is entirely free, open-source, and offline. There are no paid tiers, no subscription locks, no in-app purchases, and no external API keys or cloud services required.
          """.trimIndent(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@Composable
private fun SafetyRow(title: String, description: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
