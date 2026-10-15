package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CapsuleManager
import com.example.model.CapsuleConfig
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FilterAppItem(
  val packageName: String,
  val label: String,
  val iconBitmap: ImageBitmap? = null
)

private fun Drawable.toImageBitmapSafe(): ImageBitmap? {
  return try {
    if (this is BitmapDrawable && this.bitmap != null) {
      this.bitmap.asImageBitmap()
    } else {
      val w = intrinsicWidth.coerceAtLeast(1).coerceAtMost(144)
      val h = intrinsicHeight.coerceAtLeast(1).coerceAtMost(144)
      val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      setBounds(0, 0, canvas.width, canvas.height)
      draw(canvas)
      bitmap.asImageBitmap()
    }
  } catch (_: Exception) {
    null
  }
}

/**
 * Full-featured UI dialog allowing users to manage per-app notification filtering.
 * Users can enable/disable filtering, search installed apps, toggle individual app permissions,
 * and perform batch actions (Select All / Deselect All).
 */
@Composable
fun AppNotificationFilterDialog(
  config: CapsuleConfig,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var installedApps by remember { mutableStateOf<List<FilterAppItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  // Query installed launcher applications asynchronously on IO dispatcher
  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val pm = context.packageManager
      val currentPkg = context.packageName

      val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

      val appsList = mutableListOf<FilterAppItem>()
      val seenPackages = mutableSetOf<String>()

      for (info in resolveInfos) {
        val pkg = info.activityInfo.packageName ?: continue
        if (pkg == currentPkg || seenPackages.contains(pkg)) continue
        seenPackages.add(pkg)

        val label = try {
          info.loadLabel(pm).toString()
        } catch (_: Exception) {
          pkg
        }

        val iconBitmap = try {
          info.loadIcon(pm)?.toImageBitmapSafe()
        } catch (_: Exception) {
          null
        }

        appsList.add(FilterAppItem(packageName = pkg, label = label, iconBitmap = iconBitmap))
      }

      // Also merge any discovered packages that posted notifications
      for (discoveredPkg in CapsuleManager.discoveredPackages.value) {
        if (!seenPackages.contains(discoveredPkg) && discoveredPkg != currentPkg) {
          seenPackages.add(discoveredPkg)
          val label = try {
            val appInfo = pm.getApplicationInfo(discoveredPkg, 0)
            pm.getApplicationLabel(appInfo).toString()
          } catch (_: Exception) {
            discoveredPkg
          }
          val iconBitmap = try {
            pm.getApplicationIcon(discoveredPkg).toImageBitmapSafe()
          } catch (_: Exception) {
            null
          }
          appsList.add(FilterAppItem(packageName = discoveredPkg, label = label, iconBitmap = iconBitmap))
        }
      }

      // Sort alphabetically by label
      appsList.sortBy { it.label.lowercase() }

      withContext(Dispatchers.Main) {
        installedApps = appsList
        isLoading = false
      }
    }
  }

  val filteredApps by remember(searchQuery, installedApps) {
    derivedStateOf {
      if (searchQuery.isBlank()) {
        installedApps
      } else {
        val q = searchQuery.trim().lowercase()
        installedApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
    }
  }

  val allowedCount = config.allowedNotificationPackages.size
  val totalCount = installedApps.size

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f)
        .testTag("dialog_app_notification_filter"),
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                "App Notification Filter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                if (config.notificationFilterEnabled) "$allowedCount of $totalCount apps allowed" else "Allowing all apps (Filter off)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_close_app_filter_dialog")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        // Master Toggle Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = if (config.notificationFilterEnabled) PrimaryBlue.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Per-App Filtering",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
              )
              Text(
                if (config.notificationFilterEnabled)
                  "Active: Only notifications from checked apps will appear in the capsule"
                else
                  "Inactive: Notifications from all installed apps are shown in the capsule",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = config.notificationFilterEnabled,
              onCheckedChange = { isChecked ->
                CapsuleManager.updateConfig {
                  val updated = it.copy(notificationFilterEnabled = isChecked)
                  // If enabling for the first time and empty, initialize with current apps
                  if (isChecked && updated.allowedNotificationPackages.isEmpty() && installedApps.isNotEmpty()) {
                    updated.copy(allowedNotificationPackages = installedApps.map { a -> a.packageName }.toSet())
                  } else {
                    updated
                  }
                }
              },
              colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
              modifier = Modifier.testTag("switch_master_notification_filter")
            )
          }
        }

        // Quick batch actions row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              val allPkgs = installedApps.map { it.packageName }.toSet()
              CapsuleManager.updateConfig { it.copy(allowedNotificationPackages = allPkgs) }
            },
            modifier = Modifier
              .weight(1f)
              .testTag("btn_select_all_apps"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Select All", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = {
              CapsuleManager.updateConfig { it.copy(allowedNotificationPackages = emptySet()) }
            },
            modifier = Modifier
              .weight(1f)
              .testTag("btn_deselect_all_apps"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Deselect All", fontSize = 12.sp)
          }
        }

        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_search_apps"),
          placeholder = { Text("Search by name or package...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp)
        )

        // Apps List
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          if (isLoading) {
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text("Loading installed applications...", style = MaterialTheme.typography.bodySmall)
            }
          } else if (filteredApps.isEmpty()) {
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "No applications match '$searchQuery'",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("list_filterable_apps"),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredApps, key = { it.packageName }) { appItem ->
                val isAllowed = config.allowedNotificationPackages.contains(appItem.packageName)

                AppFilterRow(
                  app = appItem,
                  isAllowed = isAllowed,
                  filterActive = config.notificationFilterEnabled,
                  onToggle = { shouldAllow ->
                    CapsuleManager.updateConfig { current ->
                      val updatedAllowed = if (shouldAllow) {
                        current.allowedNotificationPackages + appItem.packageName
                      } else {
                        current.allowedNotificationPackages - appItem.packageName
                      }
                      current.copy(allowedNotificationPackages = updatedAllowed)
                    }
                  }
                )
              }
            }
          }
        }

        // Done button
        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_save_app_filter"),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Done", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun AppFilterRow(
  app: FilterAppItem,
  isAllowed: Boolean,
  filterActive: Boolean,
  onToggle: (Boolean) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle(!isAllowed) }
      .testTag("app_row_${app.packageName}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isAllowed && filterActive) PrimaryBlue.copy(alpha = 0.08f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (app.iconBitmap != null) {
          Image(
            bitmap = app.iconBitmap,
            contentDescription = "${app.label} icon",
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(8.dp))
          )
        } else {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.NotificationsActive,
              contentDescription = null,
              tint = PrimaryBlue,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = app.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Switch(
        checked = isAllowed,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
        modifier = Modifier.testTag("switch_app_${app.packageName}")
      )
    }
  }
}
