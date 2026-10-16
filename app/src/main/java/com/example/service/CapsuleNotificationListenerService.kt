package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.core.CapsuleManager
import com.example.model.CapsuleEvent

/**
 * Legitimate NotificationListenerService used exclusively for displaying user-selected
 * alerts and active media session playback in the capsule UI.
 *
 * Privacy Guarantees:
 * - Local-only in-memory processing.
 * - Zero storage: Notification contents are never saved to disk, database, or cache.
 * - Zero transmission: No notification data ever leaves this device.
 * - Zero analytics: No tracking or telemetry is attached to notifications.
 */
class CapsuleNotificationListenerService : NotificationListenerService() {

  private var mediaSessionManager: MediaSessionManager? = null
  private var activeMediaController: MediaController? = null

  private val mediaCallback = object : MediaController.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackState?) {
      updateMediaFromController(activeMediaController)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
      updateMediaFromController(activeMediaController)
    }
  }

  private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
    activeMediaController?.unregisterCallback(mediaCallback)
    // Prefer Echo Music when it has an active media session; otherwise keep
    // the existing behavior and use the first active session.
    activeMediaController = controllers?.firstOrNull { isEchoMusicPackage(it.packageName) }
      ?: controllers?.firstOrNull()
    activeMediaController?.registerCallback(mediaCallback)
    registerMediaActionHandler(activeMediaController)
    updateMediaFromController(activeMediaController)
  }

  private fun isEchoMusicPackage(packageName: String): Boolean {
    val appLabel = try {
      packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
      ""
    }
    val identity = "$packageName $appLabel".lowercase()
    return identity.contains("echo") && identity.contains("music")
  }

  private fun registerMediaActionHandler(controller: MediaController?) {
    if (controller != null) {
      CapsuleManager.mediaActionHandler = object : CapsuleManager.MediaActionHandler {
        override fun onPlayPause() {
          try {
            val pbState = controller.playbackState?.state
            if (pbState == PlaybackState.STATE_PLAYING) {
              controller.transportControls?.pause()
            } else {
              controller.transportControls?.play()
            }
          } catch (e: Exception) {
            Log.e(TAG, "Error toggling media play/pause", e)
          }
        }

        override fun onSkipToNext() {
          try {
            controller.transportControls?.skipToNext()
          } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next track", e)
          }
        }

        override fun onSkipToPrevious() {
          try {
            controller.transportControls?.skipToPrevious()
          } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous track", e)
          }
        }
      }
    } else {
      CapsuleManager.mediaActionHandler = null
    }
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    try {
      mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
      val component = ComponentName(this, CapsuleNotificationListenerService::class.java)
      val sessions = mediaSessionManager?.getActiveSessions(component)
      activeMediaController = sessions?.firstOrNull { isEchoMusicPackage(it.packageName) }
        ?: sessions?.firstOrNull()
      activeMediaController?.registerCallback(mediaCallback)
      mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
      registerMediaActionHandler(activeMediaController)
      updateMediaFromController(activeMediaController)
    } catch (_: SecurityException) {
      // Permission not yet granted or restricted
    }
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    try {
      activeMediaController?.unregisterCallback(mediaCallback)
      mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
      registerMediaActionHandler(null)
    } catch (_: Exception) {}
  }

  override fun onDestroy() {
    super.onDestroy()
    registerMediaActionHandler(null)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null || sbn.isOngoing) return
    val config = CapsuleManager.config.value
    if (!config.featureNotifications) return

    val pkg = sbn.packageName ?: ""
    if (pkg.isEmpty() || pkg == packageName || config.blockedPackages.contains(pkg)) return

    // Record discovered package so user can easily toggle it in app selection UI
    CapsuleManager.recordDiscoveredPackage(pkg)

    // Per-app notification filtering: drop notifications if app is not in the allowed list
    if (config.notificationFilterEnabled && !config.allowedNotificationPackages.contains(pkg)) {
      Log.d(TAG, "Notification from $pkg skipped by per-app filter")
      return
    }

    val extras = sbn.notification.extras ?: return
    val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

    if (rawTitle.isEmpty() && rawText.isEmpty()) return

    val pm = packageManager
    val appName = try {
      val appInfo = pm.getApplicationInfo(pkg, 0)
      pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
      pkg
    }

    val displayContent = if (config.notificationPreviewsEnabled) {
      rawText
    } else {
      "New notification"
    }

    val event = CapsuleEvent.Notification(
      id = "notif_${sbn.id}_${sbn.postTime}",
      title = rawTitle.ifEmpty { appName },
      content = displayContent,
      appName = appName,
      packageName = pkg,
      timestamp = sbn.postTime,
      isSensitive = !config.notificationPreviewsEnabled
    )

    CapsuleManager.postEvent(event)
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    if (sbn == null) return
    CapsuleManager.removeEvent("notif_${sbn.id}_${sbn.postTime}")
  }

  private fun updateMediaFromController(controller: MediaController?) {
    if (controller == null) {
      CapsuleManager.removeEvent("media_active")
      return
    }

    val config = CapsuleManager.config.value
    if (!config.featureMedia) return

    val metadata = controller.metadata
    val state = controller.playbackState

    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
      ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
      ?: "Playing Audio"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
      ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
      ?: "Media Session"
    val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
      ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    val isPlaying = state?.state == PlaybackState.STATE_PLAYING
    val position = state?.position ?: 0L

    if (isPlaying || state?.state == PlaybackState.STATE_PAUSED) {
      CapsuleManager.postEvent(
        CapsuleEvent.Media(
          title = title,
          artist = artist,
          albumArt = art,
          isPlaying = isPlaying,
          progressMs = position,
          durationMs = duration,
          appName = controller.packageName ?: "Music",
          packageName = controller.packageName ?: ""
        )
      )
    } else if (state?.state == PlaybackState.STATE_STOPPED || state?.state == PlaybackState.STATE_NONE) {
      CapsuleManager.removeEvent("media_active")
    }
  }

  companion object {
    private const val TAG = "CapsuleNotifListener"
  }
}
