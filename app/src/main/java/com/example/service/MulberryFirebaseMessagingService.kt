package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MulberryFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MulberryFCM"
        const val CHANNEL_ID = "announcements_channel"
        const val CHANNEL_NAME = "Mulberry Announcements"
        private val notificationIdGenerator = AtomicInteger(1000)

        /**
         * Ensures subscription to standard broadcast topics.
         */
        fun subscribeToStandardTopics() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to 'announcements' topic successfully")
                        } else {
                            Log.w(TAG, "Failed subscribing to 'announcements' topic", task.exception)
                        }
                    }
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to 'all_users' topic successfully")
                        } else {
                            Log.w(TAG, "Failed subscribing to 'all_users' topic", task.exception)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error initiating FCM topic subscriptions: ${e.message}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM registration token received: $token")
        subscribeToStandardTopics()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        // 1. Resolve title & body from notification payload or custom data
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""

        // 2. Resolve image URL from notification or data fields
        val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            ?: remoteMessage.data["image_url"]
            ?: remoteMessage.data["image"]
            ?: remoteMessage.data["attachment"]

        // 3. Resiliently download bitmap if image URL present (4-second timeout)
        val bitmap = if (!imageUrl.isNullOrBlank()) {
            downloadImageBitmapWithTimeout(imageUrl)
        } else {
            null
        }

        // 4. Ensure High-Importance notification channel on Android 8.0+ (API 26+)
        createNotificationChannelIfNeeded()

        // 5. Construct pending intent targeting MainActivity, forwarding all payload data extras
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((key, value) in remoteMessage.data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 6. Build Notification with BigPictureStyle or BigTextStyle fallback
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            val bigPictureStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .setBigContentTitle(title)
                .setSummaryText(body)
            notificationBuilder.setStyle(bigPictureStyle)
            notificationBuilder.setLargeIcon(bitmap)
        } else {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
            )
        }

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            val notificationId = notificationIdGenerator.incrementAndGet()
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted to post notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display FCM notification: ${e.message}", e)
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Broadcast announcements and library updates for Mulberry students"
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Downloads an image from the provided URL with a strict 4-second network timeout.
     */
    private fun downloadImageBitmapWithTimeout(imageUrl: String): Bitmap? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mulberry-Android-FCM")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    Log.w(TAG, "Image download HTTP ${response.code} for $imageUrl")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Timed out or failed downloading image from $imageUrl: ${e.message}")
            null
        }
    }
}
