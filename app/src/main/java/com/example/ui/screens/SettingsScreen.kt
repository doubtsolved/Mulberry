package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.screens.SettingsSheetHeader
import com.example.ui.theme.IconStyle
import com.example.ui.theme.SemanticColors
import com.example.ui.theme.getSemanticIconTint
import com.example.util.StorageMetricsHelper
import com.example.viewmodel.rootPath
import com.example.data.model.VaultEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.MulberryTheme
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.MulberryViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.BuildConfig
import com.example.ui.components.UpdateBottomSheet
import com.example.util.DownloadState
import com.example.util.ReleaseInfo
import com.example.util.UpdateCheckResult
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.util.Locale

enum class ActiveSettingsSheet {
    NONE,
    VAULTS,
    SYNC_OPTIONS,
    THEME,
    READING_MODE,
    DATA_PORTABILITY,
    STORAGE_CACHE,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MulberryViewModel,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentTheme by settingsViewModel.activeTheme.collectAsState()
    val iconStyle by settingsViewModel.iconStyle.collectAsState()
    val vaults by settingsViewModel.vaults.collectAsState()
    val books by settingsViewModel.books.collectAsState()
    val isSinglePageDefault by settingsViewModel.isSinglePageDefault.collectAsState()
    val isPageFlipAnimationEnabled by settingsViewModel.isPageFlipAnimationEnabled.collectAsState()
    val keepScreenAwake by settingsViewModel.keepScreenAwake.collectAsState()
    val cacheSizeBytes by settingsViewModel.cacheSizeBytes.collectAsState()
    val databaseSizeBytes by settingsViewModel.databaseSizeBytes.collectAsState()
    val userProfile by settingsViewModel.userProfile.collectAsState()

    var showProfileBottomSheet by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(ActiveSettingsSheet.NONE) }
    var vaultToRename by remember { mutableStateOf<VaultEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val isCheckingForUpdate by settingsViewModel.isCheckingForUpdate.collectAsState()
    val updateCheckResult by settingsViewModel.updateCheckResult.collectAsState()
    val downloadState by settingsViewModel.downloadState.collectAsState()
    var showUpdateSheet by remember { mutableStateOf(false) }
    var activeRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Storage Access Framework Folder Picker
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val segment = uri.lastPathSegment ?: "Vault"
            val folderName = segment.substringAfterLast(":").ifEmpty { "Vault" }
            settingsViewModel.bindVaultFolder(uri, folderName)
            viewModel.bindFolderUri(uri, folderName)
            Toast.makeText(context, "Bound vault: $folderName", Toast.LENGTH_SHORT).show()
        }
    }

    // Zip Backup Restore Picker
    val zipRestorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val currentVault = viewModel.activeVault.value ?: vaults.firstOrNull()
            val vaultPath = currentVault?.pathDisplay?.ifEmpty { currentVault.uriString } ?: ""
            if (vaultPath.isNotBlank()) {
                settingsViewModel.restoreZipBackup(uri, vaultPath) { success ->
                    if (success) {
                        viewModel.refreshLibrary()
                        Toast.makeText(context, "Backup restored successfully from ZIP", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to restore backup from ZIP", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Please link an active vault first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    MulberryTheme(themeMode = currentTheme) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Screen Header: Poppins Bold 28sp
            Text(
                text = "Settings",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // TOP PROFILE HERO CARD
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceCard,
                border = BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showProfileBottomSheet = true }
                    .testTag("profile_hero_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Initials Avatar Box (52.dp circle/rounded square)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getInitials(userProfile.displayName),
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Middle: Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = userProfile.displayName,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${userProfile.academicRole} • ${userProfile.targetExamOrSubject}",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "🎯 Target: ${userProfile.dailyPageGoal} pages/day",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Right: Fluent ChevronRight16 icon
                    Icon(
                        imageVector = FluentIcons.ChevronRight16Regular,
                        contentDescription = "Edit Profile",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // GROUP 1: VAULTS & SYNC
            SettingsCategoryGroup(
                title = "VAULTS & SYNC",
                titleColor = colors.primary
            ) {
                SettingsItemRow(
                    icon = FluentIcons.Folder24Filled,
                    title = "Storage Vaults",
                    valueLabel = "${vaults.size} Vaults Linked",
                    iconTint = getSemanticIconTint(SemanticColors.Folder, iconStyle),
                    onClick = { activeSheet = ActiveSettingsSheet.VAULTS }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
                SettingsItemRow(
                    icon = FluentIcons.Clock24Regular,
                    title = "Sync & Scan",
                    valueLabel = "Automatic (On Resume)",
                    iconTint = getSemanticIconTint(SemanticColors.Sync, iconStyle),
                    onClick = { activeSheet = ActiveSettingsSheet.SYNC_OPTIONS }
                )
            }

            // GROUP 2: READING & APPEARANCE
            SettingsCategoryGroup(
                title = "READING & APPEARANCE",
                titleColor = colors.primary
            ) {
                SettingsItemRow(
                    icon = FluentIcons.Shapes24Regular,
                    title = "Theme & Palette",
                    valueLabel = "${currentTheme.title} • ${if (iconStyle == IconStyle.VIBRANT) "Vibrant" else "Adaptive"}",
                    onClick = { activeSheet = ActiveSettingsSheet.THEME }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
                SettingsItemRow(
                    icon = FluentIcons.BookOpen24Regular,
                    title = "Reading Preferences",
                    valueLabel = if (isPageFlipAnimationEnabled) "Animation On" else "Animation Off",
                    onClick = { activeSheet = ActiveSettingsSheet.READING_MODE }
                )
            }

            // GROUP 3: DATA & STORAGE
            SettingsCategoryGroup(
                title = "DATA & STORAGE",
                titleColor = colors.primary
            ) {
                SettingsItemRow(
                    icon = FluentIcons.LibrarySpines24Regular,
                    title = "Data Portability (.zip)",
                    valueLabel = "Active",
                    iconTint = getSemanticIconTint(SemanticColors.Pdf, iconStyle),
                    onClick = { activeSheet = ActiveSettingsSheet.DATA_PORTABILITY }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
                SettingsItemRow(
                    icon = FluentIcons.Delete24Regular,
                    title = "Storage & Cache",
                    valueLabel = StorageMetricsHelper.formatFileSize(context, cacheSizeBytes),
                    iconTint = getSemanticIconTint(SemanticColors.Target, iconStyle),
                    onClick = { activeSheet = ActiveSettingsSheet.STORAGE_CACHE }
                )
            }

            // ABOUT MULBERRY
            SettingsCategoryGroup(
                title = "ABOUT MULBERRY",
                titleColor = colors.primary
            ) {
                SettingsItemRow(
                    icon = FluentIcons.ArrowSync24Regular,
                    title = "Check for Updates",
                    showChevron = false,
                    trailingContent = if (isCheckingForUpdate) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else null,
                    onClick = {
                        settingsViewModel.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
                            when (result) {
                                is UpdateCheckResult.UpdateAvailable -> {
                                    activeRelease = result.release
                                    showUpdateSheet = true
                                }
                                is UpdateCheckResult.UpToDate -> {
                                    Toast.makeText(context, "✓ Mulberry is up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                }
                                is UpdateCheckResult.Error -> {
                                    Toast.makeText(context, "Update check failed: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
                SettingsItemRow(
                    icon = FluentIcons.Info24Regular,
                    title = "App Version",
                    subtitle = BuildConfig.VERSION_NAME.ifEmpty { "1.0.0" },
                    showChevron = false,
                    onClick = { activeSheet = ActiveSettingsSheet.ABOUT }
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

        // ============================================================
        // MODAL BOTTOM SHEETS
        // ============================================================
        if (activeSheet != ActiveSettingsSheet.NONE) {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = ActiveSettingsSheet.NONE },
                sheetState = sheetState,
                containerColor = colors.surface,
                contentColor = colors.textPrimary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                when (activeSheet) {
                    ActiveSettingsSheet.VAULTS -> {
                        VaultsSheetContent(
                            vaults = vaults,
                            booksCount = books.size,
                            onBindNew = {
                                coroutineScope.launch { sheetState.hide() }
                                activeSheet = ActiveSettingsSheet.NONE
                                folderPicker.launch(null)
                            },
                            onRename = { vault ->
                                vaultToRename = vault
                                renameText = vault.name
                            },
                            onUnbind = { vault ->
                                settingsViewModel.unbindVault(vault)
                                viewModel.deleteVault(vault)
                                Toast.makeText(context, "Unbound ${vault.name}", Toast.LENGTH_SHORT).show()
                            },
                            onCopyPath = { path ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Vault Path", path))
                                Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    ActiveSettingsSheet.THEME -> {
                        ThemePickerSheetContent(
                            currentTheme = currentTheme,
                            iconStyle = iconStyle,
                            onSelectTheme = { theme ->
                                settingsViewModel.setTheme(theme)
                                viewModel.setThemeMode(theme)
                            },
                            onSelectIconStyle = { style ->
                                settingsViewModel.setIconStyle(style)
                                viewModel.setIconStyle(style)
                            }
                        )
                    }
                    ActiveSettingsSheet.READING_MODE -> {
                        ReadingModeSheetContent(
                            isPageFlipAnimationEnabled = isPageFlipAnimationEnabled,
                            keepScreenAwake = keepScreenAwake,
                            onToggleAnimation = { enabled ->
                                settingsViewModel.togglePageFlipAnimation(enabled)
                            },
                            onToggleKeepScreenAwake = { enabled ->
                                settingsViewModel.toggleKeepScreenAwake(enabled)
                            }
                        )
                    }
                    ActiveSettingsSheet.SYNC_OPTIONS -> {
                        SyncOptionsSheetContent(
                            onForceReindex = {
                                settingsViewModel.forceReindexVaults {
                                    Toast.makeText(context, "Vault directories reconciled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ActiveSettingsSheet.DATA_PORTABILITY -> {
                        val currentVault = viewModel.activeVault.value ?: vaults.firstOrNull()
                        val vaultPath = currentVault?.pathDisplay?.ifEmpty { currentVault.uriString } ?: ""
                        DataPortabilitySheetContent(
                            onForceFlush = {
                                settingsViewModel.forceFlushMetadata {
                                    Toast.makeText(context, "Flushed all metadata to .mulberry folders", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onExportZipBackup = {
                                if (vaultPath.isNotBlank()) {
                                    settingsViewModel.exportZipBackup(vaultPath) { zipUri ->
                                        if (zipUri != null) {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(Intent.EXTRA_STREAM, zipUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Mulberry Backup (.zip)"))
                                        } else {
                                            Toast.makeText(context, "Export failed. Link an active vault first.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No active vault linked", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRestoreZipBackup = {
                                zipRestorePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                    ActiveSettingsSheet.STORAGE_CACHE -> {
                        StorageCacheSheetContent(
                            cacheSizeBytes = cacheSizeBytes,
                            databaseSizeBytes = databaseSizeBytes,
                            onClearCache = {
                                settingsViewModel.clearCache {
                                    Toast.makeText(context, "Render cache cleared.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReindex = {
                                settingsViewModel.forceReindexVaults {
                                    Toast.makeText(context, "Re-indexed library from disk", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ActiveSettingsSheet.ABOUT -> {
                        AboutSheetContent()
                    }
                    ActiveSettingsSheet.NONE -> {}
                }
            }
        }

        // User Profile Modal Bottom Sheet
        if (showProfileBottomSheet) {
            UserProfileBottomSheet(
                userProfile = userProfile,
                onDismissRequest = { showProfileBottomSheet = false },
                onSaveProfile = { updated ->
                    val vaultPath = viewModel.activeVault.value?.rootPath
                    settingsViewModel.saveProfile(updated, vaultPath)
                    viewModel.saveUserProfile(updated)
                }
            )
        }

        // In-App Update Modal Bottom Sheet
        if (showUpdateSheet && activeRelease != null) {
            UpdateBottomSheet(
                release = activeRelease!!,
                currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.0.0" },
                downloadState = downloadState,
                onStartDownload = {
                    activeRelease?.let { settingsViewModel.startDownload(it) }
                },
                onCancelDownload = {
                    settingsViewModel.cancelDownload()
                },
                onInstall = {
                    val completed = downloadState as? DownloadState.Completed
                    if (completed != null) {
                        settingsViewModel.installApk(context, completed.apkFile)
                    }
                },
                onDismiss = {
                    showUpdateSheet = false
                    settingsViewModel.dismissUpdate()
                }
            )
        }

        // Rename Vault Dialog
        if (vaultToRename != null) {
            AlertDialog(
                onDismissRequest = { vaultToRename = null },
                title = {
                    Text(
                        text = "Rename Vault",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Vault Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            cursorColor = colors.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val v = vaultToRename
                            if (v != null && renameText.isNotBlank()) {
                                settingsViewModel.renameVault(v, renameText.trim())
                                viewModel.renameVault(v, renameText.trim())
                                Toast.makeText(context, "Vault renamed", Toast.LENGTH_SHORT).show()
                            }
                            vaultToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Save", fontFamily = InterFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vaultToRename = null }) {
                        Text("Cancel", fontFamily = InterFamily, color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Import Backup JSON Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text(
                        text = "Import JSON Backup",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste your exported Mulberry JSON below to restore tasks and books:",
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("{ \"version\": 1, ... }") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                cursorColor = colors.primary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                settingsViewModel.importJsonBackup(importJsonText.trim()) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid JSON backup format", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showImportDialog = false
                            importJsonText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Restore", fontFamily = InterFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImportDialog = false
                        importJsonText = ""
                    }) {
                        Text("Cancel", fontFamily = InterFamily, color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ============================================================
// REUSABLE SETTINGS ITEM ROW COMPONENT
// ============================================================
@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    valueLabel: String? = null,
    showChevron: Boolean = true,
    iconTint: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon (24dp, tinted with iconTint or textSecondary)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Title & optional Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Trailing Content or valueLabel + Chevron
        if (trailingContent != null) {
            trailingContent()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!valueLabel.isNullOrBlank()) {
                    Text(
                        text = valueLabel,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = FluentIcons.ChevronRight16Regular,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Category Group Container Card
@Composable
private fun SettingsCategoryGroup(
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = titleColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// ============================================================
// BOTTOM SHEET CONTENT COMPOSABLES
// ============================================================

// 1. Vaults Sheet
@Composable
private fun VaultsSheetContent(
    vaults: List<VaultEntity>,
    booksCount: Int,
    onBindNew: () -> Unit,
    onRename: (VaultEntity) -> Unit,
    onUnbind: (VaultEntity) -> Unit,
    onCopyPath: (String) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Storage Vaults",
            tooltipText = "Mulberry directly indexes physical folders via Android Storage Access Framework (SAF). Linked textbooks stay portable and intact on your device."
        )

        Text(
            text = "${vaults.size} linked folders • $booksCount total books indexed",
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = colors.textSecondary
        )

        if (vaults.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceCard,
                border = BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No vaults connected. Bind a physical directory with PDFs.",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                vaults.forEach { vault ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vault.name,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text(
                                        text = "${vault.bookCount} books",
                                        fontFamily = InterFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = vault.pathDisplay.ifEmpty { vault.uriString },
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onCopyPath(vault.pathDisplay.ifEmpty { vault.uriString }) }) {
                                    Text("Copy Path", fontFamily = InterFamily, fontSize = 12.sp, color = colors.textSecondary)
                                }
                                TextButton(onClick = { onRename(vault) }) {
                                    Text("Rename", fontFamily = InterFamily, fontSize = 12.sp, color = colors.primary)
                                }
                                TextButton(onClick = { onUnbind(vault) }) {
                                    Text("Unbind", fontFamily = InterFamily, fontSize = 12.sp, color = colors.accentDanger)
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBindNew,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "➕ Bind New Vault Folder",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 2. Theme Picker Sheet
@Composable
private fun ThemePickerSheetContent(
    currentTheme: ThemeMode,
    iconStyle: IconStyle,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectIconStyle: (IconStyle) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Theme & Palette",
            tooltipText = "Select eye-comfort palettes and toggle between colorful semantic accents or minimal monochrome icons."
        )

        // Top section: Two-tab pill toggle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceCard,
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isVibrant = iconStyle == IconStyle.VIBRANT
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isVibrant) colors.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelectIconStyle(IconStyle.VIBRANT) }
                ) {
                    Text(
                        text = "🎨 Vibrant Coloured",
                        fontFamily = InterFamily,
                        fontWeight = if (isVibrant) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (isVibrant) colors.onPrimary else colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (!isVibrant) colors.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelectIconStyle(IconStyle.ADAPTIVE) }
                ) {
                    Text(
                        text = "🎭 Theme Adaptive",
                        fontFamily = InterFamily,
                        fontWeight = if (!isVibrant) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (!isVibrant) colors.onPrimary else colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        val themeOptions = listOf(
            ThemeOption(
                mode = ThemeMode.OLDED,
                name = "Olded (Warm Sepia)",
                desc = "Warm sepia tones for blue-light eye comfort.",
                bgColor = Color(0xFFFBF0D9),
                surfaceColor = Color(0xFFF4E6C3),
                accentColor = Color(0xFFC26D38)
            ),
            ThemeOption(
                mode = ThemeMode.PAPER,
                name = "Paper (Clean White)",
                desc = "Crisp, daylight paper finish with clean contrast.",
                bgColor = Color(0xFFFFFFFF),
                surfaceColor = Color(0xFFF1F5F9),
                accentColor = Color(0xFF2563EB)
            ),
            ThemeOption(
                mode = ThemeMode.MIDNIGHT,
                name = "Midnight (Pitch OLED)",
                desc = "Deep pitch-black interface that maximizes battery savings.",
                bgColor = Color(0xFF000000),
                surfaceColor = Color(0xFF121212),
                accentColor = Color(0xFF38BDF8)
            ),
            ThemeOption(
                mode = ThemeMode.FOREST,
                name = "Forest (Sage Dark)",
                desc = "Calming sage dark tones inspired by natural pine.",
                bgColor = Color(0xFF111A15),
                surfaceColor = Color(0xFF19261F),
                accentColor = Color(0xFF4ADE80)
            ),
            ThemeOption(
                mode = ThemeMode.ESPRESSO,
                name = "Espresso (Mocha Dark)",
                desc = "Warm roasted mocha tones with golden amber accents.",
                bgColor = Color(0xFF1A1412),
                surfaceColor = Color(0xFF261E1A),
                accentColor = Color(0xFFF59E0B)
            ),
            ThemeOption(
                mode = ThemeMode.DUSK,
                name = "Dusk (Lavender/Indigo)",
                desc = "Twilight indigo slate theme with gentle purple tints.",
                bgColor = Color(0xFF13111C),
                surfaceColor = Color(0xFF1E1B2E),
                accentColor = Color(0xFFA78BFA)
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themeOptions.forEach { opt ->
                val isSelected = currentTheme == opt.mode ||
                    (opt.mode == ThemeMode.PAPER && currentTheme == ThemeMode.LIGHT) ||
                    (opt.mode == ThemeMode.MIDNIGHT && currentTheme == ThemeMode.DARK) ||
                    (opt.mode == ThemeMode.DUSK && currentTheme == ThemeMode.SLATE)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surfaceCard
                    ),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) colors.primary else colors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTheme(opt.mode) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 3-circle Color Preview Swatch
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(opt.bgColor)
                                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(opt.surfaceColor)
                                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(opt.accentColor)
                                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.name,
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = opt.desc,
                                fontFamily = InterFamily,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTheme(opt.mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private data class ThemeOption(
    val mode: ThemeMode,
    val name: String,
    val desc: String,
    val bgColor: Color,
    val surfaceColor: Color,
    val accentColor: Color
)

// 3. Reading Preferences Sheet
@Composable
private fun ReadingModeSheetContent(
    isPageFlipAnimationEnabled: Boolean,
    keepScreenAwake: Boolean,
    onToggleAnimation: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Reading Preferences"
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Page Turn Animation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAnimation(!isPageFlipAnimationEnabled) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.BookOpen24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Page Turn Animation",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isPageFlipAnimationEnabled,
                        onCheckedChange = onToggleAnimation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.borderSubtle
                        )
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))

                // Keep Screen Awake Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleKeepScreenAwake(!keepScreenAwake) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.Star20Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Keep Screen Awake",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = keepScreenAwake,
                        onCheckedChange = onToggleKeepScreenAwake,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.borderSubtle
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 4. Sync Options Sheet
@Composable
private fun SyncOptionsSheetContent(
    onForceReindex: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Sync & Scan",
            tooltipText = "Whenever you return to Mulberry, linked folders are scanned in background coroutines. New PDFs are added and removed files are cleansed automatically."
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FluentIcons.Checkmark24Regular,
                    contentDescription = null,
                    tint = colors.accentSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Automatic Background Scan Active",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            }
        }

        Button(
            onClick = onForceReindex,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Force Full Vault Re-scan Now", fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 5. Data Portability Sheet
@Composable
private fun DataPortabilitySheetContent(
    onForceFlush: () -> Unit,
    onExportZipBackup: () -> Unit,
    onRestoreZipBackup: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Data Portability & Backup",
            tooltipText = "Creates or restores a compressed snapshot of your reading spots, favorites, and tasks."
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onExportZipBackup,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📦 Export Backup (.zip)",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onRestoreZipBackup,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📥 Restore from Backup (.zip)",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            }

            TextButton(
                onClick = onForceFlush,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Force Flush to Disk",
                    fontFamily = InterFamily,
                    color = colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 6. Storage & Cache Sheet
@Composable
private fun StorageCacheSheetContent(
    cacheSizeBytes: Long,
    databaseSizeBytes: Long,
    onClearCache: () -> Unit,
    onReindex: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSheetHeader(
            title = "Storage & Cache",
            tooltipText = "Manage rendered page buffers, thumbnail caches, and storage footprint."
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thumbnail & Render Cache",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = StorageMetricsHelper.formatFileSize(context, cacheSizeBytes),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.primary
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Database Index",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = StorageMetricsHelper.formatFileSize(context, databaseSizeBytes),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onClearCache,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentDanger),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Cache", fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium)
            }

            TextButton(
                onClick = onReindex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Re-index Library from Disk", fontFamily = InterFamily, color = colors.textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 7. About Sheet
@Composable
private fun AboutSheetContent() {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = FluentIcons.Book24Filled,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Mulberry",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.textPrimary
            )
            Text(
                text = "Version 1.0.0 (Build 2026.09)",
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Text(
                text = "Native Medical Reader & Vault Study Environment",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.primary
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Engineering Philosophy",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Mulberry was designed exclusively for medical students and clinicians navigating massive anatomical atlases and clinical textbooks. Zero tracking, zero mandatory cloud lock-in, and offline-first folder vault architecture.",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// Diagnostic Feedback Helper
private fun sendDiagnosticFeedback(context: Context, themeTitle: String) {
    try {
        val body = """
            Hi Mulberry Team,

            [Type your suggestions, issues, or medical study feedback here]

            ---
            Diagnostics:
            • Device: ${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}
            • OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            • App: Mulberry v1.0.0
            • Active Theme: $themeTitle
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@mulberry.app?subject=" + Uri.encode("Feedback: Mulberry Android App") + "&body=" + Uri.encode(body))
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
    }
}

private fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "U"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    } else {
        trimmed.take(2).uppercase()
    }
}

