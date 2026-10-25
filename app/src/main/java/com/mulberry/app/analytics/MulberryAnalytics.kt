package com.mulberry.app.analytics

import android.content.Context
import com.aptabase.Aptabase
import com.aptabase.InitOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MulberryAnalytics {

    private const val APP_KEY = "A-US-2780652101"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    // Cached profile attributes attached to relevant event pings
    private var cachedUserName: String = "Anonymous"
    private var cachedCollege: String = "Not Provided"
    private var cachedProfYear: String = "1st Prof"
    private var cachedGender: String = "Not Specified"

    /**
     * Call once in the Application class or main entry point.
     */
    fun init(context: Context) {
        if (isInitialized) return
        try {
            Aptabase.instance.initialize(context.applicationContext, APP_KEY, InitOptions())
            isInitialized = true
        } catch (_: Exception) {
            // Fail silently: telemetry must never interrupt user flow
        }
    }

    /**
     * Cache student metadata in memory so subsequent events inherit user context.
     */
    fun setUserContext(name: String?, college: String?, prof: String?, gender: String? = null) {
        cachedUserName = name?.takeIf { it.isNotBlank() } ?: "Anonymous"
        cachedCollege = college?.takeIf { it.isNotBlank() } ?: "Not Provided"
        cachedProfYear = prof?.takeIf { it.isNotBlank() } ?: "1st Prof"
        if (gender != null) {
            cachedGender = gender.takeIf { it.isNotBlank() } ?: "Not Specified"
        }
    }

    private fun track(eventName: String, props: Map<String, Any> = emptyMap()) {
        if (!isInitialized) return
        scope.launch {
            try {
                Aptabase.instance.trackEvent(eventName, props)
            } catch (_: Exception) {
                // Fail silently
            }
        }
    }

    // ==========================================
    // Core Event Triggers
    // ==========================================

    fun logAppOpen(activeTheme: String) {
        track("app_open", mapOf(
            "user_name" to cachedUserName,
            "college" to cachedCollege,
            "prof_year" to cachedProfYear,
            "gender" to cachedGender,
            "theme" to activeTheme
        ))
    }

    fun logProfileRegistered(name: String, college: String, prof: String, gender: String = "") {
        setUserContext(name, college, prof, gender)
        track("user_onboarded", mapOf(
            "user_name" to cachedUserName,
            "college" to cachedCollege,
            "prof_year" to cachedProfYear,
            "gender" to cachedGender
        ))
    }

    fun logProfPhaseChanged(oldProf: String, newProf: String) {
        cachedProfYear = newProf
        track("prof_phase_changed", mapOf(
            "user_name" to cachedUserName,
            "old_prof" to oldProf,
            "new_prof" to newProf
        ))
    }

    fun logBookOpened(bookId: String, bookTitle: String, subject: String, page: Int) {
        track("book_opened", mapOf(
            "user_name" to cachedUserName,
            "college" to cachedCollege,
            "prof_year" to cachedProfYear,
            "book_id" to bookId,
            "book_title" to bookTitle,
            "subject" to subject,
            "current_page" to page
        ))
    }

    fun logBookSessionCompleted(bookId: String, durationSeconds: Long, pagesRead: Int) {
        track("book_session_completed", mapOf(
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear,
            "book_id" to bookId,
            "duration_seconds" to durationSeconds,
            "pages_read" to pagesRead
        ))
    }

    fun logBookDownloadStarted(bookId: String, bookTitle: String, subject: String, sizeMb: Double) {
        track("book_download_started", mapOf(
            "user_name" to cachedUserName,
            "book_id" to bookId,
            "book_title" to bookTitle,
            "subject" to subject,
            "size_mb" to sizeMb
        ))
    }

    fun logBookDownloadCompleted(bookId: String, bookTitle: String, subject: String) {
        track("book_download_completed", mapOf(
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear,
            "book_id" to bookId,
            "book_title" to bookTitle,
            "subject" to subject
        ))
    }

    fun logSearch(query: String, resultsCount: Int) {
        track("catalog_search", mapOf(
            "query" to query.lowercase().trim(),
            "results_count" to resultsCount,
            "prof_year" to cachedProfYear,
            "is_zero_result" to (resultsCount == 0)
        ))
    }

    fun logStudyGoalCreated(subject: String, chapter: String, pageCount: Int) {
        track("study_goal_created", mapOf(
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear,
            "subject" to subject,
            "chapter" to chapter,
            "page_count" to pageCount
        ))
    }

    fun logStudyGoalCompleted(subject: String, chapter: String) {
        track("study_goal_completed", mapOf(
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear,
            "subject" to subject,
            "chapter" to chapter
        ))
    }

    fun logCatalogProfSwitched(profPhase: String) {
        track("catalog_prof_switched", mapOf(
            "prof_phase" to profPhase,
            "user_name" to cachedUserName
        ))
    }

    fun logBookDownloadFailed(bookId: String, errorCode: Int, errorType: String) {
        track("book_download_failed", mapOf(
            "book_id" to bookId,
            "error_code" to errorCode,
            "error_type" to errorType,
            "user_name" to cachedUserName
        ))
    }

    fun logBookDownloadCancelled(bookId: String, progressPercent: Int) {
        track("book_download_cancelled", mapOf(
            "book_id" to bookId,
            "progress_percent" to progressPercent,
            "user_name" to cachedUserName
        ))
    }

    fun logExternalPdfImported(fileSizeMb: Double) {
        track("external_pdf_imported", mapOf(
            "file_size_mb" to fileSizeMb,
            "user_name" to cachedUserName
        ))
    }

    fun logReaderSearchPerformed(queryLength: Int, resultCount: Int) {
        track("reader_search_performed", mapOf(
            "query_length" to queryLength,
            "result_count" to resultCount,
            "prof_year" to cachedProfYear
        ))
    }

    fun logReaderThemeChanged(themeName: String) {
        track("reader_theme_changed", mapOf(
            "theme_name" to themeName.lowercase().trim()
        ))
    }

    fun logReaderEyeRelaxToggled(enabled: Boolean, warmth: Float, dim: Float) {
        track("reader_eye_relax_toggled", mapOf(
            "enabled" to enabled,
            "warmth" to warmth,
            "dim" to dim
        ))
    }

    fun logStudySessionRescheduled(delayMinutes: Int) {
        track("study_session_rescheduled", mapOf(
            "delay_minutes" to delayMinutes,
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear
        ))
    }

    fun logWeeklyGoalUpdated(targetHours: Float) {
        track("weekly_goal_updated", mapOf(
            "target_hours" to targetHours,
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear
        ))
    }

    fun logStudyStreakShared(streakDays: Int) {
        track("study_streak_shared", mapOf(
            "streak_days" to streakDays,
            "user_name" to cachedUserName,
            "prof_year" to cachedProfYear
        ))
    }

    fun logContributionInitiated(subject: String, assetType: String) {
        track("contribution_initiated", mapOf(
            "subject" to subject,
            "asset_type" to assetType,
            "user_name" to cachedUserName,
            "college" to cachedCollege
        ))
    }

    fun logContributionUploadFailed(errorMessage: String, stage: String) {
        track("contribution_upload_failed", mapOf(
            "error_message" to errorMessage,
            "stage" to stage,
            "user_name" to cachedUserName
        ))
    }

    fun logContributionFinalized(bookSlug: String, contributorTier: String) {
        track("contribution_finalized", mapOf(
            "book_slug" to bookSlug,
            "contributor_tier" to contributorTier,
            "user_name" to cachedUserName,
            "college" to cachedCollege
        ))
    }

    fun logAppUpdateActioned(action: String) {
        track("app_update_actioned", mapOf(
            "action" to action,
            "user_name" to cachedUserName
        ))
    }
}
