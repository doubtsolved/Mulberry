package com.example.ui.screens.desk.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

@Composable
fun GoldenStreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val isDark = colors.isDark

    // Golden warm container colors
    val containerColor = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
    val borderColor = if (isDark) Color(0xFFB45309).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.6f)
    val flameColor = Color(0xFFD97706)
    val textColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("desk_golden_streak_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = FluentIcons.Flame24Filled,
                contentDescription = "Streak",
                tint = flameColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$streakDays ${if (streakDays == 1) "Day" else "Days"}",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = textColor,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
