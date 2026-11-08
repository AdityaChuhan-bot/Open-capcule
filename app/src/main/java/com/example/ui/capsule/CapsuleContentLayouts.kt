package com.example.ui.capsule

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.core.CapsuleManager
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryIndigo

/**
 * Helper to launch Echo Music specifically by detecting its installed package or intent.
 * If Echo Music is unavailable, presents a clear user message rather than opening generic players.
 */
fun launchEchoMusic(context: Context) {
  val pm = context.packageManager
  val echoMusicIntent = try {
    pm.getInstalledApplications(0)
      .asSequence()
      .mapNotNull { appInfo ->
        val label = try { pm.getApplicationLabel(appInfo).toString() } catch (_: Exception) { "" }
        val normalizedLabel = label.lowercase()
        val pkg = appInfo.packageName.lowercase()
        if ((normalizedLabel.contains("echo") && normalizedLabel.contains("music")) ||
          pkg.contains("echomusic") ||
          (pkg.contains("echo") && pkg.contains("music"))
        ) {
          pm.getLaunchIntentForPackage(appInfo.packageName)
        } else null
      }
      .firstOrNull()
  } catch (_: Exception) {
    null
  }

  if (echoMusicIntent != null) {
    echoMusicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      context.startActivity(echoMusicIntent)
    } catch (_: Exception) {
      Toast.makeText(context, "Failed to launch Echo Music", Toast.LENGTH_SHORT).show()
    }
  } else {
    Toast.makeText(context, "Echo Music is not installed on this device", Toast.LENGTH_LONG).show()
  }
}

// ---------------------------------------------------------
// Collapsed Capsule Layout
// ---------------------------------------------------------

@Composable
fun CollapsedCapsuleContent(
  event: CapsuleEvent?,
  config: CapsuleConfig
) {
  if (event == null) {
    // Idle compact state: clean minimalist black pill without any text
    Spacer(modifier = Modifier.fillMaxSize())
    return
  }

  when (event) {
    is CapsuleEvent.Media -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f, fill = false)
            .widthIn(max = 110.dp)
        ) {
          if (event.albumArt != null) {
            Image(
              bitmap = event.albumArt.asImageBitmap(),
              contentDescription = "Artwork",
              modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
            )
          } else {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = "Music",
              tint = PrimaryBlue,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = event.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        AudioWaveVisualizer(isPlaying = event.isPlaying)
      }
    }

    is CapsuleEvent.Charging -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.BatteryChargingFull,
          contentDescription = "Charging",
          tint = if (event.isLowBattery) AlertAmber else BatteryGreen,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "${event.batteryPercent}%",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    is CapsuleEvent.Notification -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Notifications,
          contentDescription = "Notification",
          tint = SecondaryIndigo,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = event.title,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

// ---------------------------------------------------------
// Idle Quick Control Center (When expanded without active event)
// ---------------------------------------------------------

@Composable
fun IdleQuickControlCenter(
  config: CapsuleConfig,
  onCollapse: () -> Unit
) {
  val context = LocalContext.current
  val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager }
  val batteryLevel = remember {
    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 85
  }
  val isCharging = remember {
    try {
      val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (_: Exception) {
      false
    }
  }
  val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
  val ringerMode = remember { audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Row: Brand pill + Grabber + Collapse button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .background(Color(0xFF10B981), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Control Center",
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp
        )
      }

      // Drag Grabber Indicator
      Box(
        modifier = Modifier
          .width(36.dp)
          .height(4.dp)
          .background(Color.White.copy(alpha = 0.25f), CircleShape)
      )

      // Collapse chevron button
      Box(
        modifier = Modifier
          .size(24.dp)
          .background(Color.White.copy(alpha = 0.12f), CircleShape)
          .clip(CircleShape)
          .clickable { onCollapse() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowUp,
          contentDescription = "Collapse",
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    // Middle Row: Battery Card & Sound Mode Card
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Battery Card
      Row(
        modifier = Modifier
          .weight(1f)
          .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
          contentDescription = "Battery",
          tint = if (isCharging) Color(0xFF10B981) else if (batteryLevel <= 20) Color(0xFFEF4444) else PrimaryBlue,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "$batteryLevel%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = if (isCharging) "Charging" else "Battery",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 10.sp
          )
        }
      }

      // Sound / Mode Card
      Row(
        modifier = Modifier
          .weight(1f)
          .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val soundIcon = when (ringerMode) {
          AudioManager.RINGER_MODE_SILENT -> Icons.Default.VolumeOff
          AudioManager.RINGER_MODE_VIBRATE -> Icons.Default.Vibration
          else -> Icons.Default.VolumeUp
        }
        val soundLabel = when (ringerMode) {
          AudioManager.RINGER_MODE_SILENT -> "Silent"
          AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
          else -> "Sound"
        }
        Icon(
          imageVector = soundIcon,
          contentDescription = "Ringer Mode",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = soundLabel,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = "Audio Mode",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 10.sp
          )
        }
      }
    }

    // Bottom Row: Quick Tools & App Shortcuts
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Echo Music Launcher
      Row(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            launchEchoMusic(context)
            onCollapse()
          }
          .padding(horizontal = 8.dp)
          .testTag("btn_launch_echo_music_idle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.MusicNote,
          contentDescription = null,
          tint = PrimaryBlue,
          modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "Echo Music",
          color = Color.White,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp
        )
      }

      // Settings Launcher
      Row(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            val intent = Intent(context, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
            onCollapse()
          }
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "Settings",
          color = Color.White,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp
        )
      }
    }
  }
}

// ---------------------------------------------------------
// Expanded Capsule Layout
// ---------------------------------------------------------

@Composable
fun ExpandedCapsuleContent(
  event: CapsuleEvent?,
  config: CapsuleConfig,
  onCollapse: () -> Unit
) {
  val context = LocalContext.current

  if (event == null) {
    IdleQuickControlCenter(config = config, onCollapse = onCollapse)
    return
  }

  when (event) {
    is CapsuleEvent.Media -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (event.albumArt != null) {
            Image(
              bitmap = event.albumArt.asImageBitmap(),
              contentDescription = "Album Art",
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { launchEchoMusic(context) }
            )
          } else {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryBlue.copy(alpha = 0.2f))
                .clickable { launchEchoMusic(context) },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(
            modifier = Modifier
              .weight(1f)
              .clickable { launchEchoMusic(context) }
          ) {
            Text(
              text = event.title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = event.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open in Echo Music",
                tint = PrimaryBlue.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
              )
            }
          }

          AudioWaveVisualizer(isPlaying = event.isPlaying, barHeight = 22.dp)
        }

        // Progress indicator
        val progress = if (event.durationMs > 0) {
          (event.progressMs.toFloat() / event.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0.45f

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape),
          color = PrimaryBlue,
          trackColor = Color.White.copy(alpha = 0.15f)
        )

        // Transport Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { CapsuleManager.onMediaPrevious() },
            modifier = Modifier.testTag("btn_media_prev")
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
          }

          IconButton(
            onClick = { CapsuleManager.onMediaPlayPause() },
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(PrimaryBlue)
              .testTag("btn_media_play_pause")
          ) {
            Icon(
              imageVector = if (event.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (event.isPlaying) "Pause" else "Play",
              tint = Color.Black
            )
          }

          IconButton(
            onClick = { CapsuleManager.onMediaNext() },
            modifier = Modifier.testTag("btn_media_next")
          ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
          }
        }
      }
    }

    is CapsuleEvent.Charging -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (event.isFastCharging) "Fast Charging" else "Charging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
          Text("${event.batteryPercent}%", color = BatteryGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        LinearProgressIndicator(
          progress = { (event.batteryPercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
          color = BatteryGreen,
          trackColor = Color.White.copy(alpha = 0.15f)
        )

        Text(
          text = if (event.isFullyCharged) "Battery full • Ready to unplug" else "Charging active",
          color = Color.White.copy(alpha = 0.7f),
          fontSize = 12.sp
        )
      }
    }

    is CapsuleEvent.Notification -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SecondaryIndigo),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(event.appName, color = SecondaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          Text("Just now", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }

        Column {
          Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (event.isSensitive) "Content hidden (Privacy setting active)" else event.content,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          FilledTonalButton(
            onClick = onCollapse,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color.White.copy(alpha = 0.15f),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("btn_notif_dismiss")
          ) {
            Text("Dismiss", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------
// Animated Sound Wave Visualizer for Media Playback
// ---------------------------------------------------------

@Composable
fun AudioWaveVisualizer(
  isPlaying: Boolean,
  barHeight: Dp = 14.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")

  val scale1 by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = if (isPlaying) 1.0f else 0.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar1"
  )

  val scale2 by infiniteTransition.animateFloat(
    initialValue = 0.7f,
    targetValue = if (isPlaying) 0.2f else 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(550, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar2"
  )

  val scale3 by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = if (isPlaying) 0.9f else 0.4f,
    animationSpec = infiniteRepeatable(
      animation = tween(350, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar3"
  )

  Row(
    modifier = Modifier.height(barHeight),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(barHeight * scale1)
        .clip(CircleShape)
        .background(PrimaryBlue)
    )
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(barHeight * scale2)
        .clip(CircleShape)
        .background(SecondaryIndigo)
    )
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(barHeight * scale3)
        .clip(CircleShape)
        .background(PrimaryBlue)
    )
  }
}
