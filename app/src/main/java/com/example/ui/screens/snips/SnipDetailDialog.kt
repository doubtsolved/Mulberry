package com.example.ui.screens.snips

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entities.SnipEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import java.io.File

@Composable
fun SnipDetailDialog(
    snip: SnipEntity,
    onDismiss: () -> Unit,
    onJumpToPage: (SnipEntity) -> Unit,
    onDelete: (SnipEntity) -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = snip.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${snip.subject} • Page ${snip.pageNumber}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = FluentIcons.Dismiss24Regular,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-res Image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(snip.imageLocalPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = snip.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceTint
                    ) {
                        Text(
                            text = snip.bookTitle.ifBlank { "Textbook" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (snip.tagsCsv.isNotBlank()) {
                        Text(
                            text = "Tags: ${snip.tagsCsv}",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(snip)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Dismiss24Regular,
                            contentDescription = null,
                            tint = colors.accentDanger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 13.sp, color = colors.accentDanger)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onJumpToPage(snip)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Book24Regular,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Jump to Page ${snip.pageNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}
