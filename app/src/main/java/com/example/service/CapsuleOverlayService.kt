package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.core.CapsuleManager
import com.example.model.CapsulePosition
import com.example.ui.capsule.OpenCapsuleDynamicView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Legitimate foreground service rendering the OpenCapsule Dynamic Island window above user apps.
 *
 * Security & Policy:
 * - Minimum required permission: SYSTEM_ALERT_WINDOW.
 * - Non-deceptive: Transparent, minimal, non-blocking window positioned around the camera notch.
 * - Foreground notification visible at all times with one-tap Stop action.
 * - Immediately destroyed if overlay permission is revoked.
 */
class CapsuleOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private val appViewModelStore = ViewModelStore()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var windowManager: WindowManager? = null
  private var composeView: ComposeView? = null
  private var windowParams: WindowManager.LayoutParams? = null
  private var systemEventsRegistrar: SystemEventsRegistrar? = null

  override val lifecycle: Lifecycle get() = lifecycleRegistry
  override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
  override val viewModelStore: ViewModelStore get() = appViewModelStore

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.currentState = Lifecycle.State.CREATED

    createNotificationChannel()
    startForegroundServiceWithNotification()

    systemEventsRegistrar = SystemEventsRegistrar(this).apply { register() }

    if (Settings.canDrawOverlays(this)) {
      initOverlayView()
    } else {
      stopSelf()
    }

    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.overlay_service_label),
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = getString(R.string.overlay_notification_text)
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun startForegroundServiceWithNotification() {
    val stopIntent = Intent(this, CapsuleOverlayService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this, 0, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val openAppIntent = Intent(this, MainActivity::class.java)
    val openAppPendingIntent = PendingIntent.getActivity(
      this, 0, openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.overlay_notification_title))
      .setContentText(getString(R.string.overlay_notification_text))
      .setSmallIcon(R.drawable.ic_stat_capsule)
      .setContentIntent(openAppPendingIntent)
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        getString(R.string.overlay_notification_stop),
        stopPendingIntent
      )
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
      Log.d(TAG, "Foreground service started with notification")
    } catch (e: Exception) {
      Log.e(TAG, "Error starting foreground service notification", e)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }
    return START_STICKY
  }

  private fun initOverlayView() {
    windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    val density = resources.displayMetrics.density

    val initialConfig = CapsuleManager.config.value
    val gravity = when (initialConfig.position) {
      CapsulePosition.LEFT -> Gravity.TOP or Gravity.START
      CapsulePosition.RIGHT -> Gravity.TOP or Gravity.END
      CapsulePosition.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      this.gravity = gravity
      this.x = (initialConfig.offsetX * density).toInt()
      this.y = (initialConfig.offsetY * density).toInt()
    }
    windowParams = params

    try {
      val view = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@CapsuleOverlayService)
        setViewTreeSavedStateRegistryOwner(this@CapsuleOverlayService)
        setViewTreeViewModelStoreOwner(this@CapsuleOverlayService)
        setContent {
          MyApplicationTheme {
            val activeEvent by CapsuleManager.activeEvent.collectAsState()
            val isExpanded by CapsuleManager.isExpanded.collectAsState()
            val config by CapsuleManager.config.collectAsState()

            if (activeEvent != null || config.alwaysShowWhenIdle) {
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
      composeView = view

      windowManager?.addView(view, params)
      Log.d(TAG, "Overlay ComposeView successfully attached to WindowManager")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize overlay ComposeView", e)
    }

    // Listen to config changes and update window position
    serviceScope.launch {
      CapsuleManager.config.collect { cfg ->
        val currentParams = windowParams ?: return@collect
        val g = when (cfg.position) {
          CapsulePosition.LEFT -> Gravity.TOP or Gravity.START
          CapsulePosition.RIGHT -> Gravity.TOP or Gravity.END
          CapsulePosition.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        currentParams.gravity = g
        currentParams.x = (cfg.offsetX * density).toInt()
        currentParams.y = (cfg.offsetY * density).toInt()
        try {
          composeView?.let { windowManager?.updateViewLayout(it, currentParams) }
        } catch (e: Exception) {
          Log.e(TAG, "Error updating overlay layout", e)
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    appViewModelStore.clear()
    serviceScope.cancel()
    systemEventsRegistrar?.unregister()
    try {
      composeView?.let { windowManager?.removeViewImmediate(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error removing overlay view", e)
    }
    composeView = null
  }

  companion object {
    private const val TAG = "CapsuleOverlayService"
    const val CHANNEL_ID = "opencapsule_overlay_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP = "com.example.ACTION_STOP_CAPSULE_OVERLAY"

    fun startService(context: Context) {
      val intent = Intent(context, CapsuleOverlayService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, CapsuleOverlayService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }
}
