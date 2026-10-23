package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.repository.UserProfileRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class BookContributionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BookContributionWorker"
        const val CHANNEL_ID = "mulberry_contributions"
        const val NOTIFICATION_ID = 2001

        const val KEY_SLUG = "key_slug"
        const val KEY_TITLE = "key_title"
        const val KEY_AUTHOR = "key_author"
        const val KEY_PROF = "key_prof"
        const val KEY_SUBJECT = "key_subject"
        const val KEY_TYPE = "key_type"
        const val KEY_TOTAL_PAGES = "key_total_pages"
        const val KEY_SIZE_MB = "key_size_mb"

        private const val INIT_URL = "https://mulberry-vault.sahabulmondal2023.workers.dev/api/contribute/init"
        private const val FINALIZE_URL = "https://mulberry-vault.sahabulmondal2023.workers.dev/api/contribute/finalize"
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val slug = inputData.getString(KEY_SLUG) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: slug
        val author = inputData.getString(KEY_AUTHOR) ?: "Local Import"
        val prof = inputData.getString(KEY_PROF) ?: "1st Prof"
        val subject = inputData.getString(KEY_SUBJECT) ?: "General"
        val type = inputData.getString(KEY_TYPE) ?: "Standard Textbook"
        val totalPages = inputData.getInt(KEY_TOTAL_PAGES, 1)
        val sizeMb = inputData.getDouble(KEY_SIZE_MB, 0.0)

        val binFile = File(MulberryStorageManager.getMulberryDirectory(context), "$slug.bin")
        if (!binFile.exists() || binFile.length() == 0L) {
            Log.e(TAG, "Source .bin file not found: ${binFile.absolutePath}")
            return@withContext Result.failure()
        }

        createNotificationChannel()
        setForeground(createForegroundInfo(0, "Preparing contribution for $title..."))

        try {
            // 1. Compute SHA-256 and byte size
            val fileSizeBytes = binFile.length()
            val sha256Hex = computeSha256(binFile)

            // 2. Fetch UserProfile & Android ID
            val userProfile = UserProfileRepository.getInstance(context).userProfile.value
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            val vipCode = userProfile.vipCode ?: ""

            // 3. Preflight & Rate check: POST /api/contribute/init
            val initPayload = JsonObject().apply {
                addProperty("slug", slug)
                addProperty("sha256", sha256Hex)
                addProperty("sizeBytes", fileSizeBytes)
            }

            val initRequest = Request.Builder()
                .url(INIT_URL)
                .addHeader("X-Device-Id", androidId)
                .addHeader("X-Vip-Code", vipCode)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), initPayload.toString()))
                .build()

            val initResponse = httpClient.newCall(initRequest).execute()
            val initCode = initResponse.code
            val initBody = initResponse.body?.string() ?: ""

            if (initCode == 429) {
                val limit = userProfile.dailyUploadLimit
                val msg = "Daily upload limit reached ($limit/$limit files today). Try again tomorrow or enter a Contributor Code in Profile."
                showToastOnMain(msg)
                notifyFailed(title, msg)
                return@withContext Result.failure()
            }

            if (!initResponse.isSuccessful || initBody.isBlank()) {
                val errorMsg = "Contribution init failed (HTTP $initCode): $initBody"
                Log.e(TAG, errorMsg)
                notifyFailed(title, "Server error during initialization ($initCode)")
                return@withContext Result.retry()
            }

            val initJson = gson.fromJson(initBody, JsonObject::class.java)

            // 4. Extract presigned S3 upload URL and headers
            val lfsObj = initJson.getAsJsonObject("lfsObject")
            val actions = lfsObj?.getAsJsonObject("actions")
            val uploadAction = actions?.getAsJsonObject("upload")
            val uploadHref = uploadAction?.get("href")?.asString

            if (uploadHref.isNullOrBlank()) {
                Log.e(TAG, "No S3 upload URL returned in lfsObject: $initBody")
                notifyFailed(title, "Invalid upload destination returned by server")
                return@withContext Result.failure()
            }

            val headerMap = mutableMapOf<String, String>()
            uploadAction.getAsJsonObject("header")?.let { headersObj ->
                for ((k, v) in headersObj.entrySet()) {
                    if (v.isJsonPrimitive) headerMap[k] = v.asString
                }
            }

            // 5. Direct S3 Upload with 8KB buffer streaming and progress
            val s3RequestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()

                override fun contentLength() = fileSizeBytes

                override fun writeTo(sink: BufferedSink) {
                    val source = binFile.source()
                    val buffer = okio.Buffer()
                    var totalBytesRead = 0L
                    var lastReportTime = 0L

                    while (true) {
                        val readCount = source.read(buffer, 8192L)
                        if (readCount == -1L) break
                        sink.write(buffer, readCount)
                        totalBytesRead += readCount

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 500L || totalBytesRead == fileSizeBytes) {
                            lastReportTime = now
                            val progressPercent = ((totalBytesRead.toFloat() / fileSizeBytes.toFloat()) * 100f).toInt().coerceIn(0, 100)
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildNotification(progressPercent, "Uploading $title: $progressPercent%")
                            )
                            setProgressAsync(workDataOf("progress" to progressPercent))
                        }
                    }
                }
            }

            val s3RequestBuilder = Request.Builder()
                .url(uploadHref)
                .put(s3RequestBody)

            for ((k, v) in headerMap) {
                s3RequestBuilder.addHeader(k, v)
            }

            val s3Response = httpClient.newCall(s3RequestBuilder.build()).execute()
            if (!s3Response.isSuccessful) {
                val s3Err = "S3 direct upload failed with HTTP ${s3Response.code}"
                Log.e(TAG, s3Err)
                notifyFailed(title, "Upload failed to S3 (${s3Response.code})")
                return@withContext Result.retry()
            }

            // 6. Read cover image as Base64 if available
            val coverFile = File(MulberryStorageManager.getMulberryDirectory(context), ".covers/$slug.webp")
            val coverBase64 = if (coverFile.exists() && coverFile.length() > 0L) {
                val bytes = coverFile.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""

            // 7. Finalize Quarantine Commit: POST /api/contribute/finalize
            val metadataObj = JsonObject().apply {
                addProperty("title", title)
                addProperty("author", author)
                addProperty("prof", prof)
                addProperty("subject", subject)
                addProperty("type", type)
                addProperty("totalPages", totalPages)
                addProperty("sizeMb", sizeMb)
                addProperty("uploader", userProfile.displayName)
            }

            val finalizePayload = JsonObject().apply {
                addProperty("slug", slug)
                addProperty("sha256", sha256Hex)
                addProperty("sizeBytes", fileSizeBytes)
                addProperty("coverBase64", coverBase64)
                add("metadata", metadataObj)
            }

            val finalizeRequest = Request.Builder()
                .url(FINALIZE_URL)
                .addHeader("X-Device-Id", androidId)
                .addHeader("X-Vip-Code", vipCode)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), finalizePayload.toString()))
                .build()

            val finalizeResponse = httpClient.newCall(finalizeRequest).execute()
            if (!finalizeResponse.isSuccessful) {
                Log.w(TAG, "Finalize failed (HTTP ${finalizeResponse.code})")
            }

            // 8. Update contributedStatus = "PENDING" in downloaded_external.json
            MulberryStorageManager.updateExternalContributedStatus(context, slug, "PENDING")

            showToastOnMain("Thank you! Your book was submitted for review.")
            notifySuccess(title)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in contribution pipeline: ${e.message}", e)
            notifyFailed(title, "Contribution failed: ${e.localizedMessage}")
            Result.failure()
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(32768)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Community Book Contributions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows upload progress for textbooks submitted to Mulberry Library"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = buildNotification(progress, message)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(progress: Int, message: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Mulberry Community Library")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun notifySuccess(title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Book Contribution Submitted")
            .setContentText("\"$title\" was uploaded and submitted for review. Thank you!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun notifyFailed(title: String, reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Upload Paused")
            .setContentText("$title: $reason")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun showToastOnMain(text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}
