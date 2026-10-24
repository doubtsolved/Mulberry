package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.components.fluidGlassPressEffect
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors

@Composable
fun MulberryNavigationRail(
    selectedTab: MulberryTab,
    onTabSelected: (MulberryTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<MulberryTab> = MulberryTab.entries,
    onTabLongClick: (MulberryTab) -> Unit = {}
) {
    val colors = LocalMulberryColors.current
    val haptic = LocalHapticFeedback.current
    val borderHairline = colors.borderSubtle

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .drawBehind {
                drawLine(
                    color = borderHairline,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.94f else 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / App Logo Indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Book24Filled,
                    contentDescription = "Mulberry",
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Tab Navigation Column
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.92f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "RailTabScale"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) colors.surfaceTint else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "RailPillBg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.textSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "RailContentColor"
                    )

                    Column(
                        modifier = Modifier
                            .testTag(tab.testTag)
                            .clip(RoundedCornerShape(18.dp))
                            .fluidGlassPressEffect(
                                onClick = { onTabSelected(tab) },
                                onLongClick = { onTabLongClick(tab) }
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val isLibraryTab = tab == MulberryTab.LIBRARY
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(if (isLibraryTab) 42.dp else 36.dp)
                                .clip(RoundedCornerShape(if (isLibraryTab) 21.dp else 18.dp))
                                .background(pillBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLibraryTab) {
                                com.example.ui.components.MulberryExpressiveLogo(
                                    size = 28.dp,
                                    showContainer = false
                                )
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.filledIcon else tab.regularIcon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (!isLibraryTab) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tab.title,
                                fontFamily = InterFamily,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            // Bottom placeholder to balance layout
            Spacer(modifier = Modifier.height(42.dp))
        }
    }
}
