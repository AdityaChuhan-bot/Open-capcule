package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.CapsuleManager
import com.example.model.CapsuleConfig
import com.example.service.CapsuleOverlayService
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.CapsuleDarkBg
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryIndigo

@Composable
fun OnboardingFlow(
  config: CapsuleConfig,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStep by remember { mutableIntStateOf(0) }
  val context = LocalContext.current

  // Step 3 feature states
  var notifSelected by remember { mutableStateOf(config.featureNotifications) }
  var mediaSelected by remember { mutableStateOf(config.featureMedia) }
  var callSelected by remember { mutableStateOf(config.featureCalls) }
  var chargeSelected by remember { mutableStateOf(config.featureCharging) }
  var timerSelected by remember { mutableStateOf(config.featureTimers) }
  var btSelected by remember { mutableStateOf(config.featureBluetooth) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Step indicator dots
      Row(
        modifier = Modifier
          .padding(top = 16.dp)
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(4) { index ->
          val isActive = index == currentStep
          Box(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .height(6.dp)
              .width(if (isActive) 24.dp else 6.dp)
              .clip(CircleShape)
              .background(if (isActive) PrimaryBlue else Color.White.copy(alpha = 0.2f))
          )
        }
      }

      // Step content with animated transition
      Box(modifier = Modifier.weight(1f)) {
        AnimatedContent(
          targetState = currentStep,
          transitionSpec = {
            if (targetState > initialState) {
              (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            } else {
              (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }
          },
          label = "onboarding_step"
        ) { step ->
          when (step) {
            0 -> OnboardingStepIntro()
            1 -> OnboardingStepPrivacy()
            2 -> OnboardingStepFeatures(
              notif = notifSelected, onNotif = { notifSelected = it },
              media = mediaSelected, onMedia = { mediaSelected = it },
              call = callSelected, onCall = { callSelected = it },
              charge = chargeSelected, onCharge = { chargeSelected = it },
              timer = timerSelected, onTimer = { timerSelected = it },
              bt = btSelected, onBt = { btSelected = it }
            )
            3 -> OnboardingStepPermissions(context = context)
            else -> OnboardingStepIntro()
          }
        }
      }

      // Bottom Navigation Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentStep > 0) {
          OutlinedButton(
            onClick = { currentStep-- },
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Back")
          }
        } else {
          Spacer(modifier = Modifier.width(1.dp))
        }

        Button(
          onClick = {
            if (currentStep < 3) {
              if (currentStep == 2) {
                // Save chosen features
                CapsuleManager.updateConfig {
                  it.copy(
                    featureNotifications = notifSelected,
                    featureMedia = mediaSelected,
                    featureCalls = callSelected,
                    featureCharging = chargeSelected,
                    featureTimers = timerSelected,
                    featureBluetooth = btSelected
                  )
                }
              }
              currentStep++
            } else {
              // Final Step Complete
              val canOverlay = Settings.canDrawOverlays(context)
              CapsuleManager.updateConfig {
                it.copy(
                  isOnboardingCompleted = true,
                  isOverlayEnabled = canOverlay
                )
              }
              if (canOverlay) {
                CapsuleOverlayService.startService(context)
              }
              onComplete()
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black),
          modifier = Modifier.testTag("btn_onboarding_next")
        ) {
          Text(if (currentStep == 3) "Get Started" else "Continue", fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 1: INTRO
// -------------------------------------------------------------
@Composable
private fun OnboardingStepIntro() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Elegant capsule illustration
    Box(
      modifier = Modifier
        .width(180.dp)
        .height(52.dp)
        .clip(RoundedCornerShape(26.dp))
        .background(Color.Black)
        .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(PrimaryBlue)
        )
        Text("OpenCapsule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(SecondaryIndigo)
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "OpenCapsule",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Your private Dynamic Island-style control center.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Seamlessly integrates around your camera cutout with tactile spring physics, live media controls, countdown timers, and subtle notifications.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      lineHeight = 22.sp
    )
  }
}

// -------------------------------------------------------------
// STEP 2: PRIVACY BY DEFAULT
// -------------------------------------------------------------
@Composable
private fun OnboardingStepPrivacy() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(BatteryGreen.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Default.Security, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(36.dp))
    }

    Text(
      text = "Privacy by Default",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )

    Text(
      text = "OpenCapsule is built offline-only. Your data never leaves your device.",
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        PrivacyPillar(
          title = "Zero Internet Access",
          description = "No INTERNET permission is declared in our manifest. The app cannot make network calls."
        )
        PrivacyPillar(
          title = "Zero Tracking & Analytics",
          description = "No third-party SDKs, telemetry frameworks, or behavioral profilers."
        )
        PrivacyPillar(
          title = "No Notification History",
          description = "Notification alerts are processed in volatile memory only and never saved to storage."
        )
        PrivacyPillar(
          title = "No Accessibility Abuse",
          description = "OpenCapsule operates through standard, legitimate WindowManager overlays."
        )
      }
    }
  }
}

@Composable
private fun PrivacyPillar(title: String, description: String) {
  Row(verticalAlignment = Alignment.Top) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
      Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

// -------------------------------------------------------------
// STEP 3: FEATURE SELECTION
// -------------------------------------------------------------
@Composable
private fun OnboardingStepFeatures(
  notif: Boolean, onNotif: (Boolean) -> Unit,
  media: Boolean, onMedia: (Boolean) -> Unit,
  call: Boolean, onCall: (Boolean) -> Unit,
  charge: Boolean, onCharge: (Boolean) -> Unit,
  timer: Boolean, onTimer: (Boolean) -> Unit,
  bt: Boolean, onBt: (Boolean) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Select Capsule Features",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )

    Text(
      text = "Choose which contextual modules you would like active in OpenCapsule:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    FeatureCheckboxRow("Notifications", "Selected incoming app alerts", Icons.Default.Notifications, notif, onNotif)
    FeatureCheckboxRow("Media Controls", "Active music artwork and player", Icons.Default.MusicNote, media, onMedia)
    FeatureCheckboxRow("Phone Calls", "Ongoing call duration banner", Icons.Default.Call, call, onCall)
    FeatureCheckboxRow("Battery & Charging", "Plugged in, fast charging and full alerts", Icons.Default.BatteryChargingFull, charge, onCharge)
    FeatureCheckboxRow("Timers & Stopwatch", "Countdowns and stopwatch presentation", Icons.Default.HourglassBottom, timer, onTimer)
    FeatureCheckboxRow("Bluetooth Accessories", "Headset connection status", Icons.Default.Bluetooth, bt, onBt)
  }
}

@Composable
private fun FeatureCheckboxRow(
  title: String,
  desc: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  checked: Boolean,
  onChecked: (Boolean) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Checkbox(
        checked = checked,
        onCheckedChange = onChecked,
        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
      )
    }
  }
}

// -------------------------------------------------------------
// STEP 4: PERMISSION SETUP
// -------------------------------------------------------------
@Composable
private fun OnboardingStepPermissions(context: Context) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
  var notifGranted by remember {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    mutableStateOf(flat != null && flat.contains(context.packageName))
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        canOverlay = Settings.canDrawOverlays(context)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        notifGranted = flat != null && flat.contains(context.packageName)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Permission Setup",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )

    Text(
      text = "OpenCapsule requires explicit permission to render above other apps and monitor media playback:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Overlay Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
          Text("1. Display Over Other Apps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          if (canOverlay) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(18.dp))
          }
        }
        Text(
          "Used only to draw the capsule UI around your front camera notch. Never used to record your screen.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!canOverlay) {
          Button(
            onClick = {
              val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
              )
              context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black),
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Enable Overlay", fontSize = 12.sp)
          }
        }
      }
    }

    // Notification Listener Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
          Text("2. Notification & Media Access", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          if (notifGranted) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(18.dp))
          }
        }
        Text(
          "Used exclusively for displaying selected notification alerts and music controls. 100% offline, zero data storage.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!notifGranted) {
          Button(
            onClick = {
              val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
              context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black),
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Enable Access", fontSize = 12.sp)
          }
        }
      }
    }
  }
}
