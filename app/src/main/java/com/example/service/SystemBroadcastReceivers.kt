package com.example.service

import android.app.DownloadManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.example.core.CapsuleManager
import com.example.model.CapsuleEvent

/**
 * System event bridge owned by the foreground overlay service.
 *
 * The receiver is only registered while the user-enabled overlay service is alive.
 * It is exported because Android's highly privileged Bluetooth system process can be
 * a distinct UID; Android documents RECEIVER_EXPORTED for that class of system broadcasts.
 */
class SystemEventsRegistrar(private val context: Context) {

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      if (intent == null) return
      when (intent.action) {
        Intent.ACTION_BATTERY_CHANGED,
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED -> handleBattery(intent)
        Intent.ACTION_HEADSET_PLUG -> handleHeadset(intent)
        BluetoothDevice.ACTION_ACL_CONNECTED -> handleBluetooth(intent, true)
        BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleBluetooth(intent, false)
        DownloadManager.ACTION_DOWNLOAD_COMPLETE -> handleDownload(intent)
        AudioManager.RINGER_MODE_CHANGED_ACTION -> handleRinger(intent)
      }
    }
  }

  fun register() {
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_POWER_CONNECTED)
      addAction(Intent.ACTION_POWER_DISCONNECTED)
      addAction(Intent.ACTION_BATTERY_CHANGED)
      addAction(Intent.ACTION_HEADSET_PLUG)
      addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
      addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
      addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
      addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
    }

    ContextCompat.registerReceiver(
      context,
      receiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED
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

  private fun handleHeadset(intent: Intent) {
    if (intent.getIntExtra("state", -1) != 1) return
    val name = intent.getStringExtra("name") ?: "Wired Headset"
    CapsuleManager.postEvent(
      CapsuleEvent.Bluetooth(
        deviceName = name,
        isConnected = true,
        isHeadset = true
      )
    )
  }

  private fun handleBluetooth(intent: Intent, isConnected: Boolean) {
    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

    val deviceName = try {
      device?.name ?: "Bluetooth Audio"
    } catch (_: SecurityException) {
      "Bluetooth Audio"
    }

    CapsuleManager.postEvent(
      CapsuleEvent.Bluetooth(
        deviceName = deviceName,
        isConnected = isConnected,
        isHeadset = true
      )
    )
  }

  private fun handleDownload(intent: Intent) {
    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
    if (id != -1L) {
      CapsuleManager.postEvent(
        CapsuleEvent.Download(
          id = "dl_$id",
          fileName = "File download",
          progressPercent = 100,
          isComplete = true
        )
      )
    }
  }

  private fun handleRinger(intent: Intent) {
    val mode = intent.getIntExtra(
      AudioManager.EXTRA_RINGER_MODE,
      AudioManager.RINGER_MODE_NORMAL
    )
    val ringerMode = when (mode) {
      AudioManager.RINGER_MODE_SILENT -> CapsuleEvent.RingerMode.SILENT
      AudioManager.RINGER_MODE_VIBRATE -> CapsuleEvent.RingerMode.VIBRATE
      else -> CapsuleEvent.RingerMode.NORMAL
    }
    CapsuleManager.postEvent(CapsuleEvent.Ringer(mode = ringerMode))
  }
}
