@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entities.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors

@Composable
fun BookDetailBottomSheet(
    book: BookEntity,
    sheetState: SheetState,
    downloadProgress: Float?,
    onDismiss: () -> Unit,
    onDownload: (BookEntity) -> Unit,
    onOpenInReader: (BookEntity) -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderSubtle)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Cover + Main Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Cover Art
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(book.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Type pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceTint.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = book.type,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = book.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${book.author} • ${book.edition}",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Stats badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${book.totalPages} pages",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = "${book.sizeMb} MB",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.Lock24Regular,
                        contentDescription = "Encrypted",
                        tint = colors.accentSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AES-256-GCM Vault Payload",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Zero-plaintext disk encryption • Ephemeral viewer session",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mirrors & External Links
            Text(
                text = "Download Mirrors",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary GitHub Releases Mirror badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FluentIcons.ArrowDownload24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GitHub Release",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Google Drive Fallback
                if (book.driveFallbackUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.driveFallbackUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Drive", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }

                // Telegram Deep Link
                if (book.telegramDeepLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.telegramDeepLink))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Telegram", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Download Progress or CTA Button
            if (downloadProgress != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading encrypted payload...",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        color = colors.primary,
                        trackColor = colors.borderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            } else if (book.isDownloaded) {
                Button(
                    onClick = {
                        onDismiss()
                        onOpenInReader(book)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Book24Regular,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open in Reader (Page ${book.lastReadPage})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
            } else {
                Button(
                    onClick = { onDownload(book) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.ArrowDownload24Regular,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download to Shelf (${book.sizeMb} MB)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}
