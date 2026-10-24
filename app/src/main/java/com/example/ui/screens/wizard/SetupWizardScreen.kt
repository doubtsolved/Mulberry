package com.example.ui.screens.wizard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.mulberry.app.analytics.MulberryAnalytics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.components.FluidGlassDefaults
import com.example.ui.components.fluidGlassPressEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.data.model.UserProfile
import com.example.ui.components.FluentIcons
import com.example.ui.components.MulberryExpressiveLogo
import com.example.ui.screens.MBBS_CURRICULUM
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.MulberryTheme
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.ThemeMode
import com.example.util.PermissionUtils
import com.example.viewmodel.MulberryViewModel
import com.example.viewmodel.SettingsViewModel

enum class WizardStep {
    INTRODUCE,
    PROFILE_AND_THEME,
    PERMISSIONS
}

@Composable
fun SetupWizardScreen(
    viewModel: MulberryViewModel,
    settingsViewModel: SettingsViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme by settingsViewModel.activeTheme.collectAsState()
    val userProfile by settingsViewModel.userProfile.collectAsState()

    var currentStep by remember { mutableStateOf(WizardStep.INTRODUCE) }

    // Transient form states for Step 2
    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var institution by remember(userProfile) { mutableStateOf(userProfile.institution) }
    var selectedProf by remember(userProfile) { mutableStateOf(userProfile.selectedProf) }

    MulberryTheme(themeMode = activeTheme) {
        val colors = LocalMulberryColors.current

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.backgroundGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> -width } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width / 3 } + fadeOut())
                    }
                },
                label = "WizardStepTransition"
            ) { step ->
                when (step) {
                    WizardStep.INTRODUCE -> {
                        IntroduceScreen(
                            onNext = { currentStep = WizardStep.PROFILE_AND_THEME }
                        )
                    }
                    WizardStep.PROFILE_AND_THEME -> {
                        ProfileSetupScreen(
                            displayName = displayName,
                            onDisplayNameChange = { displayName = it },
                            institution = institution,
                            onInstitutionChange = { institution = it },
                            selectedProf = selectedProf,
                            onProfSelected = { selectedProf = it },
                            onBack = { currentStep = WizardStep.INTRODUCE },
                            onNext = {
                                val profCurriculum = MBBS_CURRICULUM.find { it.id.equals(selectedProf, ignoreCase = true) }
                                    ?: MBBS_CURRICULUM[0]
                                val updated = userProfile.copy(
                                    displayName = displayName.trim().ifEmpty { "Sahabul" },
                                    institution = institution.trim(),
                                    selectedProf = profCurriculum.id,
                                    targetExamOrSubject = profCurriculum.title
                                )
                                settingsViewModel.saveProfile(updated)
                                viewModel.saveUserProfile(updated)
                                currentStep = WizardStep.PERMISSIONS
                            }
                        )
                    }
                    WizardStep.PERMISSIONS -> {
                        val context = LocalContext.current
                        PermissionsScreen(
                            onBack = { currentStep = WizardStep.PROFILE_AND_THEME },
                            onStorageGranted = {
                                settingsViewModel.syncLocalProfile()
                            },
                            onLaunchApp = {
                                val prefs = context.getSharedPreferences("mulberry_onboarding", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                                settingsViewModel.syncLocalProfile()
                                val currentProfile = viewModel.userProfile.value
                                MulberryAnalytics.logProfileRegistered(
                                    name = currentProfile.displayName,
                                    college = currentProfile.institution,
                                    prof = currentProfile.selectedProf
                                )
                                onFinish()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 1: Welcome & The Morula Principle
 */
@Composable
private fun IntroduceScreen(
    onNext: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Center Branding & Editorial Morula Principle Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            // Centered Expressive Logo
            MulberryExpressiveLogo(
                size = 96.dp,
                animated = true,
                showContainer = true
            )

            // Header titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Mulberry",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Medical Study Workspace",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            // THE MORULA PRINCIPLE CARD
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "THE MORULA PRINCIPLE",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 1.2.sp,
                        color = colors.primary
                    )

                    Text(
                        text = "Named after the embryological morula (Latin for mulberry)—the earliest cluster of dividing cells before life takes complex form.",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        lineHeight = 19.sp
                    )

                    Text(
                        text = "Mastery in medicine follows the exact same pattern: competence is built through quiet, compounding divisions—sixteen pages digested today, an anatomical pathway understood tomorrow.",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shimmering "Get Started ->" Button
        ShimmeringGetStartedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Luminous button with continuous shimmering gradient light sweep across its surface
 */
@Composable
private fun ShimmeringGetStartedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current

    val transition = rememberInfiniteTransition(label = "ShimmerSweep")
    val sweepProgress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepProgress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.primary,
        border = BorderStroke(1.dp, FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)),
        shadowElevation = 8.dp,
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = colors.primary.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(16.dp))
            .fluidGlassPressEffect(onClick = onClick)
            .drawWithContent {
                drawContent()
                val shimmerWidth = size.width * 0.45f
                val startX = sweepProgress * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(startX + shimmerWidth, size.height)
                    )
                )
            }
            .testTag("wizard_get_started_button")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Get Started",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = FluentIcons.ArrowRight24Regular,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Screen 2: Academic Identity Profile Setup.
 * Contains only Display Name, MBBS Professional Year (modal picker), and Institution.
 * Completely free of daily goals, color theme dropdowns, or subject subtexts.
 */
@Composable
private fun ProfileSetupScreen(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    institution: String,
    onInstitutionChange: (String) -> Unit,
    selectedProf: String,
    onProfSelected: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalMulberryColors.current
    var showProfModal by remember { mutableStateOf(false) }

    val currentCurriculum = remember(selectedProf) {
        MBBS_CURRICULUM.find { it.id.equals(selectedProf, ignoreCase = true) } ?: MBBS_CURRICULUM[0]
    }

    if (showProfModal) {
        com.example.ui.screens.ProfSelectionModalBottomSheet(
            selectedProf = selectedProf,
            onProfSelected = { onProfSelected(it) },
            onDismissRequest = { showProfModal = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Header: "Step 1 of 2" & Indicator Dots [o] [ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step 1 of 2",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primary
                )

                // Dots [o] [ ]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.primary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .border(1.dp, colors.primary.copy(alpha = 0.35f), CircleShape)
                    )
                }
            }

            Text(
                text = "Your Profile",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.textPrimary
            )

            // 1. DISPLAY NAME
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DISPLAY NAME",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    placeholder = { Text("Sahabul", color = colors.textMuted) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = FluentIcons.Person24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.surfaceCard,
                        unfocusedContainerColor = colors.surfaceCard,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_display_name_input")
                )
            }

            // 2. MBBS PROFESSIONAL YEAR (Tappable M3 Expressive modal launcher)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MBBS PROFESSIONAL YEAR",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceCard.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .fluidGlassPressEffect(onClick = { showProfModal = true })
                        .testTag("wizard_prof_dropdown")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            contentDescription = "Select Year",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. INSTITUTION · OPTIONAL
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "INSTITUTION · OPTIONAL",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = institution,
                    onValueChange = onInstitutionChange,
                    placeholder = { Text("College or institution name", color = colors.textMuted) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = FluentIcons.Building24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.surfaceCard,
                        unfocusedContainerColor = colors.surfaceCard,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_institution_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom CTA Buttons: Back & Continue ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .fluidGlassPressEffect(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Back", fontFamily = InterFamily, color = colors.textSecondary)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.primary,
                border = BorderStroke(1.dp, FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .weight(1.8f)
                    .height(50.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = colors.primary.copy(alpha = 0.40f))
                    .clip(RoundedCornerShape(14.dp))
                    .fluidGlassPressEffect(onClick = onNext)
                    .testTag("wizard_continue_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = FluentIcons.ArrowRight24Regular,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Screen 3: Permissions (Minimal Action Rows)
 */
@Composable
private fun PermissionsScreen(
    onBack: () -> Unit,
    onStorageGranted: () -> Unit = {},
    onLaunchApp: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    var isStorageGranted by remember {
        mutableStateOf(PermissionUtils.hasStoragePermission(context))
    }
    var isNotificationGranted by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }

    fun checkAndNotifyStorage() {
        val grantedStorage = PermissionUtils.hasStoragePermission(context)
        if (grantedStorage && !isStorageGranted) {
            onStorageGranted()
        }
        isStorageGranted = grantedStorage
        isNotificationGranted = PermissionUtils.hasNotificationPermission(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        checkAndNotifyStorage()
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndNotifyStorage()
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkAndNotifyStorage()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        checkAndNotifyStorage()
    }

    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val uri = Uri.parse("package:${context.packageName}")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    manageStorageLauncher.launch(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(fallback)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } else {
            legacyLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    fun requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionUtils.openAppSettings(context)
        }
    }

    val canLaunch = isStorageGranted && isNotificationGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Header: "Step 2 of 2" & Indicator Dots [o] [o]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step 2 of 2",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primary
                )

                // Dots [o] [o]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.primary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.primary, CircleShape)
                    )
                }
            }

            Text(
                text = "Permissions",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Row 1: Storage Access
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.10f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = FluentIcons.Folder24Regular,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Storage Access",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Save books from online library",
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isStorageGranted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Allowed",
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.primary,
                            border = BorderStroke(1.dp, FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .fluidGlassPressEffect(onClick = { requestStorageAccess() })
                                .testTag("wizard_allow_storage_button")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Allow",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Action Row 2: Notifications
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.10f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = FluentIcons.Alert24Regular,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Notifications",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Study & exam reminders",
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isNotificationGranted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Allowed",
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.primary,
                            border = BorderStroke(1.dp, FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .fluidGlassPressEffect(onClick = { requestNotificationAccess() })
                                .testTag("wizard_allow_notifications_button")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Allow",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Buttons: Back & Launch Mulberry ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .fluidGlassPressEffect(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Back", fontFamily = InterFamily, color = colors.textSecondary)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (canLaunch) colors.primary else colors.surfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (canLaunch) FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)
                    else FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)
                ),
                shadowElevation = if (canLaunch) 8.dp else 0.dp,
                modifier = Modifier
                    .weight(1.8f)
                    .height(50.dp)
                    .then(
                        if (canLaunch) Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = colors.primary.copy(alpha = 0.40f)
                        ) else Modifier
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .fluidGlassPressEffect(enabled = canLaunch, onClick = onLaunchApp)
                    .testTag("wizard_launch_app_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FluentIcons.Play24Regular,
                        contentDescription = null,
                        tint = if (canLaunch) Color.White else colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Launch Mulberry",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (canLaunch) Color.White else colors.textMuted
                    )
                }
            }
        }
    }
}
