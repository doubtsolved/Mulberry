package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.repository.AnnouncementBanner
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.delay

private val DefaultFallbackBgColor = Color(0xFF4A2511)
private val DefaultFallbackTextColor = Color(0xFFFFFFFF)

/**
 * Interactive, auto-advancing 16:6 Announcement Carousel for Mulberry Shelf (Library Home).
 *
 * - Enforces a strict 16:6 aspect ratio with 16.dp horizontal padding and 16.dp rounded corners.
 * - Auto-advances every 5,000 ms when more than 1 banner is active, pausing while touched/scrolled.
 * - Renders compact pill indicator dots at the bottom-end corner inside the banner overlay.
 */
@Composable
fun MulberryAnnouncementCarousel(
    banners: List<AnnouncementBanner>,
    onBannerClick: (AnnouncementBanner) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val colors = LocalMulberryColors.current
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { banners.size }
    )

    // Keep currentPage in bounds if banner count shrinks on Prof switch
    LaunchedEffect(banners.size) {
        if (pagerState.currentPage >= banners.size && banners.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    // Auto-advance every 5,000 ms when > 1 active banner; pause while user is touching/scrolling
    LaunchedEffect(banners.size, pagerState.isScrollInProgress, pagerState.settledPage) {
        if (banners.size > 1 && !pagerState.isScrollInProgress) {
            delay(5_000L)
            if (!pagerState.isScrollInProgress && banners.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                runCatching {
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }
    }

    val bannerShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .aspectRatio(16f / 6f)
            .clip(bannerShape)
            .border(
                width = 1.dp,
                color = colors.borderSubtle,
                shape = bannerShape
            )
            .testTag("mulberry_announcement_carousel")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { index -> banners.getOrNull(index)?.id?.ifEmpty { "banner_$index" } ?: "banner_$index" }
        ) { pageIndex ->
            val banner = banners.getOrNull(pageIndex) ?: return@HorizontalPager
            AnnouncementBannerCard(
                banner = banner,
                hasMultipleBanners = banners.size > 1,
                onClick = { onBannerClick(banner) }
            )
        }

        // Bottom-End Page Indicator Dots inside the banner overlay
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
                    .testTag("announcement_carousel_indicators"),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 6.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "carousel_dot_width"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.42f),
                        animationSpec = tween(durationMillis = 200),
                        label = "carousel_dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBannerCard(
    banner: AnnouncementBanner,
    hasMultipleBanners: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val parsedBgColor = remember(banner.bgColor) {
        parseHexColorSafely(banner.bgColor, DefaultFallbackBgColor)
    }

    val parsedTextColor = remember(banner.textColor) {
        parseHexColorSafely(banner.textColor, DefaultFallbackTextColor)
    }

    val effectiveImageUrl = remember(banner.imageUrl, banner.updatedAt) {
        val raw = banner.imageUrl.trim()
        if (raw.isEmpty()) {
            ""
        } else if (banner.updatedAt > 0L) {
            if (raw.contains("?")) "$raw&v=${banner.updatedAt}" else "$raw?v=${banner.updatedAt}"
        } else {
            raw
        }
    }

    val scrimGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.32f),
                Color.Black.copy(alpha = 0.78f)
            )
        )
    }

    val isActionable = remember(banner.action.type) {
        !banner.action.type.trim().equals("NONE", ignoreCase = true) &&
            banner.action.type.isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parsedBgColor)
            .then(
                if (isActionable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag("announcement_banner_${banner.id.ifEmpty { "item" }}")
    ) {
        // 1. Full-bleed WebP Image with Coil Disk & Memory Caching
        if (effectiveImageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(effectiveImageUrl)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Smooth Bottom-to-Top Dark Scrim Gradient for Crisp Text Legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimGradient)
        )

        // 3. Typography & Badge Overlay (Bottom-Start aligned, 14.dp padding)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp,
                    // Reserve horizontal room on the right so text never collides with indicator dots
                    end = if (hasMultipleBanners) 76.dp else 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (banner.badge.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.22f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.38f))
                ) {
                    Text(
                        text = banner.badge.uppercase(),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                        color = parsedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (banner.title.isNotBlank()) {
                Text(
                    text = banner.title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = parsedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (banner.subtitle.isNotBlank()) {
                Text(
                    text = banner.subtitle,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = parsedTextColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

internal fun parseHexColorSafely(hexString: String, fallback: Color): Color {
    val cleaned = hexString.trim()
    if (cleaned.isEmpty()) return fallback
    return try {
        val formatted = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
        Color(android.graphics.Color.parseColor(formatted))
    } catch (_: Exception) {
        fallback
    }
}
