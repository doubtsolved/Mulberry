package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object BookContributionManager {

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
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
