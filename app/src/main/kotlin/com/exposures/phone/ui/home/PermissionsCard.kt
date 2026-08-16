package com.exposures.phone.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Capture needs Camera + Notifications granted, and ideally the battery-optimization exemption so
 * a message arriving while the phone is idle can still start [com.exposures.phone.capture.CaptureForegroundService]
 * promptly. Location is optional (reference photos are geotagged only if granted).
 */
@Composable
fun PermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var notificationsGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) }
    var locationGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var batteryExempted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        cameraGranted = results[Manifest.permission.CAMERA] ?: cameraGranted
        notificationsGranted = results[Manifest.permission.POST_NOTIFICATIONS] ?: notificationsGranted
        locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: locationGranted
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryExempted = isIgnoringBatteryOptimizations(context)
    }

    Card(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Capture requires:")
            PermissionRow("Camera", cameraGranted)
            PermissionRow("Notifications", notificationsGranted)
            PermissionRow("Location (optional)", locationGranted)
            PermissionRow("Battery optimization ignored", batteryExempted)

            if (!cameraGranted || !notificationsGranted || !locationGranted) {
                TextButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                        )
                    },
                ) { Text("Grant permissions") }
            }
            if (!batteryExempted) {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}"))
                        batteryLauncher.launch(intent)
                    },
                ) { Text("Ignore battery optimization") }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Text(if (granted) "✓ $label" else "✗ $label")
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
