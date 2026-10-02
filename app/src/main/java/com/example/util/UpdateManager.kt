package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

object UpdateManager {
    private const val TAG = "UpdateManager"

    // Configured GitHub repository coordinates
    const val GITHUB_OWNER = "doubtsolved"
    const val GITHUB_REPO = "Mulberry"
    const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Checks GitHub API for the latest published release.
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(RELEASES_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Mulberry-Android-App")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.Error("No releases found on GitHub repository ($GITHUB_OWNER/$GITHUB_REPO).")
            }
            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error("GitHub API returned error code: $responseCode")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).ifEmpty { tagName }
            val body = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            // Parse assets looking for an APK file
            val assetsArray = json.optJSONArray("assets")
            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L

            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            // If no individual apk asset found, fallback to html_url
            if (apkUrl.isBlank()) {
                apkUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases")
                apkName = "Mulberry.apk"
            }

            val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val isNewer = isNewerVersion(remoteVersion, currentVersion)

            val releaseInfo = ReleaseInfo(
                tagName = tagName,
                version = remoteVersion,
                title = releaseTitle,
                changelog = body,
                publishedAt = publishedAt,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize
            )

            if (isNewer) {
                UpdateCheckResult.UpdateAvailable(releaseInfo, currentVersion)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}", e)
            UpdateCheckResult.Error(e.localizedMessage ?: "Failed to connect to GitHub")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compare two semantic version strings (e.g. "1.2.0" vs "1.0.0" or "1.2" vs "1.0").
     * Returns true if remote is strictly greater than current.
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val cleanRemote = remote.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = current.removePrefix("v").removePrefix("V").trim()

            if (cleanRemote == cleanCurrent) return false

            val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing versions: remote=$remote, current=$current", e)
            return false
        }
    }

    /**
     * Downloads APK directly to app's cache directory with progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (DownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "Mulberry-${release.version.ifEmpty { "latest" }}.apk")

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            onProgress(DownloadState.Downloading(0, 0L, release.apkSizeBytes, 0L))

            var targetUrl = release.apkDownloadUrl
            var redirects = 0
            while (redirects < 5) {
                val url = URL(targetUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mulberry-Android-App")
                    connectTimeout = 15_000
                    readTimeout = 30_000
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        targetUrl = location
                        connection.disconnect()
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: throw IllegalStateException("Could not establish connection")
            val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else release.apkSizeBytes
            inputStream = conn.inputStream
            outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!coroutineContext.isActive) {
                    apkFile.delete()
                    onProgress(DownloadState.Idle)
                    return@withContext null
                }

                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateMs
                if (elapsed >= 300) {
                    currentSpeed = (bytesSinceLastUpdate * 1000L) / elapsed.coerceAtLeast(1L)
                    val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                    onProgress(DownloadState.Downloading(percent, totalRead, totalBytes, currentSpeed))
                    lastUpdateMs = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            val finalPercent = 100
            onProgress(DownloadState.Downloading(finalPercent, totalRead, totalBytes, 0L))

            val completedState = DownloadState.Completed(apkFile, release)
            onProgress(completedState)
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            apkFile.delete()
            onProgress(DownloadState.Error(e.localizedMessage ?: "Download failed"))
            null
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * Launches Android Package Installer for the downloaded APK using FileProvider.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
                return false
            }

            // Check Unknown Sources permission on Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return false
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            return false
        }
    }
}
