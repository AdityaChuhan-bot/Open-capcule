package com.example

import android.app.Application
import com.example.core.CapsuleManager

class OpenCapsuleApp : Application() {
  override fun onCreate() {
    super.onCreate()
    CapsuleManager.initialize(this)
  }
}
