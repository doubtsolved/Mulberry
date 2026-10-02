package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.DownloadState
import com.example.util.ReleaseInfo
import com.example.util.UpdateManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    release: ReleaseInfo,
    currentVersion: String,
    downloadState: DownloadState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedContent(
                targetState = downloadState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "update_state_transition"
            ) { state ->
                when (state) {
                    is DownloadState.Idle -> {
                        StateAChangelog(
                            release = release,
                            currentVersion = currentVersion,
                            onUpdateNow = onStartDownload,
                            onLater = onDismiss
                        )
                    }
                    is DownloadState.Downloading -> {
                        StateBDownloading(
                            release = release,
                            downloading = state,
                            onCancel = onCancelDownload
                        )
                    }
                    is DownloadState.Completed -> {
                        StateCReadyToInstall(
                            release = release,
                            onInstall = onInstall,
                            onDismiss = onDismiss
                        )
                    }
                    is DownloadState.Error -> {
                        StateError(
                            errorMessage = state.message,
                            onRetry = onStartDownload,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// STATE A: Release Overview & Changelog
// -------------------------------------------------------------------------
@Composable
private fun StateAChangelog(
    release: ReleaseInfo,
    currentVersion: String,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowDownload24Regular,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "New Update Available!",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Mulberry ${release.tagName} is now ready",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Version Comparison Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: v$currentVersion",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
                Text(
                    text = "──➔",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.primary
                )
                Text(
                    text = "New: ${release.tagName}${if (release.formattedSize.isNotBlank()) " • " + release.formattedSize else ""}",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primary
                )
            }
        }

        // Changelog Section
        Text(
            text = "WHAT'S NEW IN THIS VERSION:",
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = colors.textSecondary
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            val changelogText = if (release.changelog.isNotBlank()) {
                release.changelog.trim()
            } else {
                "• Performance and stability enhancements\n• Bug fixes and library optimizations"
            }
            Text(
                text = changelogText,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            )
        }

        // Action Buttons: [ Later ] [ Update Now ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_later_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = "Later",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = onUpdateNow,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_now_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(
                    text = "Update Now",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onPrimary
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// STATE B: Active Download & Installation Progress
// -------------------------------------------------------------------------
@Composable
private fun StateBDownloading(
    release: ReleaseInfo,
    downloading: DownloadState.Downloading,
    onCancel: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowDownload24Regular,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Downloading Update...",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Mulberry ${release.tagName} • Please keep app open",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val progressFraction = (downloading.progressPercent / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.primary,
                trackColor = colors.borderSubtle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${downloading.progressPercent}% • ${downloading.downloadedMb} MB / ${downloading.totalMb}",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                if (downloading.speedFormatted.isNotBlank()) {
                    Text(
                        text = downloading.speedFormatted,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.primary
                    )
                }
            }
        }

        // Action Buttons: [ Cancel ] [ Downloading... (Disabled) ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cancel_download_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = colors.primary.copy(alpha = 0.5f),
                    disabledContentColor = colors.onPrimary.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Downloading...",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// STATE C: Ready to Install (Revised per user request: no extra paragraphs)
// -------------------------------------------------------------------------
@Composable
private fun StateCReadyToInstall(
    release: ReleaseInfo,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Checkmark24Regular,
                    contentDescription = null,
                    tint = colors.accentSuccess,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Download Complete!",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Ready to install Mulberry ${release.tagName}",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Action Buttons: [ Dismiss ] [ Install Now ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("dismiss_install_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = "Dismiss",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = onInstall,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("install_now_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentSuccess)
            ) {
                Text(
                    text = "Install Now",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onPrimary
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// STATE ERROR
// -------------------------------------------------------------------------
@Composable
private fun StateError(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentDanger.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = null,
                    tint = colors.accentDanger,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Update Failed",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = errorMessage,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = "Close",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(
                    text = "Try Again",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onPrimary
                )
            }
        }
    }
}
