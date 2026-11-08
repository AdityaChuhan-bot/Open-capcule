package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.example.core.CapsuleManager
import com.example.model.CapsuleEvent

/**
 * System event bridge owned by the foreground overlay service.
 *
 * Dedicated strictly to detecting battery and charging states through supported Android APIs.
 * The receiver is only registered while the user-enabled overlay service is alive.
 */
class SystemEventsRegistrar(private val context: Context) {

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      if (intent == null) return
      when (intent.action) {
        Intent.ACTION_BATTERY_CHANGED,
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED -> handleBattery(intent)
      }
    }
  }

  fun register() {
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_POWER_CONNECTED)
      addAction(Intent.ACTION_POWER_DISCONNECTED)
      addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    ContextCompat.registerReceiver(
      context,
      receiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED
    )
  }

  fun unregister() {
    try {
      context.unregisterReceiver(receiver)
    } catch (_: Exception) {
    }
  }

  private fun handleBattery(intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

    val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 50
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL ||
      plugged > 0
    val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC
    val isLow = percent <= 20 && !isCharging
    val isFull = percent >= 100

    CapsuleManager.postEvent(
      CapsuleEvent.Charging(
        batteryPercent = percent,
        isCharging = isCharging,
        isFastCharging = isFast,
        isLowBattery = isLow,
        isFullyCharged = isFull
      )
    )
  }
}
