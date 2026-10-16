package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.example.data.model.UserProfile
import com.example.util.PermissionUtils
import com.example.util.SafeFileWriter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class UserProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    @Volatile
    private var currentVaultPath: String? = null

    private val _userProfile = MutableStateFlow(loadInitialProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    companion object {
        private const val TAG = "UserProfileRepository"
        private const val PREFS_NAME = "mulberry_user_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ACADEMIC_ROLE = "academic_role"
        private const val KEY_TARGET_EXAM_OR_SUBJECT = "target_exam_or_subject"
        private const val KEY_INSTITUTION = "institution"
        private const val KEY_DAILY_PAGE_GOAL = "daily_page_goal"
        private const val KEY_SELECTED_PROF = "selected_prof"
        private const val KEY_LAST_UPDATED = "last_updated"

        // Local storage file path: /sdcard/Mulberry/.profile
        val LOCAL_PROFILE_FILE: File
            get() {
                val externalDir = Environment.getExternalStorageDirectory()
                val mulberryDir = File(externalDir, "Mulberry")
                return File(mulberryDir, ".profile")
            }

        // Fallback direct path in case of environment mount variation
        private val DIRECT_SDCARD_FILE = File("/sdcard/Mulberry/.profile")

        @Volatile
        private var instance: UserProfileRepository? = null

        fun getInstance(context: Context): UserProfileRepository {
            return instance ?: synchronized(this) {
                instance ?: UserProfileRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Loads the initial profile on app launch.
     * Prioritizes /sdcard/Mulberry/.profile if it exists and is valid,
     * otherwise falls back to SharedPreferences.
     */
    private fun loadInitialProfile(): UserProfile {
        // 1. Try reading from /sdcard/Mulberry/.profile
        val candidateFiles = listOf(LOCAL_PROFILE_FILE, DIRECT_SDCARD_FILE)
        for (candidate in candidateFiles) {
            try {
                if (candidate.exists() && candidate.isFile) {
                    val json = SafeFileWriter.readSafely(candidate)
                    if (!json.isNullOrBlank()) {
                        val parsed = gson.fromJson(json, UserProfile::class.java)
                        if (parsed != null && parsed.displayName.isNotBlank()) {
                            Log.d(TAG, "Hydrated profile from local file: ${candidate.absolutePath}")
                            saveToPrefs(parsed)
                            return parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Local profile not readable from ${candidate.absolutePath}: ${e.message}")
            }
        }

        // 2. Fallback to SharedPreferences
        return loadFromPrefs()
    }

    private fun loadFromPrefs(): UserProfile {
        return UserProfile(
            displayName = prefs.getString(KEY_DISPLAY_NAME, "Sahabul") ?: "Sahabul",
            academicRole = prefs.getString(KEY_ACADEMIC_ROLE, "Medical Student") ?: "Medical Student",
            targetExamOrSubject = prefs.getString(KEY_TARGET_EXAM_OR_SUBJECT, "MBBS 1st Year") ?: "MBBS 1st Year",
            institution = prefs.getString(KEY_INSTITUTION, "") ?: "",
            dailyPageGoal = prefs.getInt(KEY_DAILY_PAGE_GOAL, 20).coerceAtLeast(1),
            selectedProf = prefs.getString(KEY_SELECTED_PROF, "1st Prof") ?: "1st Prof",
            lastUpdated = prefs.getLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        )
    }

    fun setActiveVaultPath(path: String?) {
        currentVaultPath = path
        if (!path.isNullOrBlank()) {
            val metadataDir = File(path, ".mulberry")
            val profileFile = File(metadataDir, "user_profile.json")
            if (profileFile.exists()) {
                try {
                    val json = SafeFileWriter.readSafely(profileFile)
                    if (!json.isNullOrBlank()) {
                        val parsed = gson.fromJson(json, UserProfile::class.java)
                        if (parsed != null && parsed.displayName.isNotBlank()) {
                            saveToPrefs(parsed)
                            _userProfile.value = parsed
                            // Also sync back to /sdcard/Mulberry/.profile
                            syncProfileToLocalDisk(parsed)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read vault profile", e)
                }
            } else {
                mirrorProfileToVault(_userProfile.value, path)
            }
        }
    }

    /**
     * Updates the user profile across all layers:
     * 1. In-memory StateFlow
     * 2. SharedPreferences
     * 3. /sdcard/Mulberry/.profile (structured JSON)
     * 4. Active Vault's .mulberry/user_profile.json
     */
    suspend fun updateProfile(updated: UserProfile, vaultPath: String? = null) = withContext(Dispatchers.IO) {
        val stampedProfile = updated.copy(lastUpdated = System.currentTimeMillis())

        // 1. Persist to SharedPreferences
        saveToPrefs(stampedProfile)

        // 2. Update reactive StateFlow
        _userProfile.value = stampedProfile

        // 3. Sync to /sdcard/Mulberry/.profile
        syncProfileToLocalDisk(stampedProfile)

        // 4. Mirror to active vault if available
        val targetPath = vaultPath ?: currentVaultPath
        if (!targetPath.isNullOrBlank()) {
            mirrorProfileToVault(stampedProfile, targetPath)
        }
    }

    /**
     * Synchronously/atomically writes profile JSON to /sdcard/Mulberry/.profile
     */
    fun syncProfileToLocalDisk(profile: UserProfile) {
        if (!PermissionUtils.hasStoragePermission(context)) {
            Log.d(TAG, "Storage permission not granted yet; skipping local profile sync until permission is granted")
            return
        }
        val state = Environment.getExternalStorageState()
        if (state != Environment.MEDIA_MOUNTED) {
            Log.d(TAG, "External storage not mounted ($state); skipping local profile sync")
            return
        }
        try {
            val jsonContent = gson.toJson(profile)
            var written = writeToProfileFile(LOCAL_PROFILE_FILE, jsonContent)
            if (!written && DIRECT_SDCARD_FILE.absolutePath != LOCAL_PROFILE_FILE.absolutePath) {
                written = writeToProfileFile(DIRECT_SDCARD_FILE, jsonContent)
            }
            if (written) {
                Log.d(TAG, "Successfully synced profile to local storage")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync profile to /sdcard/Mulberry/.profile: ${e.message}")
        }
    }

    private fun writeToProfileFile(file: File, jsonContent: String): Boolean {
        return try {
            val parent = file.parentFile ?: return false
            if (!parent.exists()) {
                val created = parent.mkdirs()
                if (!created && !parent.exists()) {
                    Log.d(TAG, "Storage directory not ready yet: ${parent.absolutePath}")
                    return false
                }
            }
            if (!parent.isDirectory) {
                Log.d(TAG, "Storage directory parent is not a directory: ${parent.absolutePath}")
                return false
            }
            SafeFileWriter.writeAtomically(file, jsonContent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to ${file.absolutePath}: ${e.message}")
            false
        }
    }

    private fun saveToPrefs(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName.trim().ifEmpty { "Sahabul" })
            .putString(KEY_ACADEMIC_ROLE, profile.academicRole.trim().ifEmpty { "Medical Student" })
            .putString(KEY_TARGET_EXAM_OR_SUBJECT, profile.targetExamOrSubject.trim().ifEmpty { "MBBS 1st Year" })
            .putString(KEY_INSTITUTION, profile.institution.trim())
            .putInt(KEY_DAILY_PAGE_GOAL, profile.dailyPageGoal.coerceAtLeast(1))
            .putString(KEY_SELECTED_PROF, profile.selectedProf.ifEmpty { "1st Prof" })
            .putLong(KEY_LAST_UPDATED, profile.lastUpdated)
            .apply()
    }

    private fun mirrorProfileToVault(profile: UserProfile, vaultPath: String) {
        try {
            val root = File(vaultPath)
            if (!root.exists() || !root.isDirectory) return

            val metadataDir = File(root, ".mulberry")
            if (!metadataDir.exists()) {
                metadataDir.mkdirs()
            }

            val targetFile = File(metadataDir, "user_profile.json")
            val jsonContent = gson.toJson(profile)
            SafeFileWriter.writeAtomically(targetFile, jsonContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error mirroring profile to vault", e)
        }
    }
}
