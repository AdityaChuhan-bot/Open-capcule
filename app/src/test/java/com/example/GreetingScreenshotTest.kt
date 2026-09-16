package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.ui.capsule.OpenCapsuleDynamicView
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        OpenCapsuleDynamicView(
          event = CapsuleEvent.Media(
            title = "Midnight Horizon",
            artist = "Solaris Wave",
            isPlaying = true,
            progressMs = 65000L,
            durationMs = 210000L,
            appName = "Music"
          ),
          isExpanded = false,
          config = CapsuleConfig(),
          onToggleExpand = {},
          onCollapse = {}
        )
      }
    }

    val candidateDirs = listOf(
      File("src/test/screenshots"),
      File("app/src/test/screenshots"),
      File(System.getProperty("user.dir"), "src/test/screenshots"),
      File(System.getProperty("user.dir"), "app/src/test/screenshots")
    )
    val existingDir = candidateDirs.firstOrNull { it.exists() } ?: File("src/test/screenshots")
    val screenshotFile = File(existingDir, "greeting.png")
    composeTestRule.onRoot().captureRoboImage(filePath = screenshotFile.path)
  }
}
