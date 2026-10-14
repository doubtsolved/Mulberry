@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.snips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.local.entities.SnipEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.EphemeralMulberryViewModel
import java.io.File

@Composable
fun SnipsScreen(
    viewModel: EphemeralMulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val allSnips by viewModel.allSnips.collectAsState()
    val allBooks by viewModel.allCatalogBooks.collectAsState()

    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var selectedSnipForDetail by remember { mutableStateOf<SnipEntity?>(null) }

    val distinctSubjects = remember(allSnips) {
        listOf("All") + allSnips.map { it.subject }.filter { it.isNotBlank() }.distinct()
    }

    val filteredSnips = remember(allSnips, selectedSubjectFilter) {
        if (selectedSubjectFilter == null || selectedSubjectFilter == "All") allSnips
        else allSnips.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    // Snip Detail Dialog
    selectedSnipForDetail?.let { snip ->
        SnipDetailDialog(
            snip = snip,
            onDismiss = { selectedSnipForDetail = null },
            onJumpToPage = { targetSnip ->
                val matchedBook = allBooks.find { it.id == targetSnip.bookId }
                if (matchedBook != null) {
                    viewModel.openBookInReader(matchedBook, targetSnip.pageNumber)
                }
            },
            onDelete = { viewModel.deleteSnip(it) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Visual Snips",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Clipped Diagrams • Histology • Flowcharts",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceTint
            ) {
                Text(
                    text = "${allSnips.size} snips",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Subject Filter Row (if there are snips)
        if (distinctSubjects.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(distinctSubjects) { subject ->
                    val isSelected = (selectedSubjectFilter == null && subject == "All") ||
                            selectedSubjectFilter.equals(subject, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSubjectFilter = if (subject == "All") null else subject
                        },
                        label = { Text(subject, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                            containerColor = colors.surfaceCard,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = colors.primary,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // 2-Column Staggered / Masonry-style Grid
        if (filteredSnips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = FluentIcons.Scissors24Regular,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Snips clipped yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "While reading a textbook in the PDF Viewer, tap the Snip tool to crop and save high-yield diagrams directly here.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSnips, key = { it.id }) { snip ->
                    SnipGridCard(
                        snip = snip,
                        onClick = { selectedSnipForDetail = snip }
                    )
                }
            }
        }
    }
}

@Composable
fun SnipGridCard(
    snip: SnipEntity,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Cropped Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(colors.surfaceTint.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(snip.imageLocalPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = snip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                Surface(
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    color = colors.primary.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Pg ${snip.pageNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = snip.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = snip.subject,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
