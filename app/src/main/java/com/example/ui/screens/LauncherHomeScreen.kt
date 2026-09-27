package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LauncherApp(val label: String, val packageName: String, val intent: Intent)

@Composable
fun LauncherHomeScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val apps = remember(context.packageManager) {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.packageManager.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.queryIntentActivities(query, 0)
    }
    resolved.mapNotNull { info ->
      val launch = context.packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
        ?: return@mapNotNull null
      LauncherApp(
        label = info.loadLabel(context.packageManager).toString(),
        packageName = info.activityInfo.packageName,
        intent = launch
      )
    }.filter { it.packageName != context.packageName }
      .distinctBy { it.packageName }
      .sortedBy { it.label.lowercase(Locale.getDefault()) }
  }
  val date = remember {
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
  }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("OpenCapsule", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Light
      )
    }
    Text("Your apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    if (apps.isEmpty()) {
      Text("No launchable apps were found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(apps, key = { it.packageName }) { app ->
          Card(
            modifier = Modifier.clickable {
              runCatching { context.startActivity(app.intent) }
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(app.label.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text(
                app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                minLines = 2
              )
            }
          }
        }
      }
    }
  }
}
