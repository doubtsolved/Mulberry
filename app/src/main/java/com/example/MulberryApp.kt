package com.example

import android.app.Application
import com.example.data.repository.UserProfileRepository
import com.mulberry.app.analytics.MulberryAnalytics

class MulberryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Aptabase privacy-respecting background analytics
        MulberryAnalytics.init(this)

        // Seed initial student profile context
        try {
            val userProfile = UserProfileRepository.getInstance(this).userProfile.value
            MulberryAnalytics.setUserContext(
                name = userProfile.displayName,
                college = userProfile.institution,
                prof = userProfile.selectedProf
            )
        } catch (_: Exception) {
            // Fail silently
        }
    }
}
