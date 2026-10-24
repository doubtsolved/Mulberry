package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.annotations.SerializedName
import com.mulberry.app.analytics.MulberryAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class InitResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("needsUpload") val needsUpload: Boolean = true,
    @SerializedName("uploadUrl") val uploadUrl: String? = null,
    @SerializedName("uploadHeaders") val uploadHeaders: Map<String, String>? = null,
    @SerializedName("oid") val oid: String? = null,
    @SerializedName("sizeBytes") val sizeBytes: Long? = null,
    @SerializedName("tier") val tier: String? = null
)

sealed class ContributionUploadState {
    data object Idle : ContributionUploadState()
    data class Uploading(
        val slug: String,
        val bookTitle: String,
        val stage: String, // "Preparing SHA-256...", "Uploading to Vault...", "Finalizing..."
        val progressPercent: Int,
        val bytesUploaded: Long,
        val totalBytes: Long
    ) : ContributionUploadState()
    data class Success(val slug: String, val bookTitle: String) : ContributionUploadState()
    data class Error(val slug: String, val bookTitle: String, val errorMessage: String) : ContributionUploadState()
}

object BookContributionManager {

    // Strictly system DNS - prevent custom DNS-over-HTTPS or interceptors from altering target URLs/paths
    val customDns: Dns = Dns.SYSTEM

    // Retain appDns alias for compatibility across the codebase
    val appDns: Dns = customDns

    // HttpClient configured for Cloudflare Worker & AWS S3 streaming upload without stale pooled sockets
    val contributionHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(Dns.SYSTEM)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS)) // Do NOT reuse stale sockets across long uploads
        .build()

    private val _uploadState = MutableStateFlow<ContributionUploadState>(ContributionUploadState.Idle)
    val uploadState: StateFlow<ContributionUploadState> = _uploadState.asStateFlow()

    fun updateUploadState(state: ContributionUploadState) {
        _uploadState.value = state
    }

    fun updateState(state: ContributionUploadState) = updateUploadState(state)

    fun dismissUploadState() {
        _uploadState.value = ContributionUploadState.Idle
    }

    fun cancelContribution(context: Context, slug: String) {
        try {
            val wm = WorkManager.getInstance(context.applicationContext)
            wm.cancelUniqueWork("contribution_$slug")
            wm.cancelUniqueWork("contrib_$slug")
        } catch (_: Exception) {}
        _uploadState.value = ContributionUploadState.Idle
    }

    /**
     * Enqueues a background upload of an encrypted book to Hugging Face LFS S3
     * via Android WorkManager with progress notifications.
     */
    fun enqueueContribution(
        context: Context,
        slug: String,
        title: String,
        author: String = "Local Import",
        prof: String = "1st Prof",
        subject: String = "Anatomy",
        type: String = "Standard Textbook",
        totalPages: Int = 1,
        sizeMb: Double = 0.0
    ) {
        MulberryAnalytics.logContributionInitiated(
            subject = subject,
            assetType = type
        )
        val totalBytes = (sizeMb * 1024.0 * 1024.0).toLong().coerceAtLeast(0L)
        _uploadState.value = ContributionUploadState.Uploading(
            slug = slug,
            bookTitle = title,
            stage = "Enqueued for library upload...",
            progressPercent = 0,
            bytesUploaded = 0L,
            totalBytes = totalBytes
        )

        val inputData = workDataOf(
            BookContributionWorker.KEY_SLUG to slug,
            BookContributionWorker.KEY_TITLE to title,
            BookContributionWorker.KEY_AUTHOR to author,
            BookContributionWorker.KEY_PROF to prof,
            BookContributionWorker.KEY_SUBJECT to subject,
            BookContributionWorker.KEY_TYPE to type,
            BookContributionWorker.KEY_TOTAL_PAGES to totalPages,
            BookContributionWorker.KEY_SIZE_MB to sizeMb
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BookContributionWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("contribution_$slug")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "contribution_$slug",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
