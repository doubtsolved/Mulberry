package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.util.AtomicFile
import com.example.data.model.UserProfile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UserProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    @Volatile
    private var currentVaultPath: String? = null

    private val _userProfile = MutableStateFlow(loadFromPrefs())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    companion object {
        private const val PREFS_NAME = "mulberry_user_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ACADEMIC_ROLE = "academic_role"
        private const val KEY_TARGET_EXAM_OR_SUBJECT = "target_exam_or_subject"
        private const val KEY_INSTITUTION = "institution"
        private const val KEY_DAILY_PAGE_GOAL = "daily_page_goal"

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

    private fun loadFromPrefs(): UserProfile {
        return UserProfile(
            displayName = prefs.getString(KEY_DISPLAY_NAME, "Sahabul") ?: "Sahabul",
            academicRole = prefs.getString(KEY_ACADEMIC_ROLE, "Medical Student") ?: "Medical Student",
            targetExamOrSubject = prefs.getString(KEY_TARGET_EXAM_OR_SUBJECT, "MBBS 1st Year") ?: "MBBS 1st Year",
            institution = prefs.getString(KEY_INSTITUTION, "") ?: "",
            dailyPageGoal = prefs.getInt(KEY_DAILY_PAGE_GOAL, 20).coerceAtLeast(1)
        )
    }

    fun setActiveVaultPath(path: String?) {
        currentVaultPath = path
        if (!path.isNullOrBlank()) {
            // Check if active vault has an existing user_profile.json
            val metadataDir = File(path, ".mulberry")
            val profileFile = File(metadataDir, "user_profile.json")
            if (profileFile.exists()) {
                try {
                    val json = profileFile.readText(Charsets.UTF_8)
                    val parsed = gson.fromJson(json, UserProfile::class.java)
                    if (parsed != null && parsed.displayName.isNotBlank()) {
                        // Hydrate in-memory state and prefs if vault has portable profile
                        saveToPrefs(parsed)
                        _userProfile.value = parsed
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Mirror current profile to the newly active vault
                mirrorProfileToVault(_userProfile.value, path)
            }
        }
    }

    suspend fun updateProfile(updated: UserProfile, vaultPath: String? = null) = withContext(Dispatchers.IO) {
        // 1. Persist to SharedPreferences
        saveToPrefs(updated)

        // 2. Update reactive StateFlow
        _userProfile.value = updated

        // 3. Mirror to active vault if available
        val targetPath = vaultPath ?: currentVaultPath
        if (!targetPath.isNullOrBlank()) {
            mirrorProfileToVault(updated, targetPath)
        }
    }

    private fun saveToPrefs(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName.trim().ifEmpty { "Sahabul" })
            .putString(KEY_ACADEMIC_ROLE, profile.academicRole.trim().ifEmpty { "Medical Student" })
            .putString(KEY_TARGET_EXAM_OR_SUBJECT, profile.targetExamOrSubject.trim().ifEmpty { "MBBS 1st Year" })
            .putString(KEY_INSTITUTION, profile.institution.trim())
            .putInt(KEY_DAILY_PAGE_GOAL, profile.dailyPageGoal.coerceAtLeast(1))
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
            val atomicFile = AtomicFile(targetFile)
            var fos: FileOutputStream? = null
            val jsonContent = gson.toJson(profile)

            try {
                fos = atomicFile.startWrite()
                fos.write(jsonContent.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(fos)
            } catch (e: Exception) {
                if (fos != null) {
                    atomicFile.failWrite(fos)
                }
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
