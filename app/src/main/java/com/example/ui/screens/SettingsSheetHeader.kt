package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

@Composable
fun SettingsSheetHeader(
    title: String,
    tooltipText: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    var showTooltip by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top center drag handle pill (32.dp x 4.dp)
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.borderSubtle)
        )

        // Row with Title and optional Info tooltip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = colors.textPrimary,
                modifier = Modifier.testTag("sheet_header_title")
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!tooltipText.isNullOrBlank()) {
                IconButton(
                    onClick = { showTooltip = !showTooltip },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("sheet_info_button")
                ) {
                    Icon(
                        imageVector = FluentIcons.Info24Regular,
                        contentDescription = "Sheet Information",
                        tint = if (showTooltip) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Animated Tooltip card
        if (!tooltipText.isNullOrBlank()) {
            AnimatedVisibility(
                visible = showTooltip,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = tooltipText,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
