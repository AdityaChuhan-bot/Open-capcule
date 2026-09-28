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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
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
 * Long-lived foreground service responsible for the actual floating capsule window.
 *
 * The Activity is only the configuration UI. The ComposeView below remains attached to
 * WindowManager while this service is alive, so leaving the Activity does not remove the
 * idle capsule or its event UI.
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

    try {
      systemEventsRegistrar = SystemEventsRegistrar(this).apply { register() }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to register system events receiver", e)
    }

    if (Settings.canDrawOverlays(this)) {
      initOverlayView()
    } else {
      stopSelf()
      return
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
      getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
  }

  private fun startForegroundServiceWithNotification() {
    val stopIntent = Intent(this, CapsuleOverlayService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      0,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val openAppPendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification: Notification = Notification.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.overlay_notification_title))
      .setContentText(getString(R.string.overlay_notification_text))
      .setSmallIcon(R.drawable.ic_stat_capsule)
      .setContentIntent(openAppPendingIntent)
      .addAction(
        Notification.Action.Builder(
          android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
          getString(R.string.overlay_notification_stop),
          stopPendingIntent
        ).build()
      )
      .setOngoing(true)
      .setCategory(Notification.CATEGORY_SERVICE)
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
    } catch (e: Exception) {
      Log.e(TAG, "Unable to promote overlay service to foreground", e)
      stopSelf()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      CapsuleManager.updateConfig { it.copy(isOverlayEnabled = false) }
      stopSelf()
      return START_NOT_STICKY
    }
    return START_STICKY
  }

  private fun calculateInitialY(density: Float): Int {
    // Anchor the island to the display top and let expanded content grow downward.
    // Centering a tall transparent hit target on the tiny cutout shifted the pill down.
    return (CapsuleManager.config.value.offsetY * density).toInt()
  }

  private fun resolveHorizontalPosition(density: Float): Pair<Int, Int> {
    val config = CapsuleManager.config.value
    val gravity = when (config.position) {
      CapsulePosition.LEFT -> Gravity.TOP or Gravity.START
      CapsulePosition.RIGHT -> Gravity.TOP or Gravity.END
      CapsulePosition.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    var xOffset = (config.offsetX * density).toInt()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && config.position == CapsulePosition.CENTER) {
      val metrics = windowManager?.currentWindowMetrics
      val screenCenterX = metrics?.bounds?.centerX()
      val cameraCutout = metrics?.windowInsets?.displayCutout?.boundingRects
        ?.filter { it.width() > 0 && it.height() > 0 }
        ?.minByOrNull { rect ->
          kotlin.math.abs(rect.centerX() - (screenCenterX ?: rect.centerX())) +
            rect.top.coerceAtLeast(0) * 2
        }

      if (cameraCutout != null && screenCenterX != null) {
        // Gravity centers the window on the display; compensate for a camera cutout
        // that is not exactly at the display center.
        xOffset += cameraCutout.centerX() - screenCenterX
      }
    }
    return gravity to xOffset
  }

  private fun initOverlayView() {
    windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    val density = resources.displayMetrics.density
    val (gravity, x) = resolveHorizontalPosition(density)

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      PixelFormat.TRANSLUCENT
    ).apply {
      this.gravity = gravity
      this.x = x
      this.y = calculateInitialY(density)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
      }
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

            // Keep the idle capsule composed whenever the user has enabled
            // "Always Show Idle Pill"; transient events are also composed while active.
            if (activeEvent != null || config.alwaysShowWhenIdle) {
              // Give the compact island a larger transparent hit target so it remains
              // reliably tappable on the launcher/status-bar area without changing its
              // visual size.
              Box(
                modifier = Modifier
                  .width(if (isExpanded) config.expandedWidthDp.dp + 32.dp else 180.dp)
                  .height(if (isExpanded) config.expandedHeightDp.dp + 32.dp else 72.dp),
                contentAlignment = Alignment.TopCenter
              ) {
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
      }
      composeView = view
      windowManager?.addView(view, params)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize overlay ComposeView", e)
      stopSelf()
      return
    }

    serviceScope.launch {
      CapsuleManager.config.collect { cfg ->
        val currentParams = windowParams ?: return@collect
        val (newGravity, newX) = resolveHorizontalPosition(density)
        currentParams.gravity = when (cfg.position) {
          CapsulePosition.LEFT -> Gravity.TOP or Gravity.START
          CapsulePosition.RIGHT -> Gravity.TOP or Gravity.END
          CapsulePosition.CENTER -> newGravity
        }
        currentParams.x = newX
        currentParams.y = calculateInitialY(density)
        try {
          composeView?.let { windowManager?.updateViewLayout(it, currentParams) }
        } catch (e: Exception) {
          Log.e(TAG, "Error updating overlay layout", e)
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    val density = resources.displayMetrics.density
    val params = windowParams ?: return
    val (gravity, x) = resolveHorizontalPosition(density)
    params.gravity = gravity
    params.x = x
    params.y = calculateInitialY(density)
    try {
      composeView?.let { windowManager?.updateViewLayout(it, params) }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to reposition capsule after configuration change", e)
    }
  }

  override fun onDestroy() {
    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    systemEventsRegistrar?.unregister()
    serviceScope.cancel()
    appViewModelStore.clear()
    try {
      composeView?.let { windowManager?.removeViewImmediate(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error removing overlay view", e)
    }
    composeView = null
    windowParams = null
    super.onDestroy()
  }

  companion object {
    private const val TAG = "CapsuleOverlayService"
    const val CHANNEL_ID = "opencapsule_overlay_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP = "com.example.ACTION_STOP_CAPSULE_OVERLAY"

    fun startService(context: Context) {
      if (!Settings.canDrawOverlays(context)) return
      val intent = Intent(context, CapsuleOverlayService::class.java)
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: RuntimeException) {
        Log.e(TAG, "Could not start overlay service", e)
      }
    }

    fun stopService(context: Context) {
      try {
        context.startService(Intent(context, CapsuleOverlayService::class.java).apply {
          action = ACTION_STOP
        })
      } catch (_: Exception) {
        context.stopService(Intent(context, CapsuleOverlayService::class.java))
      }
    }
  }
}
