package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.service.MulberryFirebaseMessagingService

/**
 * Composable effect that requests POST_NOTIFICATIONS permission on Android 13+ (API 33+)
 * and verifies subscription to broadcast topics ("announcements" and "all_users").
 */
@Composable
fun RequestNotificationPermissionEffect() {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            MulberryFirebaseMessagingService.subscribeToStandardTopics()
        }
    }

    LaunchedEffect(Unit) {
        // Always ensure topic subscriptions on launch
        MulberryFirebaseMessagingService.subscribeToStandardTopics()

        // Check runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
