package com.example.model

import android.graphics.Bitmap

/**
 * Sealed class hierarchy representing all contextual events that can be presented
 * in the OpenCapsule dynamic island.
 *
 * Each event is processed purely locally on-device. Zero data is serialized to external services
 * or sent over the network.
 */
sealed interface CapsuleEvent {
  val id: String
  val priority: Int // Lower number = higher priority
  val isPersistent: Boolean // If true, remains active until explicitly stopped (e.g. ongoing call, timer, playing music)

  data class Media(
    override val id: String = "media_active",
    val title: String,
    val artist: String,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = true,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val appName: String = "Music",
    val packageName: String = "",
    override val priority: Int = 40,
    override val isPersistent: Boolean = true
  ) : CapsuleEvent

  data class Notification(
    override val id: String,
    val title: String,
    val content: String,
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSensitive: Boolean = false,
    override val priority: Int = 50,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  data class Call(
    override val id: String = "call_active",
    val callerName: String,
    val state: CallState,
    val durationSeconds: Long = 0L,
    override val priority: Int = if (state == CallState.INCOMING) 10 else 20,
    override val isPersistent: Boolean = state != CallState.DISCONNECTED
  ) : CapsuleEvent

  enum class CallState {
    INCOMING,
    ONGOING,
    DISCONNECTED
  }

  data class Timer(
    override val id: String = "timer_active",
    val label: String = "Timer",
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean = true,
    val isAlarmState: Boolean = false,
    override val priority: Int = if (isAlarmState) 15 else 30,
    override val isPersistent: Boolean = true
  ) : CapsuleEvent

  data class Stopwatch(
    override val id: String = "stopwatch_active",
    val elapsedSeconds: Long = 0L,
    val isRunning: Boolean = true,
    override val priority: Int = 35,
    override val isPersistent: Boolean = true
  ) : CapsuleEvent

  data class Charging(
    override val id: String = "charging_event",
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isFastCharging: Boolean = false,
    val isLowBattery: Boolean = false,
    val isFullyCharged: Boolean = false,
    override val priority: Int = 45,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  data class Bluetooth(
    override val id: String = "bluetooth_event",
    val deviceName: String,
    val isConnected: Boolean,
    val isHeadset: Boolean = true,
    override val priority: Int = 60,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  data class Download(
    override val id: String,
    val fileName: String,
    val progressPercent: Int,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false,
    override val priority: Int = 55,
    override val isPersistent: Boolean = !isComplete && !isFailed
  ) : CapsuleEvent

  data class Screenshot(
    override val id: String = "screenshot_event",
    val timestamp: Long = System.currentTimeMillis(),
    override val priority: Int = 65,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  data class Ringer(
    override val id: String = "ringer_event",
    val mode: RingerMode,
    override val priority: Int = 70,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  enum class RingerMode {
    SILENT,
    VIBRATE,
    NORMAL
  }
}
