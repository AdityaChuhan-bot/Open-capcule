package com.example.service

import android.media.session.MediaController
import android.media.session.PlaybackState

object MediaControlBridge {
  @Volatile private var controller: MediaController? = null
  fun attach(controller: MediaController?) { this.controller = controller }
  fun detach(controller: MediaController?) { if (this.controller == controller) this.controller = null }
  fun playPause() {
    val c = controller ?: return
    if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
    else c.transportControls.play()
  }
  fun next() { controller?.transportControls?.skipToNext() }
  fun previous() { controller?.transportControls?.skipToPrevious() }
}
