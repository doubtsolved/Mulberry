package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MulberryTab
import com.example.ui.navigation.NavTabConfig
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.MulberryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeTabsSheet(
    viewModel: MulberryViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val tabConfigs by viewModel.tabConfigs.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visibleCount = tabConfigs.count { it.isVisible }
    val itemHeightPx = with(density) { 76.dp.toPx() }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle.copy(alpha = 0.7f))
            )
        },
        modifier = modifier.testTag("customize_tabs_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = FluentIcons.Navigation24Regular,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Customize Navigation",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Drag handle to reorder • Toggle to show or hide",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                // Reset Action Button
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.resetTabsToDefault()
                    },
                    modifier = Modifier.testTag("reset_tabs_button")
                ) {
                    Icon(
                        imageVector = FluentIcons.ArrowSync24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                }
            }

            // Live Preview Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_bar_live_preview"),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = colors.primary
                        )
                        Text(
                            text = "$visibleCount of ${tabConfigs.size} tabs visible",
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    // Mini Bottom Bar Replica
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeList = tabConfigs.filter { it.isVisible }
                            activeList.forEachIndexed { idx, config ->
                                val isFirst = idx == 0
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(if (isFirst) colors.surfaceTint else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFirst) config.tab.filledIcon else config.tab.regularIcon,
                                            contentDescription = config.tab.title,
                                            tint = if (isFirst) colors.primary else colors.textSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = config.tab.title,
                                        fontFamily = InterFamily,
                                        fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = if (isFirst) colors.primary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab Reorder List
            Text(
                text = "TABS ORDER & VISIBILITY",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabConfigs.forEachIndexed { index, config ->
                    val isBeingDragged = draggedIndex == index
                    val isLibrary = config.tab == MulberryTab.LIBRARY
                    val canHide = !isLibrary && visibleCount > 2

                    val translationY = if (isBeingDragged) dragOffsetY else 0f
                    val animatedElevation by animateFloatAsState(
                        targetValue = if (isBeingDragged) 10f else 0f,
                        label = "cardElevation"
                    )
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isBeingDragged) 1.025f else 1f,
                        label = "cardScale"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isBeingDragged) 5f else 0f)
                            .graphicsLayer {
                                this.translationY = translationY
                                this.scaleX = animatedScale
                                this.scaleY = animatedScale
                                this.shadowElevation = animatedElevation
                            }
                            .testTag("tab_config_item_${config.tab.name}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isBeingDragged) colors.surfaceCard else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isBeingDragged) 1.5.dp else 1.dp,
                            color = if (isBeingDragged) colors.primary else colors.borderSubtle.copy(alpha = 0.8f)
                        ),
                        shadowElevation = if (isBeingDragged) 8.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle Grip
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isBeingDragged) colors.primary.copy(alpha = 0.15f) else colors.surfaceTint.copy(alpha = 0.6f))
                                    .pointerInput(tabConfigs) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y

                                                val currentIdx = draggedIndex ?: return@detectDragGestures
                                                val threshold = itemHeightPx * 0.6f

                                                if (dragOffsetY > threshold && currentIdx < tabConfigs.lastIndex) {
                                                    val targetIdx = currentIdx + 1
                                                    draggedIndex = targetIdx
                                                    dragOffsetY -= itemHeightPx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.reorderTabs(currentIdx, targetIdx)
                                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                    val targetIdx = currentIdx - 1
                                                    draggedIndex = targetIdx
                                                    dragOffsetY += itemHeightPx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.reorderTabs(currentIdx, targetIdx)
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                                    .testTag("drag_handle_${config.tab.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Reorder24Regular,
                                    contentDescription = "Drag to reorder ${config.tab.title}",
                                    tint = if (isBeingDragged) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Tab Icon badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (config.isVisible) colors.surfaceTint else colors.borderSubtle.copy(alpha = 0.3f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (config.isVisible) config.tab.filledIcon else config.tab.regularIcon,
                                        contentDescription = null,
                                        tint = if (config.isVisible) colors.primary else colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Description
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.tab.title,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (config.isVisible) colors.textPrimary else colors.textMuted
                                )
                                Text(
                                    text = when {
                                        isLibrary -> "Required • Core anchor"
                                        config.isVisible -> "Visible in navigation"
                                        else -> "Hidden from navigation"
                                    },
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = if (isLibrary) colors.primary else colors.textSecondary
                                )
                            }

                            // Quick Up / Down Reorder Arrows
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.reorderTabs(index, index - 1)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("move_up_${config.tab.name}")
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.ChevronUp24Regular,
                                        contentDescription = "Move ${config.tab.title} Up",
                                        tint = if (index > 0) colors.textSecondary else colors.textMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (index < tabConfigs.lastIndex) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.reorderTabs(index, index + 1)
                                        }
                                    },
                                    enabled = index < tabConfigs.lastIndex,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("move_down_${config.tab.name}")
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.ChevronDown24Regular,
                                        contentDescription = "Move ${config.tab.title} Down",
                                        tint = if (index < tabConfigs.lastIndex) colors.textSecondary else colors.textMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Switch or Locked Chip
                            if (isLibrary) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = colors.primary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = FluentIcons.Lock24Regular,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Locked",
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = colors.primary
                                        )
                                    }
                                }
                            } else {
                                Switch(
                                    checked = config.isVisible,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked || canHide) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleTabVisibility(config.tab, isChecked)
                                        }
                                    },
                                    enabled = config.isVisible.not() || canHide,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.primary,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.surfaceCard
                                    ),
                                    modifier = Modifier.testTag("toggle_visibility_${config.tab.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Explanatory note if minimum tabs reached
            AnimatedVisibility(
                visible = visibleCount <= 2,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FluentIcons.Info24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "A minimum of 2 tabs must remain visible so navigation is always accessible.",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("done_customize_tabs_button")
            ) {
                Icon(
                    imageVector = FluentIcons.Checkmark24Regular,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Done",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
