package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors

enum class MulberryTab(
    val title: String,
    val testTag: String,
    val regularIcon: ImageVector,
    val filledIcon: ImageVector
) {
    LIBRARY(
        title = "Library",
        testTag = "nav_tab_library",
        regularIcon = FluentIcons.Book24Regular,
        filledIcon = FluentIcons.Book24Filled
    ),
    SNIPS(
        title = "Snips",
        testTag = "nav_tab_snips",
        regularIcon = FluentIcons.Scissors24Regular,
        filledIcon = FluentIcons.Scissors24Filled
    ),
    STUDY(
        title = "Study",
        testTag = "nav_tab_study",
        regularIcon = FluentIcons.TaskListSquareLtr24Regular,
        filledIcon = FluentIcons.TaskListSquareLtr24Filled
    ),
    SETTINGS(
        title = "Settings",
        testTag = "nav_tab_settings",
        regularIcon = FluentIcons.Settings24Regular,
        filledIcon = FluentIcons.Settings24Filled
    )
}

@Composable
fun MulberryBottomNavBar(
    selectedTab: MulberryTab,
    onTabSelected: (MulberryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val borderHairline = colors.borderSubtle

    // Ultra-fine 0.5.dp top hairline border + translucent frosted backdrop
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderHairline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.92f else 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MulberryTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "TabPillScale"
                )

                val pillBgColor by animateColorAsState(
                    targetValue = if (isSelected) colors.surfaceTint else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabPillBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.textSecondary,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabContentColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tab.testTag)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pill Indicator: 34.dp height, 56.dp width, 18.dp radius
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.filledIcon else tab.regularIcon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        color = contentColor,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
