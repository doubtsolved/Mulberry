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
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.repository.UserProfileRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mulberry.app.analytics.MulberryAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
        private const val TAG = "MulberryUpload"
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

    private val contributionHttpClient: OkHttpClient = BookContributionManager.contributionHttpClient

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
            BookContributionManager.updateUploadState(
                ContributionUploadState.Error(slug, title, "File not found in vault")
            )
            return@withContext Result.failure()
        }

        createNotificationChannel()
        try {
            setForeground(createForegroundInfo(0, "Preparing contribution for $title..."))
        } catch (e: Exception) {
            Log.w(TAG, "setForeground non-fatal exception: ${e.message}")
        }
        safeNotify(NOTIFICATION_ID, buildNotification(0, "Preparing contribution for $title..."))

        val fileSizeBytes = binFile.length()
        BookContributionManager.updateUploadState(
            ContributionUploadState.Uploading(
                slug = slug,
                bookTitle = title,
                stage = "Preparing SHA-256 & Vault...",
                progressPercent = 5,
                bytesUploaded = 0L,
                totalBytes = fileSizeBytes
            )
        )

        try {
            // 1. Compute SHA-256 and byte size
            val sha256Hex = computeSha256(binFile)
            Log.d(TAG, "Computed SHA-256 for $slug: $sha256Hex, size: $fileSizeBytes bytes")

            // 2. Fetch UserProfile & Android ID
            val userProfile = UserProfileRepository.getInstance(context).userProfile.value
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            val vipCode = userProfile.vipCode ?: ""

            // 3. Preflight & Rate check: POST /api/contribute/init
            Log.d(TAG, "Step 1: Calling /api/contribute/init...")
            BookContributionManager.updateUploadState(
                ContributionUploadState.Uploading(
                    slug = slug,
                    bookTitle = title,
                    stage = "Connecting to Mulberry Cloud...",
                    progressPercent = 10,
                    bytesUploaded = 0L,
                    totalBytes = fileSizeBytes
                )
            )

            val initPayload = JsonObject().apply {
                addProperty("slug", slug)
                addProperty("sha256", sha256Hex)
                addProperty("sizeBytes", fileSizeBytes)
            }

            val initJsonBody = initPayload.toString()
            val initRequest = Request.Builder()
                .url(INIT_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "close")
                .addHeader("X-Device-Id", deviceId)
                .addHeader("X-Vip-Code", vipCode)
                .post(initJsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val (initCode, initBody, initSuccessful) = contributionHttpClient.newCall(initRequest).execute().use { response ->
                Triple(response.code, response.body?.string() ?: "", response.isSuccessful)
            }
            Log.d(TAG, "Step 1 Init Response Code: $initCode, body: $initBody")

            if (initCode == 429) {
                val limit = userProfile.dailyUploadLimit
                val msg = "Daily upload limit reached ($limit/$limit files today). Try again tomorrow or enter a Contributor Code in Profile."
                showToastOnMain(msg)
                notifyFailed(title, msg)
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Error(slug, title, msg)
                )
                return@withContext Result.failure()
            }

            if (initCode in 400..499) {
                val errorMsg = "Contribution preflight failed (HTTP $initCode): $initBody"
                Log.e(TAG, errorMsg)
                showToastOnMain("Upload rejected by server ($initCode)")
                notifyFailed(title, "Upload rejected ($initCode)")
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Error(slug, title, errorMsg)
                )
                return@withContext Result.failure()
            }

            if (!initSuccessful || initBody.isBlank()) {
                val errorMsg = "Contribution init failed (HTTP $initCode): $initBody"
                Log.e(TAG, errorMsg)
                MulberryAnalytics.logContributionUploadFailed(errorMsg, "init")
                notifyFailed(title, errorMsg)
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Error(slug, title, errorMsg)
                )
                return@withContext Result.failure()
            }

            val initDto = try {
                gson.fromJson(initBody, InitResponseDto::class.java)
            } catch (e: Exception) {
                val errorMsg = "Failed to parse init response: ${e.message}"
                Log.e(TAG, errorMsg, e)
                notifyFailed(title, errorMsg)
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Error(slug, title, errorMsg)
                )
                return@withContext Result.failure()
            }

            // 4. Stream payload to S3 only if server requests it (needsUpload == true)
            if (initDto.needsUpload) {
                val uploadUrl = initDto.uploadUrl
                if (uploadUrl.isNullOrBlank()) {
                    val errorMsg = "Upload URL missing from server response: $initBody"
                    Log.e(TAG, errorMsg)
                    notifyFailed(title, "Server provided empty upload URL")
                    BookContributionManager.updateUploadState(
                        ContributionUploadState.Error(slug, title, errorMsg)
                    )
                    return@withContext Result.failure()
                }

                Log.d(TAG, "Step 2: Uploading file bytes to S3: $uploadUrl")
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Uploading(
                        slug = slug,
                        bookTitle = title,
                        stage = "Uploading to Community Library...",
                        progressPercent = 10,
                        bytesUploaded = 0L,
                        totalBytes = fileSizeBytes
                    )
                )

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
                            if (now - lastReportTime > 250L || totalBytesRead == fileSizeBytes) {
                                lastReportTime = now
                                val progressPercent = ((totalBytesRead.toFloat() / fileSizeBytes.toFloat()) * 100f).toInt().coerceIn(0, 100)
                                safeNotify(
                                    NOTIFICATION_ID,
                                    buildNotification(progressPercent, "Uploading $title: $progressPercent%")
                                )
                                setProgressAsync(workDataOf("progress" to progressPercent))
                                BookContributionManager.updateUploadState(
                                    ContributionUploadState.Uploading(
                                        slug = slug,
                                        bookTitle = title,
                                        stage = "Uploading to Community Library...",
                                        progressPercent = progressPercent,
                                        bytesUploaded = totalBytesRead,
                                        totalBytes = fileSizeBytes
                                    )
                                )
                            }
                        }
                    }
                }

                val s3RequestBuilder = Request.Builder()
                    .url(uploadUrl)
                    .put(s3RequestBody)
                    .apply {
                        initDto.uploadHeaders?.forEach { (k, v) ->
                            addHeader(k, v)
                        }
                    }

                val (s3Code, s3Error, s3Successful) = contributionHttpClient.newCall(s3RequestBuilder.build()).execute().use { response ->
                    val err = if (!response.isSuccessful) response.body?.string() ?: "Unknown S3 error" else ""
                    Triple(response.code, err, response.isSuccessful)
                }
                Log.d(TAG, "Step 2 S3 Response Code: $s3Code")
                if (!s3Successful) {
                    val errorMsg = "S3 Upload Failed [$s3Code]: $s3Error"
                    Log.e(TAG, errorMsg)
                    MulberryAnalytics.logContributionUploadFailed(errorMsg, "s3_upload")
                    notifyFailed(title, "S3 upload failed ($s3Code)")
                    BookContributionManager.updateUploadState(
                        ContributionUploadState.Error(slug, title, errorMsg)
                    )
                    return@withContext Result.failure()
                }
            } else {
                Log.d(TAG, "Step 2 Bypassed: Asset already cached on Hugging Face (oid=${initDto.oid}, tier=${initDto.tier}). Skipping binary upload.")
                safeNotify(
                    NOTIFICATION_ID,
                    buildNotification(95, "Asset verified in Vault. Finalizing review...")
                )
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Uploading(
                        slug = slug,
                        bookTitle = title,
                        stage = "Asset already verified in Vault, finalizing...",
                        progressPercent = 95,
                        bytesUploaded = fileSizeBytes,
                        totalBytes = fileSizeBytes
                    )
                )
            }

            // 5. Read cover image as Base64 if available
            val coverFile = File(MulberryStorageManager.getMulberryDirectory(context), ".covers/$slug.webp")
            val coverBase64 = if (coverFile.exists() && coverFile.length() > 0L) {
                val bytes = coverFile.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""

            // 6. Finalize Review Submission: POST /api/contribute/finalize
            Log.d(TAG, "Step 3: Calling /api/contribute/finalize...")
            BookContributionManager.updateUploadState(
                ContributionUploadState.Uploading(
                    slug = slug,
                    bookTitle = title,
                    stage = "Finalizing review submission...",
                    progressPercent = 98,
                    bytesUploaded = fileSizeBytes,
                    totalBytes = fileSizeBytes
                )
            )

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

            val finalizeJsonBody = finalizePayload.toString()
            val finalizeRequest = Request.Builder()
                .url(FINALIZE_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "close") // Forces fresh handshake; prevents Cloudflare edge 408
                .addHeader("X-Device-Id", deviceId)
                .addHeader("X-Vip-Code", vipCode)
                .post(finalizeJsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val (finalizeCode, finalizeBody, finalizeSuccessful) = contributionHttpClient.newCall(finalizeRequest).execute().use { response ->
                Triple(response.code, response.body?.string() ?: "", response.isSuccessful)
            }
            Log.d(TAG, "Step 3 Finalize Response Code: $finalizeCode, body: $finalizeBody")
            if (!finalizeSuccessful) {
                val errorMsg = "Finalize failed (HTTP $finalizeCode): $finalizeBody"
                Log.e(TAG, errorMsg)
                MulberryAnalytics.logContributionUploadFailed(errorMsg, "finalize")
                notifyFailed(title, "Finalize failed ($finalizeCode)")
                BookContributionManager.updateUploadState(
                    ContributionUploadState.Error(slug, title, errorMsg)
                )
                return@withContext Result.failure()
            }

            // 7. Update contributedStatus = "PENDING" in downloaded_external.json
            MulberryStorageManager.updateExternalContributedStatus(context, slug, "PENDING")

            val contributorTier = userProfile.vipBadgeLabel.ifBlank { userProfile.vipTier }.lowercase()
            MulberryAnalytics.logContributionFinalized(
                bookSlug = slug,
                contributorTier = contributorTier
            )

            showToastOnMain("Thank you! Your book was submitted for review.")
            notifySuccess(title)
            BookContributionManager.updateUploadState(
                ContributionUploadState.Success(slug, title)
            )
            Result.success()
        } catch (e: Exception) {
            val failReason = e.localizedMessage ?: "Unknown error during upload"
            Log.e(TAG, "CRITICAL CONTRIBUTION FAILURE: ${e.message}", e)
            MulberryAnalytics.logContributionUploadFailed(failReason, "unknown")
            notifyFailed(title, "Contribution failed: $failReason")
            BookContributionManager.updateUploadState(
                ContributionUploadState.Error(
                    slug = slug,
                    bookTitle = title,
                    errorMessage = failReason
                )
            )
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

    private fun safeNotify(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }

    private fun notifySuccess(title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Book Contribution Submitted")
            .setContentText("\"$title\" was uploaded and submitted for review. Thank you!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        safeNotify(NOTIFICATION_ID + 1, notification)
    }

    private fun notifyFailed(title: String, reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Upload Paused")
            .setContentText("$title: $reason")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        safeNotify(NOTIFICATION_ID + 2, notification)
    }

    private fun showToastOnMain(text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}
