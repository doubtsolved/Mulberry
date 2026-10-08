package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * Checks whether file storage access is granted for the app.
     * On Android 11+ (API 30+), checks Environment.isExternalStorageManager().
     * On Android 10 and lower, checks READ_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks whether notification permission is enabled for the app.
     * On Android 13+ (API 33+), checks POST_NOTIFICATIONS permission.
     * On Android 12 and lower, checks if notifications are allowed system-wide for this app.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * True only if both storage and notification permissions are satisfied.
     */
    fun areAllRequiredPermissionsGranted(context: Context): Boolean {
        return hasStoragePermission(context) && hasNotificationPermission(context)
    }

    /**
     * Opens the system settings screen for this specific app.
     */
    fun openAppSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Enable Storage and Notifications for Mulberry in App Settings",
                Toast.LENGTH_LONG
            ).show()
        }.onFailure {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Creates an intent for requesting All Files Access on Android 11+.
     */
    fun createStorageAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }
}
