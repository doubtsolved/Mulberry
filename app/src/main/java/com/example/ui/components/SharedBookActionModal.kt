package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookEntity
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

/**
 * Shared Book-Action Modal (Wireframe 05)
 * Consistent across Desk, My Library, Books Collection, and Study Planner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedBookActionModal(
    book: BookEntity,
    onDismiss: () -> Unit,
    onResumeReading: (BookEntity) -> Unit,
    onOpenFromBeginning: (BookEntity) -> Unit,
    onAddToCollection: (BookEntity) -> Unit,
    onViewDetails: (BookEntity) -> Unit,
    onRemoveFromLibrary: (BookEntity) -> Unit
) {
    val colors = LocalMulberryColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalPages = book.pageCount.coerceAtLeast(1)
    val currentPage = book.lastReadPage.coerceIn(1, totalPages)
    val hasStarted = book.lastReadPage > 1
    val progressPercent = if (book.progressPercent > 0) {
        book.progressPercent
    } else if (hasStarted) {
        ((currentPage.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderSubtle)
            )
        },
        modifier = Modifier.testTag("shared_book_action_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Resource Header: Cover + Title + Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Thumbnail
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier
                        .size(width = 54.dp, height = 72.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    if (book.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FluentIcons.Book24Filled,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (hasStarted) "PDF · Page $currentPage of $totalPages" else "PDF · ${totalPages} pages",
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                    if (book.subject.isNotBlank()) {
                        Text(
                            text = book.subject,
                            fontFamily = InterFamily,
                            fontSize = 11.5.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Progress Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasStarted) "Reading progress" else "Unread",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "$progressPercent%",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = colors.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.primary,
                    trackColor = colors.borderSubtle
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary CTA: Resume Reading / Open Book
            Button(
                onClick = {
                    onDismiss()
                    onResumeReading(book)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("action_resume_reading"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Icon(
                    imageVector = FluentIcons.BookOpen24Regular,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasStarted) "Resume Reading" else "Open Book",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Actions
            ActionRowItem(
                icon = FluentIcons.ArrowSync24Regular,
                label = "Open from beginning",
                onClick = {
                    onDismiss()
                    onOpenFromBeginning(book)
                }
            )

            ActionRowItem(
                icon = FluentIcons.FolderAdd24Regular,
                label = "Add to collection",
                onClick = {
                    onDismiss()
                    onAddToCollection(book)
                }
            )

            ActionRowItem(
                icon = FluentIcons.Info24Regular,
                label = "View details",
                onClick = {
                    onDismiss()
                    onViewDetails(book)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Destructive Action: Remove from My Library
            ActionRowItem(
                icon = FluentIcons.Delete24Regular,
                label = "Remove from My Library",
                textColor = colors.accentDanger,
                onClick = {
                    onDismiss()
                    onRemoveFromLibrary(book)
                }
            )
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    label: String,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val effectiveColor = if (textColor != Color.Unspecified) textColor else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = effectiveColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontFamily = InterFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = effectiveColor
        )
    }
}
