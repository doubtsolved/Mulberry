package com.example.util

import java.io.File

/**
 * Metadata representation of a release parsed from GitHub Releases API.
 */
data class ReleaseInfo(
    val tagName: String,
    val version: String,
    val title: String,
    val changelog: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
) {
    val formattedSize: String
        get() {
            if (apkSizeBytes <= 0) return ""
            val mb = apkSizeBytes / (1024.0 * 1024.0)
            return "%.1f MB".format(mb)
        }
}

/**
 * Result state returned by UpdateManager check.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Download progress and status lifecycle.
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : DownloadState() {
        val downloadedMb: String get() = "%.1f".format(bytesDownloaded / (1024.0 * 1024.0))
        val totalMb: String get() = if (totalBytes > 0) "%.1f MB".format(totalBytes / (1024.0 * 1024.0)) else "--"
        val speedFormatted: String
            get() {
                val mbps = speedBytesPerSec / (1024.0 * 1024.0)
                return if (mbps >= 0.1) "%.1f MB/s".format(mbps) else "${speedBytesPerSec / 1024} KB/s"
            }
    }
    data class Completed(val apkFile: File, val release: ReleaseInfo) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
