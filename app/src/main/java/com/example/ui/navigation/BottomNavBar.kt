package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.components.GlassVariant
import com.example.ui.components.MulberryExpressiveLogo
import com.example.ui.components.fluidGlassPressEffect
import com.example.ui.components.glassEffect
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.MulberryTheme
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Three core tabs for the Mulberry workspace.
 */
enum class DeskTab(val title: String) {
    DESK("Desk"),
    LIBRARY("Library"),
    SETTINGS("Settings");

    fun toMulberryTab(): MulberryTab = when (this) {
        DESK -> MulberryTab.DESK
        LIBRARY -> MulberryTab.LIBRARY
        SETTINGS -> MulberryTab.SETTINGS
    }

    companion object {
        fun fromMulberryTab(tab: MulberryTab): DeskTab = when (tab) {
            MulberryTab.DESK -> DESK
            MulberryTab.LIBRARY -> LIBRARY
            MulberryTab.SETTINGS -> SETTINGS
        }
    }
}

/**
 * Legacy MulberryTab representation retained for backward-compatibility.
 */
enum class MulberryTab(
    val title: String,
    val testTag: String,
    val regularIcon: ImageVector,
    val filledIcon: ImageVector
) {
    DESK(
        title = "Desk",
        testTag = "nav_tab_desk",
        regularIcon = FluentIcons.CalendarLtr24Regular,
        filledIcon = FluentIcons.CalendarLtr24Filled
    ),
    LIBRARY(
        title = "Library",
        testTag = "nav_tab_library",
        regularIcon = FluentIcons.Book24Regular,
        filledIcon = FluentIcons.Book24Filled
    ),
    SETTINGS(
        title = "Settings",
        testTag = "nav_tab_settings",
        regularIcon = FluentIcons.Settings24Regular,
        filledIcon = FluentIcons.Settings24Filled
    )
}

data class NavTabConfig(
    val tab: MulberryTab,
    val isVisible: Boolean = true
)

val DEFAULT_TAB_CONFIGS = listOf(
    NavTabConfig(MulberryTab.DESK, isVisible = true),
    NavTabConfig(MulberryTab.LIBRARY, isVisible = true),
    NavTabConfig(MulberryTab.SETTINGS, isVisible = true)
)

/**
 * High-fidelity "Elastic Liquid Pill" floating glass dock in Jetpack Compose.
 *
 * Strictly adapts to Mulberry's 6 custom themes:
 * - Dock Surface: Translucent frosted glass effect (70% opacity) with specular highlight and ambient shadow.
 * - Active Pill: Theme surfaceCard background with subtle primary outline border.
 * - Icons & Labels: Active colored in theme primary, inactive in theme textSecondary.
 * - Spring Physics: Medium bouncy spring with fluid stretch deformation during transit.
 */
@Composable
fun ElasticGlassBottomBar(
    selectedTab: DeskTab,
    onTabSelected: (DeskTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<DeskTab> = DeskTab.entries,
    onTabLongClick: (DeskTab) -> Unit = {}
) {
    val colors = LocalMulberryColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val activeTabs = remember(tabs) { if (tabs.isEmpty()) DeskTab.entries else tabs }

    // Tab measured positions: Map<DeskTab, Pair<OffsetXPx, WidthPx>>
    var tabBounds by remember { mutableStateOf(mapOf<DeskTab, Pair<Float, Float>>()) }

    val currentBounds = tabBounds[selectedTab]
    val targetOffset = currentBounds?.first ?: 0f
    val targetWidth = currentBounds?.second ?: 0f

    val offsetAnimatable = remember { Animatable(0f) }
    val widthAnimatable = remember { Animatable(0f) }
    var hasInitializedTarget by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab, currentBounds) {
        if (currentBounds != null) {
            if (!hasInitializedTarget) {
                offsetAnimatable.snapTo(currentBounds.first)
                widthAnimatable.snapTo(currentBounds.second)
                hasInitializedTarget = true
            } else {
                coroutineScope.launch {
                    offsetAnimatable.animateTo(
                        targetValue = currentBounds.first,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                coroutineScope.launch {
                    widthAnimatable.animateTo(
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

    // Dynamic horizontal deformation (stretch) based on transit delta
    val distance = kotlin.math.abs(offsetAnimatable.value - targetOffset)
    val stretchScaleX = 1f + (distance / (targetWidth.coerceAtLeast(80f) * 2.2f)).coerceIn(0f, 0.22f)
    val movingRight = targetOffset >= offsetAnimatable.value

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

    // Floating dock container with 16.dp horizontal & 12.dp bottom margin (floating freely above system navigation bars)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered refractive frosted glass dock surface (70% opacity with prism border & caustic light sheen)
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(64.dp)
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
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // 1. Sliding Elastic Liquid Pill Indicator with optical blur translucency
                if (hasInitializedTarget) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetAnimatable.value.roundToInt(), 0) }
                            .width(with(LocalDensity.current) { widthAnimatable.value.toDp() })
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = stretchScaleX
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (movingRight) 0.20f else 0.80f,
                                    pivotFractionY = 0.5f
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
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.48f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                                )
                            }
                    )
                }

                // 2. Interactive Tabs Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeTabs.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val mulberryTab = tab.toMulberryTab()

                        Box(
                            modifier = Modifier
                                .weight(if (isSelected) 1.35f else 1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .onGloballyPositioned { coordinates ->
                                    val localPos = coordinates.positionInParent()
                                    val size = coordinates.size
                                    tabBounds = tabBounds + (tab to Pair(localPos.x, size.width.toFloat()))
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
                                .testTag(mulberryTab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                // Tab Icon
                                when (tab) {
                                    DeskTab.LIBRARY -> {
                                        MulberryExpressiveLogo(
                                            size = 24.dp,
                                            showContainer = false
                                        )
                                    }
                                    DeskTab.DESK -> {
                                        Icon(
                                            imageVector = if (isSelected) FluentIcons.CalendarLtr24Filled else FluentIcons.CalendarLtr24Regular,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.70f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DeskTab.SETTINGS -> {
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
                                    enter = fadeIn(animationSpec = tween(180, delayMillis = 40)) + expandHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                    exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                                        animationSpec = tween(100)
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            fontFamily = PoppinsFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = colors.primary,
                                            maxLines = 1
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

/**
 * Backward-compatible wrapper mapping MulberryTab to the new ElasticGlassBottomBar dock.
 */
@Composable
fun MulberryBottomNavBar(
    selectedTab: MulberryTab,
    onTabSelected: (MulberryTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<MulberryTab> = MulberryTab.entries,
    onTabLongClick: (MulberryTab) -> Unit = {}
) {
    val deskTabs = remember(tabs) { tabs.map { DeskTab.fromMulberryTab(it) } }
    ElasticGlassBottomBar(
        selectedTab = DeskTab.fromMulberryTab(selectedTab),
        onTabSelected = { deskTab -> onTabSelected(deskTab.toMulberryTab()) },
        modifier = modifier,
        tabs = deskTabs,
        onTabLongClick = { deskTab -> onTabLongClick(deskTab.toMulberryTab()) }
    )
}

@Preview(name = "Elastic Glass Bottom Bar - Scaffold", showBackground = true)
@Composable
fun ElasticGlassBottomBarPreview() {
    MulberryTheme {
        var selected by remember { mutableStateOf(DeskTab.DESK) }
        Scaffold(
            bottomBar = {
                ElasticGlassBottomBar(
                    selectedTab = selected,
                    onTabSelected = { selected = it }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Current: ${selected.title}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
