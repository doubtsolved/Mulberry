package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private data class VipCodeResult(
    val code: String,
    val tier: String,
    val badgeLabel: String,
    val dailyLimit: Int
)

private suspend fun verifyVipCodeNetwork(code: String): Result<VipCodeResult> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .dns(com.example.util.BookContributionManager.appDns)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val jsonPayload = JSONObject().apply {
            put("code", code.trim())
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://mulberry-vault.sahabulmondal2023.workers.dev/api/verify-code")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val resJson = JSONObject(responseString)
            val tier = resJson.optString("tier", resJson.optString("vipTier", "STANDARD")).uppercase()
            val (mappedTier, defaultBadge, defaultLimit) = when (tier) {
                "CORE_VIP" -> Triple("CORE_VIP", "Core Architect", 9999)
                "AMBASSADOR" -> Triple("AMBASSADOR", "Verified Contributor", 25)
                else -> Triple("STANDARD", "Scholar", 3)
            }
            val badgeLabel = resJson.optString("badgeLabel", resJson.optString("vipBadgeLabel", defaultBadge))
            val dailyLimit = if (resJson.has("dailyUploadLimit")) resJson.getInt("dailyUploadLimit") else defaultLimit
            Result.success(VipCodeResult(code.trim(), mappedTier, badgeLabel, dailyLimit))
        } else {
            val errMsg = try {
                JSONObject(responseString).optString("error", "Invalid or expired access code")
            } catch (_: Exception) {
                "Verification failed (HTTP ${response.code})"
            }
            Result.failure(Exception(errMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

enum class ProfileModalStep {
    VIEW,
    EDIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileBottomSheet(
    userProfile: UserProfile,
    onDismissRequest: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    initialStep: ProfileModalStep = ProfileModalStep.VIEW,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(initialStep) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState == ProfileModalStep.EDIT) {
                    (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { width -> -width / 3 } + fadeOut())
                } else {
                    (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { width -> width / 3 } + fadeOut())
                }
            },
            label = "ProfileStepTransition"
        ) { step ->
            when (step) {
                ProfileModalStep.VIEW -> {
                    ProfileOverviewContent(
                        userProfile = userProfile,
                        onSaveProfile = onSaveProfile,
                        onEditClick = { currentStep = ProfileModalStep.EDIT },
                        onDismiss = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        }
                    )
                }
                ProfileModalStep.EDIT -> {
                    ProfileEditContent(
                        userProfile = userProfile,
                        onBack = { currentStep = ProfileModalStep.VIEW },
                        onSave = { updated ->
                            onSaveProfile(updated)
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        },
                        onDismiss = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Stage 1: Streamlined Profile Overview.
 * Displays only Display Name, MBBS Professional Year, and Institution.
 * Completely free of daily goals, academic role tags, or technical disk sync indicators.
 */
@Composable
private fun ProfileOverviewContent(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalMulberryColors.current
    var isVipExpanded by remember { mutableStateOf(false) }
    var accessCodeInput by remember(userProfile.vipCode) { mutableStateOf(userProfile.vipCode ?: "") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf<String?>(null) }

    val curriculum = remember(userProfile.selectedProf) {
        MBBS_CURRICULUM.find { it.id.equals(userProfile.selectedProf, ignoreCase = true) }
            ?: MBBS_CURRICULUM[0]
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top modal bar with title & close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Profile",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = colors.textPrimary
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("profile_dismiss_button")
            ) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = "Dismiss",
                    tint = colors.textSecondary
                )
            }
        }

        // User identity summary: Avatar, Name, and MBBS Prof badge
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.example.ui.components.MulberryProfileAvatar(
                userProfile = userProfile,
                size = 72.dp,
                cornerRadius = 24.dp,
                fontSize = 26.sp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = userProfile.displayName,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = colors.primary.copy(alpha = 0.10f),
                        border = BorderStroke(0.5.dp, colors.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = curriculum.title,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    if (!userProfile.vipBadgeLabel.isNullOrBlank() && userProfile.vipTier != "STANDARD") {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.primary.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, colors.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Star20Filled,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = userProfile.vipBadgeLabel,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details list: strictly MBBS Professional Year and Institution
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileInfoTile(
                icon = FluentIcons.Bookmark24Regular,
                title = "MBBS Professional Year",
                value = curriculum.title
            )

            ProfileInfoTile(
                icon = FluentIcons.Building24Regular,
                title = "Institution / Medical College",
                value = if (userProfile.institution.isNotBlank()) userProfile.institution else "Not Specified"
            )
        }

        // Special Contributor Access Code Expandable Tile
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceCard,
            border = BorderStroke(
                1.dp,
                if (userProfile.vipTier != "STANDARD") colors.primary.copy(alpha = 0.35f) else colors.borderSubtle
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isVipExpanded = !isVipExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = FluentIcons.Lock24Regular,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Special Contributor Access Code",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = colors.textPrimary
                            )
                            val statusSubtitle = if (userProfile.vipTier != "STANDARD") {
                                "${userProfile.vipBadgeLabel} • ${if (userProfile.dailyUploadLimit >= 1000) "Unlimited" else "${userProfile.dailyUploadLimit}"} uploads/day"
                            } else {
                                "Enter code to unlock VIP contributor capabilities"
                            }
                            Text(
                                text = statusSubtitle,
                                fontFamily = InterFamily,
                                fontSize = 11.5.sp,
                                color = if (userProfile.vipTier != "STANDARD") colors.primary else colors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isVipExpanded) FluentIcons.ChevronUp24Regular else FluentIcons.ChevronDown24Regular,
                        contentDescription = if (isVipExpanded) "Collapse" else "Expand",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isVipExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = accessCodeInput,
                            onValueChange = {
                                accessCodeInput = it
                                verificationError = null
                            },
                            label = { Text("Access Code", fontFamily = InterFamily, fontSize = 12.sp) },
                            placeholder = { Text("e.g. VIP-CORE-2026", fontFamily = InterFamily, fontSize = 12.sp) },
                            singleLine = true,
                            enabled = !isVerifying,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedLabelColor = colors.primary,
                                cursorColor = colors.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vip_access_code_input")
                        )

                        if (!verificationError.isNullOrBlank()) {
                            Text(
                                text = verificationError!!,
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.accentDanger
                            )
                        }

                        Button(
                            onClick = {
                                val trimmed = accessCodeInput.trim()
                                if (trimmed.isBlank()) {
                                    verificationError = "Please enter an access code"
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isVerifying = true
                                    verificationError = null
                                    val result = verifyVipCodeNetwork(trimmed)
                                    isVerifying = false
                                    result.onSuccess { vip ->
                                        val updated = userProfile.copy(
                                            vipCode = vip.code,
                                            vipTier = vip.tier,
                                            vipBadgeLabel = vip.badgeLabel,
                                            dailyUploadLimit = vip.dailyLimit
                                        )
                                        onSaveProfile(updated)
                                        Toast.makeText(context, "Verified as ${vip.badgeLabel}!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        verificationError = err.message ?: "Invalid access code"
                                        Toast.makeText(context, err.message ?: "Verification failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isVerifying && accessCodeInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("verify_vip_code_button")
                        ) {
                            if (isVerifying) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verifying...",
                                    fontFamily = PoppinsFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Verify Code",
                                    fontFamily = PoppinsFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action: Edit Profile
        Button(
            onClick = onEditClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_edit_button")
        ) {
            Icon(
                imageVector = FluentIcons.Edit24Regular,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Profile",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp
            )
        }
    }
}

/**
 * Stage 2: Profile Edit Form.
 * Contains only Display Name, MBBS Professional Year (modal picker), and Institution.
 */
@Composable
private fun ProfileEditContent(
    userProfile: UserProfile,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current

    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var gender by remember(userProfile) { mutableStateOf(userProfile.gender) }
    var selectedProf by remember(userProfile) { mutableStateOf(userProfile.selectedProf) }
    var institution by remember(userProfile) { mutableStateOf(userProfile.institution) }

    var showProfModal by remember { mutableStateOf(false) }

    val currentCurriculum = remember(selectedProf) {
        MBBS_CURRICULUM.find { it.id.equals(selectedProf, ignoreCase = true) } ?: MBBS_CURRICULUM[0]
    }

    if (showProfModal) {
        ProfSelectionModalBottomSheet(
            selectedProf = selectedProf,
            onProfSelected = { selectedProf = it },
            onDismissRequest = { showProfModal = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back navigation & dismiss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("profile_edit_back_button")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowLeft24Regular,
                    contentDescription = "Back to Overview",
                    tint = colors.textPrimary
                )
            }

            Text(
                text = "Edit Profile",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.textPrimary
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = "Dismiss",
                    tint = colors.textSecondary
                )
            }
        }

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.borderSubtle,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.primary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.textSecondary
        )

        // 1. Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            placeholder = { Text("e.g., Dr. Sharma") },
            leadingIcon = {
                Icon(imageVector = FluentIcons.Person24Regular, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input")
        )

        // 2. MBBS Professional Year (Tappable field opening M3 Expressive modal)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "MBBS Professional Year",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showProfModal = true }
                    .testTag("profile_prof_picker")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Bookmark24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = currentCurriculum.title,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }

                    Icon(
                        imageVector = FluentIcons.ChevronDown24Regular,
                        contentDescription = "Change Year",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Gender
        OutlinedTextField(
            value = gender,
            onValueChange = { gender = it },
            label = { Text("Gender") },
            placeholder = { Text("e.g., Male, Female, Non-binary") },
            leadingIcon = {
                Icon(imageVector = FluentIcons.Person24Regular, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_gender_input")
        )

        // 4. Institution / College (Optional)
        OutlinedTextField(
            value = institution,
            onValueChange = { institution = it },
            label = { Text("Institution / College (Optional)") },
            placeholder = { Text("e.g., Calcutta National Medical College") },
            leadingIcon = {
                Icon(imageVector = FluentIcons.Building24Regular, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_institution_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        val canSave = displayName.trim().isNotBlank() && gender.trim().isNotBlank()

        // Actions: Cancel & Save Changes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("profile_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = InterFamily,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = {
                    val prof = MBBS_CURRICULUM.find { it.id.equals(selectedProf, ignoreCase = true) }
                        ?: MBBS_CURRICULUM[0]
                    val updated = userProfile.copy(
                        displayName = displayName.trim().ifEmpty { "Dr. Sharma" },
                        gender = gender.trim(),
                        institution = institution.trim(),
                        selectedProf = prof.id,
                        targetExamOrSubject = prof.title
                    )
                    onSave(updated)
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .testTag("profile_save_button")
            ) {
                Icon(imageVector = FluentIcons.Save24Regular, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Save",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}

/**
 * Material 3 Expressive MBBS Professional Year Selection Modal.
 * Rounded corners (topStart = 28.dp, topEnd = 28.dp), 56.dp height touch targets,
 * clean titles WITHOUT subject subtext, and active checkmarks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfSelectionModalBottomSheet(
    selectedProf: String,
    onProfSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary,
        tonalElevation = 6.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colors.borderSubtle,
                shape = RoundedCornerShape(100.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Select MBBS Year",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = colors.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MBBS_CURRICULUM.forEach { prof ->
                    val isSelected = prof.id.equals(selectedProf, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.primary else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                onProfSelected(prof.id)
                                onDismissRequest()
                            }
                            .testTag("prof_modal_item_${prof.id.replace(" ", "_").lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = prof.title,
                                fontFamily = PoppinsFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 14.5.sp,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = "Selected",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoTile(
    icon: ImageVector,
    title: String,
    value: String
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primary.copy(alpha = 0.08f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = InterFamily,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = value,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
