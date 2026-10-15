package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.ViewConfiguration
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
  private var detectedCutoutRect: Rect? = null

  /**
   * Callback invoked when a tap occurs outside the expanded capsule overlay window.
   * Default implementation collapses the capsule.
   */
  var onOutsideTapCallback: (() -> Unit)? = {
    CapsuleManager.collapseCapsule()
  }

  /**
   * Callback invoked when a swipe-up gesture is detected to collapse the expanded capsule.
   * Default implementation collapses the capsule.
   */
  var onSwipeUpCallback: (() -> Unit)? = {
    CapsuleManager.collapseCapsule()
  }

  fun setOnOutsideTapListener(callback: () -> Unit) {
    onOutsideTapCallback = callback
  }

  fun setOnSwipeUpListener(callback: () -> Unit) {
    onSwipeUpCallback = callback
  }

  fun handleOutsideTap() {
    if (!CapsuleManager.config.value.tapOutsideToDismissEnabled) return
    Log.d(TAG, "Dismissing expanded capsule from outside tap")
    onOutsideTapCallback?.invoke()
    globalOnOutsideTapCallback?.invoke()
  }

  fun handleSwipeUp() {
    if (!CapsuleManager.config.value.swipeUpToCollapseEnabled) return
    Log.d(TAG, "Collapsing expanded capsule from swipe-up gesture")
    onSwipeUpCallback?.invoke()
    globalOnSwipeUpCallback?.invoke()
  }

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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }
    }
    windowParams = params

    try {
      val view = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@CapsuleOverlayService)
        setViewTreeSavedStateRegistryOwner(this@CapsuleOverlayService)
        setViewTreeViewModelStoreOwner(this@CapsuleOverlayService)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          setOnApplyWindowInsetsListener { _, insets ->
            val cutout = insets.displayCutout
            if (cutout != null) {
              val rects = cutout.boundingRects
              val topLimitPx = (64f * resources.displayMetrics.density).toInt()
              val topRect = rects.firstOrNull { it.top <= topLimitPx } ?: rects.minByOrNull { it.top }
              if (topRect != null && topRect != detectedCutoutRect) {
                detectedCutoutRect = topRect
                updateOverlayLayout()
              }
            }
            insets
          }
        }

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
                onCollapse = { handleSwipeUp() }
              )
            }
          }
        }

        // GestureDetector for detecting swipe-up gestures to collapse the capsule
        val gestureDetector = GestureDetector(
          this@CapsuleOverlayService,
          object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
              e1: MotionEvent?,
              e2: MotionEvent,
              velocityX: Float,
              velocityY: Float
            ): Boolean {
              if (CapsuleManager.isExpanded.value && e1 != null) {
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                // Fast upward flick
                if (deltaY < -20f && Math.abs(deltaY) > Math.abs(deltaX) && velocityY < -80f) {
                  handleSwipeUp()
                  return true
                }
              }
              return false
            }

            override fun onScroll(
              e1: MotionEvent?,
              e2: MotionEvent,
              distanceX: Float,
              distanceY: Float
            ): Boolean {
              // Upward scrolling/drag
              if (CapsuleManager.isExpanded.value && distanceY > 25f && Math.abs(distanceY) > Math.abs(distanceX)) {
                handleSwipeUp()
                return true
              }
              return false
            }
          }
        )

        var touchStartY = 0f
        var touchStartX = 0f
        var isTrackingSwipe = false
        var trackingCollapsedTap = false
        var collapsedDownX = 0f
        var collapsedDownY = 0f
        val collapsedTapSlop = ViewConfiguration.get(this@CapsuleOverlayService).scaledTouchSlop

        setOnTouchListener { _, event ->
          // Keep the window geometry unchanged during ACTION_DOWN. Expanding on
          // touch-down resizes the overlay mid-gesture and can cancel/retarget
          // the tap on Android 16/OEM window managers.
          if (!CapsuleManager.isExpanded.value) {
            when (event.actionMasked) {
              MotionEvent.ACTION_DOWN -> {
                trackingCollapsedTap = true
                collapsedDownX = event.rawX
                collapsedDownY = event.rawY
                return@setOnTouchListener true
              }
              MotionEvent.ACTION_MOVE -> {
                if (trackingCollapsedTap &&
                  (kotlin.math.abs(event.rawX - collapsedDownX) > collapsedTapSlop ||
                   kotlin.math.abs(event.rawY - collapsedDownY) > collapsedTapSlop)) {
                  trackingCollapsedTap = false
                }
                return@setOnTouchListener trackingCollapsedTap
              }
              MotionEvent.ACTION_UP -> {
                val wasTap = trackingCollapsedTap &&
                  kotlin.math.abs(event.rawX - collapsedDownX) <= collapsedTapSlop &&
                  kotlin.math.abs(event.rawY - collapsedDownY) <= collapsedTapSlop
                trackingCollapsedTap = false
                if (wasTap) CapsuleManager.expandCapsule()
                return@setOnTouchListener true
              }
              MotionEvent.ACTION_CANCEL -> {
                trackingCollapsedTap = false
                return@setOnTouchListener true
              }
            }
          }

          // Tap outside the overlay window dismisses the expanded capsule
          if (event.action == MotionEvent.ACTION_OUTSIDE) {
            if (CapsuleManager.isExpanded.value) {
              handleOutsideTap()
              return@setOnTouchListener true
            }
            return@setOnTouchListener false
          }

          // Gesture & touch tracking for swipe-up collapse when expanded
          if (CapsuleManager.isExpanded.value) {
            if (gestureDetector.onTouchEvent(event)) {
              return@setOnTouchListener true
            }

            val density = resources.displayMetrics.density
            val minSwipeDist = 24f * density

            when (event.actionMasked) {
              MotionEvent.ACTION_DOWN -> {
                touchStartY = event.rawY
                touchStartX = event.rawX
                isTrackingSwipe = true
              }
              MotionEvent.ACTION_MOVE -> {
                if (isTrackingSwipe) {
                  val diffY = event.rawY - touchStartY
                  val diffX = event.rawX - touchStartX
                  if (diffY < -minSwipeDist && Math.abs(diffY) > Math.abs(diffX) * 1.1f) {
                    isTrackingSwipe = false
                    handleSwipeUp()
                    return@setOnTouchListener true
                  }
                }
              }
              MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTrackingSwipe) {
                  val diffY = event.rawY - touchStartY
                  val diffX = event.rawX - touchStartX
                  if (diffY < -minSwipeDist && Math.abs(diffY) > Math.abs(diffX)) {
                    isTrackingSwipe = false
                    handleSwipeUp()
                    return@setOnTouchListener true
                  }
                }
                isTrackingSwipe = false
              }
            }
          }
          false
        }
      }
      composeView = view

      windowManager?.addView(view, params)
      // Some OEMs do not dispatch cutout insets until the overlay is attached.
      view.requestApplyInsets()
      view.post { updateOverlayLayout() }
      Log.d(TAG, "Overlay ComposeView successfully attached to WindowManager")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize overlay ComposeView", e)
    }

    // Listen to config changes and update window position
    serviceScope.launch {
      CapsuleManager.config.collect {
        updateOverlayLayout()
      }
    }

    // Listen to expanded state changes to refresh touch dispatch and window flags
    serviceScope.launch {
      CapsuleManager.isExpanded.collect {
        updateOverlayLayout()
      }
    }
  }

  private fun updateOverlayLayout() {
    val currentParams = windowParams ?: return
    val cfg = CapsuleManager.config.value
    val isExpanded = CapsuleManager.isExpanded.value
    val displayMetrics = resources.displayMetrics
    val density = displayMetrics.density
    val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try { windowManager?.currentWindowMetrics?.bounds?.width() ?: displayMetrics.widthPixels }
      catch (_: Exception) { displayMetrics.widthPixels }
    } else displayMetrics.widthPixels

    val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    currentParams.flags = if (isExpanded && cfg.tapOutsideToDismissEnabled) {
      baseFlags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    } else {
      baseFlags
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      currentParams.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
      } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }

    val currentWidthDp = if (isExpanded) cfg.expandedWidthDp else cfg.collapsedWidthDp
    val cutout = detectedCutoutRect
    if (cfg.autoAlignCutout) {
      // Center punch-hole on this device. Overlay inset Rect coordinates may be
      // relative to the overlay rather than full display, causing the observed
      // left shift. Use stable center gravity and anchor below status-bar inset.
      currentParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
      currentParams.x = (cfg.offsetX * density).toInt()
      val statusBarTop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
          windowManager?.currentWindowMetrics?.windowInsets
            ?.getInsets(android.view.WindowInsets.Type.statusBars())?.top ?: 0
        } catch (_: Exception) { 0 }
      } else 0
      // DisplayCutout bounding rectangles are in display coordinates. For an
      // overlay using LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, adding the status-bar
      // inset places the pill below the physical punch-hole. Anchor at the top
      // edge of the display instead; use cutout.top only when it is a valid
      // display-coordinate value. The default offset baseline (12dp) means 0dp.
      val cutoutTop = cutout?.top?.takeIf {
        it >= 0 && it < (48f * density).toInt()
      }
      val targetTop = cutoutTop ?: 0
      currentParams.y = (targetTop + ((cfg.offsetY - 12) * density).toInt()).coerceAtLeast(0)
    } else {
      val g = when (cfg.position) {
        CapsulePosition.LEFT -> Gravity.TOP or Gravity.START
        CapsulePosition.RIGHT -> Gravity.TOP or Gravity.END
        CapsulePosition.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
      }
      currentParams.gravity = g
      currentParams.x = (cfg.offsetX * density).toInt()
      currentParams.y = (cfg.offsetY * density).toInt()
    }

    try {
      composeView?.let { windowManager?.updateViewLayout(it, currentParams) }
    } catch (e: Exception) {
      Log.e(TAG, "Error updating overlay layout", e)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    updateOverlayLayout()
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

    /**
     * Global callbacks for outside taps and swipe-up gestures, allowing app activities
     * or testing frameworks to listen for overlay interaction events.
     */
    var globalOnOutsideTapCallback: (() -> Unit)? = null
    var globalOnSwipeUpCallback: (() -> Unit)? = null

    fun setGlobalOutsideTapListener(callback: () -> Unit) {
      globalOnOutsideTapCallback = callback
    }

    fun setGlobalSwipeUpListener(callback: () -> Unit) {
      globalOnSwipeUpCallback = callback
    }
  }
}
