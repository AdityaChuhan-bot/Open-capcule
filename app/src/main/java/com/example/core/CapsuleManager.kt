package com.example.core

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.CapsulePreferencesRepository
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class DemoEventType {
  MEDIA,
  NOTIFICATION,
  CALL_INCOMING,
  CALL_ONGOING,
  TIMER,
  STOPWATCH,
  CHARGING,
  BLUETOOTH,
  DOWNLOAD,
  SCREENSHOT,
  RINGER
}

object CapsuleManager {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var repository: CapsulePreferencesRepository? = null
  private var appContext: Context? = null

  private val _config = MutableStateFlow(CapsuleConfig())
  val config: StateFlow<CapsuleConfig> = _config.asStateFlow()

  private val _activeEvent = MutableStateFlow<CapsuleEvent?>(null)
  val activeEvent: StateFlow<CapsuleEvent?> = _activeEvent.asStateFlow()

  private val _isExpanded = MutableStateFlow(false)
  val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

  // In-memory queue of active events by ID
  private val eventMap = ConcurrentHashMap<String, CapsuleEvent>()
  private var transientDismissJob: Job? = null
  private var timerTickJob: Job? = null

  fun initialize(context: Context) {
    if (appContext != null) return
    appContext = context.applicationContext
    val repo = CapsulePreferencesRepository(context.applicationContext)
    repository = repo

    scope.launch {
      repo.configFlow.collectLatest { newConfig ->
        _config.value = newConfig
      }
    }
  }

  fun updateConfig(update: (CapsuleConfig) -> CapsuleConfig) {
    scope.launch {
      repository?.updateConfig(update)
    }
  }

  fun postEvent(event: CapsuleEvent) {
    val cfg = _config.value
    // Check if corresponding feature is enabled
    val isEnabled = when (event) {
      is CapsuleEvent.Media -> cfg.featureMedia
      is CapsuleEvent.Notification -> cfg.featureNotifications && !cfg.blockedPackages.contains(event.packageName)
      is CapsuleEvent.Call -> cfg.featureCalls
      is CapsuleEvent.Timer, is CapsuleEvent.Stopwatch -> cfg.featureTimers
      is CapsuleEvent.Charging -> cfg.featureCharging
      is CapsuleEvent.Bluetooth -> cfg.featureBluetooth
      is CapsuleEvent.Download -> cfg.featureDownloads
      is CapsuleEvent.Screenshot, is CapsuleEvent.Ringer -> cfg.featureSystemEvents
    }

    if (!isEnabled) return

    eventMap[event.id] = event
    reevaluateActiveEvent(newEvent = event)
  }

  fun removeEvent(id: String) {
    eventMap.remove(id)
    if (_activeEvent.value?.id == id) {
      reevaluateActiveEvent(null)
    }
  }

  fun clearAllEvents() {
    eventMap.clear()
    _activeEvent.value = null
    _isExpanded.value = false
    transientDismissJob?.cancel()
    timerTickJob?.cancel()
  }

  private fun reevaluateActiveEvent(newEvent: CapsuleEvent?) {
    // Pick the highest priority event (lowest priority integer)
    val candidate = eventMap.values.minByOrNull { it.priority }

    if (candidate != null) {
      val isDifferent = _activeEvent.value?.id != candidate.id
      _activeEvent.value = candidate

      if (isDifferent) {
        performHaptic()
      }

      // If this is a transient event, schedule auto-dismissal / collapse
      transientDismissJob?.cancel()
      if (!candidate.isPersistent) {
        val timeout = _config.value.autoCollapseTimeoutSeconds
        transientDismissJob = scope.launch {
          delay(timeout * 1000L)
          // If still displaying this transient event, remove it and reevaluate
          if (_activeEvent.value?.id == candidate.id) {
            eventMap.remove(candidate.id)
            _isExpanded.value = false
            reevaluateActiveEvent(null)
          }
        }
      }
    } else {
      _activeEvent.value = null
      _isExpanded.value = false
    }
  }

  fun toggleExpand() {
    performHaptic()
    _isExpanded.value = !_isExpanded.value
  }

  fun expandCapsule() {
    if (!_isExpanded.value) {
      performHaptic()
      _isExpanded.value = true
    }
  }

  fun collapseCapsule() {
    if (_isExpanded.value) {
      performHaptic()
      _isExpanded.value = false
    }
  }

  private fun performHaptic() {
    appContext?.let { ctx ->
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vibratorManager?.defaultVibrator?.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
          )
        } else {
          @Suppress("DEPRECATION")
          val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
          } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20L)
          }
        }
      } catch (_: Exception) {
        // Safe fallback if vibration permission or hardware unavailable
      }
    }
  }

  // Real Media Session Action Handler (hooked by CapsuleNotificationListenerService)
  interface MediaActionHandler {
    fun onPlayPause()
    fun onSkipToNext()
    fun onSkipToPrevious()
  }

  @Volatile
  var mediaActionHandler: MediaActionHandler? = null

  // User Actions from Capsule Controls
  fun onMediaPlayPause() {
    performHaptic()
    val handler = mediaActionHandler
    if (handler != null) {
      handler.onPlayPause()
    } else {
      val current = _activeEvent.value as? CapsuleEvent.Media ?: return
      val updated = current.copy(isPlaying = !current.isPlaying)
      postEvent(updated)
    }
  }

  fun onMediaNext() {
    performHaptic()
    val handler = mediaActionHandler
    if (handler != null) {
      handler.onSkipToNext()
    } else {
      val current = _activeEvent.value as? CapsuleEvent.Media ?: return
      val updated = current.copy(progressMs = 0L)
      postEvent(updated)
    }
  }

  fun onMediaPrevious() {
    performHaptic()
    val handler = mediaActionHandler
    if (handler != null) {
      handler.onSkipToPrevious()
    } else {
      val current = _activeEvent.value as? CapsuleEvent.Media ?: return
      val updated = current.copy(progressMs = 0L)
      postEvent(updated)
    }
  }

  fun onTimerPauseResume() {
    val current = _activeEvent.value as? CapsuleEvent.Timer ?: return
    val updated = current.copy(isRunning = !current.isRunning)
    postEvent(updated)
    performHaptic()
  }

  fun onTimerReset() {
    removeEvent("timer_active")
    timerTickJob?.cancel()
    performHaptic()
  }

  fun onTimerAddMinute() {
    val current = _activeEvent.value as? CapsuleEvent.Timer ?: return
    val updated = current.copy(
      remainingSeconds = current.remainingSeconds + 60,
      totalSeconds = current.totalSeconds + 60,
      isAlarmState = false
    )
    postEvent(updated)
    performHaptic()
  }

  fun onCallAnswer() {
    val current = _activeEvent.value as? CapsuleEvent.Call ?: return
    val updated = current.copy(state = CapsuleEvent.CallState.ONGOING, durationSeconds = 0)
    postEvent(updated)
    performHaptic()
  }

  fun onCallEnd() {
    removeEvent("call_active")
    performHaptic()
  }

  // Demo simulator triggers for immediate testing and verification
  fun triggerDemo(type: DemoEventType) {
    performHaptic()
    when (type) {
      DemoEventType.MEDIA -> {
        postEvent(
          CapsuleEvent.Media(
            title = "Midnight Horizon",
            artist = "Solaris Wave",
            isPlaying = true,
            progressMs = 94000L,
            durationMs = 210000L,
            appName = "Music Player"
          )
        )
      }
      DemoEventType.NOTIFICATION -> {
        postEvent(
          CapsuleEvent.Notification(
            id = "demo_notif_${System.currentTimeMillis()}",
            title = "Sarah Jenkins",
            content = "Hey! Let's review the privacy audit for OpenCapsule tomorrow.",
            appName = "Messages",
            packageName = "com.google.android.apps.messaging",
            isSensitive = false
          )
        )
      }
      DemoEventType.CALL_INCOMING -> {
        postEvent(
          CapsuleEvent.Call(
            callerName = "Alex Rivera",
            state = CapsuleEvent.CallState.INCOMING
          )
        )
      }
      DemoEventType.CALL_ONGOING -> {
        postEvent(
          CapsuleEvent.Call(
            callerName = "Alex Rivera",
            state = CapsuleEvent.CallState.ONGOING,
            durationSeconds = 85L
          )
        )
      }
      DemoEventType.TIMER -> {
        timerTickJob?.cancel()
        var remaining = 45
        val total = 45
        postEvent(
          CapsuleEvent.Timer(
            label = "Tea Timer",
            remainingSeconds = remaining,
            totalSeconds = total,
            isRunning = true,
            isAlarmState = false
          )
        )
        timerTickJob = scope.launch {
          while (remaining > 0) {
            delay(1000L)
            remaining--
            val isAlarm = remaining == 0
            postEvent(
              CapsuleEvent.Timer(
                label = "Tea Timer",
                remainingSeconds = remaining,
                totalSeconds = total,
                isRunning = remaining > 0,
                isAlarmState = isAlarm
              )
            )
          }
        }
      }
      DemoEventType.STOPWATCH -> {
        timerTickJob?.cancel()
        var elapsed = 0L
        postEvent(CapsuleEvent.Stopwatch(elapsedSeconds = elapsed, isRunning = true))
        timerTickJob = scope.launch {
          while (true) {
            delay(1000L)
            elapsed++
            postEvent(CapsuleEvent.Stopwatch(elapsedSeconds = elapsed, isRunning = true))
          }
        }
      }
      DemoEventType.CHARGING -> {
        postEvent(
          CapsuleEvent.Charging(
            batteryPercent = 84,
            isCharging = true,
            isFastCharging = true,
            isLowBattery = false
          )
        )
      }
      DemoEventType.BLUETOOTH -> {
        postEvent(
          CapsuleEvent.Bluetooth(
            deviceName = "QuietComfort 45",
            isConnected = true,
            isHeadset = true
          )
        )
      }
      DemoEventType.DOWNLOAD -> {
        postEvent(
          CapsuleEvent.Download(
            id = "demo_dl",
            fileName = "ubuntu-minimal.iso",
            progressPercent = 68,
            isComplete = false
          )
        )
      }
      DemoEventType.SCREENSHOT -> {
        postEvent(CapsuleEvent.Screenshot())
      }
      DemoEventType.RINGER -> {
        postEvent(CapsuleEvent.Ringer(mode = CapsuleEvent.RingerMode.VIBRATE))
      }
    }
  }
}
