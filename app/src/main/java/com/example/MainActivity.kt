package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.core.CapsuleManager
import com.example.ui.screens.CustomizationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataSafetyScreen
import com.example.ui.screens.OnboardingFlow
import com.example.ui.screens.PermissionAuditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        OpenCapsuleAppRoot()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCapsuleAppRoot() {
  val config by CapsuleManager.config.collectAsState()
  val activeEvent by CapsuleManager.activeEvent.collectAsState()
  val isExpanded by CapsuleManager.isExpanded.collectAsState()

  if (!config.isOnboardingCompleted) {
    OnboardingFlow(
      config = config,
      onComplete = {
        CapsuleManager.updateConfig { it.copy(isOnboardingCompleted = true) }
      }
    )
  } else {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = when (selectedTab) {
                0 -> "OpenCapsule Studio"
                1 -> "Customization & Notch"
                2 -> "Permission Audit"
                3 -> "Data Safety & Policy"
                else -> "OpenCapsule"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
          )
        )
      },
      bottomBar = {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.testTag("main_navigation_bar")
        ) {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Studio") },
            label = { Text("Studio") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.Black,
              indicatorColor = PrimaryBlue
            ),
            modifier = Modifier.testTag("nav_item_studio")
          )

          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(Icons.Default.Tune, contentDescription = "Customize") },
            label = { Text("Customize") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.Black,
              indicatorColor = PrimaryBlue
            ),
            modifier = Modifier.testTag("nav_item_customize")
          )

          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(Icons.Default.Security, contentDescription = "Permissions") },
            label = { Text("Permissions") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.Black,
              indicatorColor = PrimaryBlue
            ),
            modifier = Modifier.testTag("nav_item_permissions")
          )

          NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            icon = { Icon(Icons.Default.Policy, contentDescription = "Safety") },
            label = { Text("Safety") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.Black,
              indicatorColor = PrimaryBlue
            ),
            modifier = Modifier.testTag("nav_item_safety")
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "tab_transition"
        ) { tab ->
          when (tab) {
            0 -> DashboardScreen(
              config = config,
              activeEvent = activeEvent,
              isExpanded = isExpanded,
              onOpenAudit = { selectedTab = 2 }
            )
            1 -> CustomizationScreen(config = config)
            2 -> PermissionAuditScreen()
            3 -> DataSafetyScreen()
          }
        }
      }
    }
  }
}
