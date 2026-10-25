package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.components.MulberryExpressiveLogo
import com.example.ui.components.fluidGlassPressEffect
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating Vertical Refractive Glass Navigation Dock for Landscape & Large Screens.
 *
 * Mirrors the exact optical refraction, translucent blurred lens pill, and elastic spring
 * physics of [ElasticGlassBottomBar], oriented vertically on the left side:
 * - Outer Container: Truly floating with zero outer background wall, detached from top/bottom/left edges.
 * - Dock Surface: 70% translucent frosted glass capsule with sweep-gradient prism border & caustic sheen.
 * - Active Lens Pill: Translucent blurred glass pill that glides vertically with Y-axis stretch deformation.
 * - Touch Physics: Standardized 0.94f spring press effect and dual-stage tactile haptics.
 */
@Composable
fun MulberryNavigationRail(
    selectedTab: MulberryTab,
    onTabSelected: (MulberryTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<MulberryTab> = MulberryTab.entries,
    onTabLongClick: (MulberryTab) -> Unit = {}
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    val activeTabs = remember(tabs) { if (tabs.isEmpty()) MulberryTab.entries else tabs }

    // Tab measured vertical positions: Map<MulberryTab, Pair<OffsetYPx, HeightPx>>
    var tabBounds by remember { mutableStateOf(mapOf<MulberryTab, Pair<Float, Float>>()) }

    val currentBounds = tabBounds[selectedTab]
    val targetOffsetY = currentBounds?.first ?: 0f
    val targetHeight = currentBounds?.second ?: 0f

    val offsetYAnimatable = remember { Animatable(0f) }
    val heightAnimatable = remember { Animatable(0f) }
    var hasInitializedTarget by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab, currentBounds) {
        if (currentBounds != null && currentBounds.second > 0f) {
            if (!hasInitializedTarget) {
                offsetYAnimatable.snapTo(currentBounds.first)
                heightAnimatable.snapTo(currentBounds.second)
                hasInitializedTarget = true
            } else {
                coroutineScope.launch {
                    offsetYAnimatable.animateTo(
                        targetValue = currentBounds.first,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                coroutineScope.launch {
                    heightAnimatable.animateTo(
                        targetValue = currentBounds.second,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }

    // Dynamic vertical deformation (stretch) based on transit delta along Y-axis
    val distanceY = abs(offsetYAnimatable.value - targetOffsetY)
    val stretchScaleY = 1f + (distanceY / (targetHeight.coerceAtLeast(64f) * 2.2f)).coerceIn(0f, 0.22f)
    val movingDown = targetOffsetY >= offsetYAnimatable.value

    // Optical refraction chromatic dispersion gradient border (light-bending prism edge)
    val refractivePrismBrush = remember(colors.isDark, colors.primary, colors.borderSubtle) {
        Brush.sweepGradient(
            colors = listOf(
                if (colors.isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.75f),
                colors.primary.copy(alpha = 0.30f),
                if (colors.isDark) Color(0xFF67E8F9).copy(alpha = 0.22f) else Color(0xFF06B6D4).copy(alpha = 0.32f),
                colors.borderSubtle.copy(alpha = 0.35f),
                if (colors.isDark) Color(0xFFFDE047).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.22f),
                if (colors.isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.75f)
            )
        )
    }

    // Caustic internal refraction sheen
    val causticSheenBrush = remember(colors.isDark, colors.primary) {
        Brush.verticalGradient(
            0.0f to (if (colors.isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.48f)),
            0.45f to colors.primary.copy(alpha = 0.08f),
            1.0f to Color.Transparent
        )
    }

    // Translucent glass pill gradient with soft blur depth and specular reflection
    val pillGlassBrush = remember(colors.surfaceCard, colors.surface, colors.primary) {
        Brush.verticalGradient(
            colors = listOf(
                colors.surfaceCard.copy(alpha = 0.65f),
                colors.surface.copy(alpha = 0.45f),
                colors.surfaceCard.copy(alpha = 0.55f)
            )
        )
    }

    val pillGlowBorder = remember(colors.isDark, colors.primary) {
        Brush.verticalGradient(
            colors = listOf(
                if (colors.isDark) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.85f),
                colors.primary.copy(alpha = 0.45f)
            )
        )
    }

    // Floating vertical dock container detached from edges (no outer background wall)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 6.dp, top = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered refractive frosted glass vertical capsule dock (70% opacity with prism border & caustic sheen)
        Box(
            modifier = Modifier
                .width(72.dp)
                .heightIn(min = 248.dp, max = 324.dp)
                .fillMaxHeight(0.76f)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = colors.primary.copy(alpha = 0.22f),
                    ambientColor = Color.Black.copy(alpha = 0.20f)
                )
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.70f), CircleShape)
                .border(1.dp, refractivePrismBrush, CircleShape)
                .drawBehind {
                    // Refractive caustic internal sheen
                    drawRoundRect(
                        brush = causticSheenBrush,
                        size = Size(size.width, size.height * 0.52f),
                        cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // 1. Vertically Sliding Elastic Liquid Pill Indicator with optical blur translucency
                if (hasInitializedTarget) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, offsetYAnimatable.value.roundToInt()) }
                            .height(with(LocalDensity.current) { heightAnimatable.value.toDp() })
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleY = stretchScaleY
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = 0.5f,
                                    pivotFractionY = if (movingDown) 0.20f else 0.80f
                                )
                            }
                            .clip(CircleShape)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = colors.primary.copy(alpha = 0.25f),
                                ambientColor = Color.Black.copy(alpha = 0.10f)
                            )
                            // Translucent blurred glass lens background
                            .background(pillGlassBrush)
                            .border(
                                width = 1.dp,
                                brush = pillGlowBorder,
                                shape = CircleShape
                            )
                            .drawBehind {
                                // Inner specular refraction sheen on top half of pill
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (colors.isDark) 0.22f else 0.55f),
                                            Color.Transparent
                                        )
                                    ),
                                    size = Size(size.width, size.height * 0.48f),
                                    cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
                                )
                            }
                    )
                }

                // 2. Interactive Tabs Column
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    activeTabs.forEach { tab ->
                        val isSelected = tab == selectedTab

                        Box(
                            modifier = Modifier
                                .weight(if (isSelected) 1.35f else 1f)
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .onGloballyPositioned { coordinates ->
                                    val localPos = coordinates.positionInParent()
                                    val size = coordinates.size
                                    tabBounds = tabBounds + (tab to Pair(localPos.y, size.height.toFloat()))
                                }
                                .fluidGlassPressEffect(
                                    onClick = {
                                        if (!isSelected) {
                                            onTabSelected(tab)
                                        }
                                    },
                                    onLongClick = {
                                        onTabLongClick(tab)
                                    }
                                )
                                .testTag(tab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                // Tab Icon (Exact match with ElasticGlassBottomBar)
                                when (tab) {
                                    MulberryTab.LIBRARY -> {
                                        MulberryExpressiveLogo(
                                            size = 24.dp,
                                            showContainer = false
                                        )
                                    }
                                    MulberryTab.DESK -> {
                                        Icon(
                                            imageVector = if (isSelected) FluentIcons.CalendarLtr24Filled else FluentIcons.CalendarLtr24Regular,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.70f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    MulberryTab.SETTINGS -> {
                                        Icon(
                                            imageVector = if (isSelected) FluentIcons.Settings24Filled else FluentIcons.Settings24Regular,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.70f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Active Label Expansion
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = tween(180, delayMillis = 40)) + expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                    exit = fadeOut(animationSpec = tween(100)) + shrinkVertically(
                                        animationSpec = tween(100)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tab.title,
                                            fontFamily = PoppinsFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp,
                                            color = colors.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
