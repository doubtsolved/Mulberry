package com.example.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

/**
 * Expandable 2-state bottom sheet for broad thumbnail overview:
 * - Opens initially at ~50% height (partially expanded) with a 3-column grid
 * - Can be dragged up to full screen for broad visual browsing
 * - Displays page previews, highlights the active page, and provides instant click-to-jump
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailBottomSheet(
    sheetState: SheetState,
    totalPages: Int,
    currentPageIndex: Int,
    thumbnailManager: PdfThumbnailManager,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        ThumbnailSheetContent(
            totalPages = totalPages,
            currentPageIndex = currentPageIndex,
            thumbnailManager = thumbnailManager,
            onPageSelected = onPageSelected,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ThumbnailSheetContent(
    totalPages: Int,
    currentPageIndex: Int,
    thumbnailManager: PdfThumbnailManager,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val gridState = rememberLazyGridState()

    // Automatically scroll to the row containing the currently active page
    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex in 0 until totalPages) {
            val targetRow = (currentPageIndex / 3).coerceAtLeast(0)
            gridState.scrollToItem(targetRow * 3)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Page Thumbnails",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "$totalPages Pages • Tap any page to jump",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Thumbnails",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3-Column Thumbnail Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(totalPages, key = { it }) { pageIdx ->
                val isCurrent = pageIdx == currentPageIndex

                ThumbnailCard(
                    pageIndex = pageIdx,
                    isCurrent = isCurrent,
                    thumbnailManager = thumbnailManager,
                    onClick = {
                        onPageSelected(pageIdx)
                    }
                )
            }
        }
    }
}

@Composable
fun ThumbnailCard(
    pageIndex: Int,
    isCurrent: Boolean,
    thumbnailManager: PdfThumbnailManager,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Thumbnail Image Surface with aspect ratio and active border
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceTint,
            border = BorderStroke(
                width = if (isCurrent) 2.5.dp else 1.dp,
                color = if (isCurrent) colors.primary else colors.borderSubtle
            ),
            shadowElevation = if (isCurrent) 4.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f) // Standard A4 / ISO 216 page aspect ratio
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncPageThumbnail(
                    pageIndex = pageIndex,
                    thumbnailManager = thumbnailManager,
                    modifier = Modifier.fillMaxSize()
                )

                // Active page badge pill
                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        color = colors.primary,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "Current",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Page number label
        Text(
            text = "Page ${pageIndex + 1}",
            fontFamily = InterFamily,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.5.sp,
            color = if (isCurrent) colors.primary else colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
