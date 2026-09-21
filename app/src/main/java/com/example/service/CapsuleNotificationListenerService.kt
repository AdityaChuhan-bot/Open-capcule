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
    activeMediaController = controllers?.firstOrNull()
    activeMediaController?.registerCallback(mediaCallback)
    MediaControlBridge.attach(activeMediaController)
    updateMediaFromController(activeMediaController)
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    try {
      mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
      val component = ComponentName(this, CapsuleNotificationListenerService::class.java)
      val sessions = mediaSessionManager?.getActiveSessions(component)
      activeMediaController = sessions?.firstOrNull()
      activeMediaController?.registerCallback(mediaCallback)
      mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
      updateMediaFromController(activeMediaController)
    } catch (_: SecurityException) {
      // Permission not yet granted or restricted
    }
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    try {
      activeMediaController?.unregisterCallback(mediaCallback)
      MediaControlBridge.detach(activeMediaController)
      mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
    } catch (_: Exception) {}
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val config = CapsuleManager.config.value
    if (!config.featureNotifications) return

    val pkg = sbn.packageName ?: ""
    if (pkg == packageName || config.blockedPackages.contains(pkg)) return

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
    if (config.consumeNotifications && sbn.packageName != packageName) {
      try { cancelNotification(sbn.key) } catch (_: SecurityException) { }
    }
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
}
