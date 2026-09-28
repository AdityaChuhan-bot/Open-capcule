package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.screens.LauncherHomeScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Dedicated HOME entry point. Kept separate from MainActivity so the launcher
 * does not open inside the Studio/settings scaffold or onboarding flow.
 */
class LauncherActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        LauncherHomeScreen()
      }
    }
  }
}
