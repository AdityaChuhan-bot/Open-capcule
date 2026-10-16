package com.example.ui.capsule

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.MainActivity
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.sp
import com.example.core.CapsuleManager
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryIndigo

// ---------------------------------------------------------
// Collapsed Capsule Layout
// ---------------------------------------------------------

@Composable
fun CollapsedCapsuleContent(
  event: CapsuleEvent?,
  config: CapsuleConfig
) {
  if (event == null) {
    // Idle state
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(PrimaryBlue)
      )
      Text(
        text = "OpenCapsule",
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(SecondaryIndigo)
      )
    }
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
            .widthIn(max = 92.dp)
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

    is CapsuleEvent.Timer -> {
      val min = event.remainingSeconds / 60
      val sec = event.remainingSeconds % 60
      val timeText = String.format("%02d:%02d", min, sec)
      val iconTint = if (event.isAlarmState) AlertAmber else PrimaryBlue

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.HourglassBottom,
          contentDescription = "Timer",
          tint = iconTint,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = timeText,
          color = if (event.isAlarmState) AlertAmber else Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    is CapsuleEvent.Stopwatch -> {
      val min = event.elapsedSeconds / 60
      val sec = event.elapsedSeconds % 60
      val timeText = String.format("%02d:%02d", min, sec)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Timer,
          contentDescription = "Stopwatch",
          tint = PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = timeText,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    is CapsuleEvent.Call -> {
      val isIncoming = event.state == CapsuleEvent.CallState.INCOMING
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Call,
          contentDescription = "Call",
          tint = if (isIncoming) BatteryGreen else PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (isIncoming) "Incoming: ${event.callerName}" else event.callerName,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
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

    is CapsuleEvent.Bluetooth -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (event.isHeadset) Icons.Default.Headphones else Icons.Default.Bluetooth,
          contentDescription = "Bluetooth",
          tint = PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (event.isConnected) "Connected" else "Disconnected",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    is CapsuleEvent.Download -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = "Download",
          tint = PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "${event.progressPercent}%",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    is CapsuleEvent.Screenshot -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = "Screenshot",
          tint = PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Screenshot saved",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    is CapsuleEvent.Ringer -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = when (event.mode) {
            CapsuleEvent.RingerMode.SILENT -> Icons.Default.VolumeOff
            CapsuleEvent.RingerMode.VIBRATE -> Icons.Default.Vibration
            CapsuleEvent.RingerMode.NORMAL -> Icons.Default.Check
          },
          contentDescription = "Ringer",
          tint = PrimaryBlue,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = event.mode.name.lowercase().replaceFirstChar { it.uppercase() },
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

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
          text = "OpenCapsule",
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
      // Music Player launcher
      Row(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            // Prefer the user's requested music app instead of Android's generic
            // CATEGORY_APP_MUSIC resolver (which may open another player).
            val echoMusicIntent = context.packageManager.getInstalledApplications(0)
              .asSequence()
              .mapNotNull { appInfo ->
                val label = context.packageManager.getApplicationLabel(appInfo).toString()
                val normalized = label.lowercase()
                if (normalized.contains("echo") && normalized.contains("music")) {
                  context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                } else null
              }
              .firstOrNull()

            val intent = echoMusicIntent ?: Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_APP_MUSIC)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
              context.startActivity(intent)
            } catch (_: Exception) {
              try {
                val fallback = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
              } catch (_: Exception) {}
            }
            onCollapse()
          }
          .padding(horizontal = 8.dp),
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
          text = "Music",
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
            )
          } else {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryBlue.copy(alpha = 0.2f)),
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

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = event.title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = event.artist,
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
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

    is CapsuleEvent.Timer -> {
      val min = event.remainingSeconds / 60
      val sec = event.remainingSeconds % 60
      val timeText = String.format("%02d:%02d", min, sec)

      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(event.label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
          Text(
            text = if (event.isAlarmState) "TIME UP!" else timeText,
            color = if (event.isAlarmState) AlertAmber else PrimaryBlue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
        }

        val progress = if (event.totalSeconds > 0) {
          (event.remainingSeconds.toFloat() / event.totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
          color = if (event.isAlarmState) AlertAmber else PrimaryBlue,
          trackColor = Color.White.copy(alpha = 0.15f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          FilledTonalButton(
            onClick = { CapsuleManager.onTimerReset() },
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color.White.copy(alpha = 0.15f),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("btn_timer_reset")
          ) {
            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset")
          }

          FilledTonalButton(
            onClick = { CapsuleManager.onTimerAddMinute() },
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color.White.copy(alpha = 0.15f),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("btn_timer_add_minute")
          ) {
            Text("+1 Min")
          }

          FilledTonalButton(
            onClick = { CapsuleManager.onTimerPauseResume() },
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = PrimaryBlue,
              contentColor = Color.Black
            ),
            modifier = Modifier.testTag("btn_timer_pause_resume")
          ) {
            Icon(
              if (event.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (event.isRunning) "Pause" else "Resume")
          }
        }
      }
    }

    is CapsuleEvent.Call -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(PrimaryBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(event.callerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val stateText = when (event.state) {
              CapsuleEvent.CallState.INCOMING -> "Incoming cellular call"
              CapsuleEvent.CallState.ONGOING -> "Ongoing call • ${event.durationSeconds}s"
              CapsuleEvent.CallState.DISCONNECTED -> "Call ended"
            }
            Text(stateText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          if (event.state == CapsuleEvent.CallState.INCOMING) {
            FilledTonalButton(
              onClick = { CapsuleManager.onCallAnswer() },
              colors = ButtonDefaults.filledTonalButtonColors(containerColor = BatteryGreen, contentColor = Color.White),
              modifier = Modifier.testTag("btn_call_answer")
            ) {
              Icon(Icons.Default.Call, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Answer")
            }
          }

          FilledTonalButton(
            onClick = { CapsuleManager.onCallEnd() },
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CallRed, contentColor = Color.White),
            modifier = Modifier.testTag("btn_call_end")
          ) {
            Icon(Icons.Default.CallEnd, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Decline")
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
          text = if (event.isFullyCharged) "Battery full • Ready to unplug" else "Estimated ~35 min until fully charged",
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

    is CapsuleEvent.Download -> {
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
            Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(event.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
          Text("${event.progressPercent}%", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        LinearProgressIndicator(
          progress = { (event.progressPercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
          color = PrimaryBlue,
          trackColor = Color.White.copy(alpha = 0.15f)
        )

        Text(
          text = if (event.isComplete) "Download complete" else "Downloading via DownloadManager...",
          color = Color.White.copy(alpha = 0.7f),
          fontSize = 12.sp
        )
      }
    }

    is CapsuleEvent.Bluetooth -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (event.isHeadset) Icons.Default.Headphones else Icons.Default.Bluetooth,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(event.deviceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              if (event.isConnected) "Audio accessory connected" else "Device disconnected",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp
            )
          }
        }

        Text(
          "Local audio routing active • Zero Bluetooth tracking",
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 11.sp
        )
      }
    }

    is CapsuleEvent.Stopwatch -> {
      val min = event.elapsedSeconds / 60
      val sec = event.elapsedSeconds % 60
      val timeText = String.format("%02d:%02d", min, sec)

      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("Stopwatch", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        Text(timeText, color = PrimaryBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          FilledTonalButton(
            onClick = onCollapse,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color.White.copy(alpha = 0.15f),
              contentColor = Color.White
            )
          ) {
            Text("Close")
          }
        }
      }
    }

    is CapsuleEvent.Screenshot -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Screenshot Captured", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Saved to local pictures", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          }
        }
        Text("Tap to dismiss", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
      }
    }

    is CapsuleEvent.Ringer -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when (event.mode) {
              CapsuleEvent.RingerMode.SILENT -> Icons.Default.VolumeOff
              CapsuleEvent.RingerMode.VIBRATE -> Icons.Default.Vibration
              CapsuleEvent.RingerMode.NORMAL -> Icons.Default.Check
            },
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Ringer: ${event.mode.name.lowercase().replaceFirstChar { it.uppercase() }}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
        Text("System audio profile updated", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
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
  barHeight: androidx.compose.ui.unit.Dp = 14.dp
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
