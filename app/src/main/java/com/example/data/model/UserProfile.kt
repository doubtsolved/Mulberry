package com.example.data.model

data class UserProfile(
    val displayName: String = "Dr. Sharma",
    val academicRole: String = "Medical Student",
    val targetExamOrSubject: String = "MBBS 1st Year",
    val institution: String = "",
    val gender: String = "",
    val dailyPageGoal: Int = 20,
    val selectedProf: String = "1st Prof",
    val lastUpdated: Long = System.currentTimeMillis(),
    val vipCode: String? = null,
    val vipTier: String = "STANDARD",
    val vipBadgeLabel: String = "Scholar",
    val dailyUploadLimit: Int = 3
)
