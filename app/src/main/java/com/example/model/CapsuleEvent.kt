package com.example.model

import android.graphics.Bitmap

/**
 * Sealed class hierarchy representing core contextual events presented
 * in the OpenCapsule Dynamic Island.
 *
 * Each event is processed purely locally on-device. Zero data is serialized to external services
 * or sent over the network.
 */
sealed interface CapsuleEvent {
  val id: String
  val priority: Int // Lower number = higher priority
  val isPersistent: Boolean // If true, remains active until explicitly stopped (e.g. playing music)

  data class Charging(
    override val id: String = "charging_event",
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isFastCharging: Boolean = false,
    val isLowBattery: Boolean = false,
    val isFullyCharged: Boolean = false,
    override val priority: Int = 20,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

  data class Notification(
    override val id: String,
    val title: String,
    val content: String,
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSensitive: Boolean = false,
    override val priority: Int = 30,
    override val isPersistent: Boolean = false
  ) : CapsuleEvent

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
}

