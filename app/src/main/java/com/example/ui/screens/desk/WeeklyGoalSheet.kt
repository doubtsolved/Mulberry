@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors

@Composable
fun WeeklyGoalSheet(
    sheetState: SheetState,
    currentGoal: Int,
    onSaveGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current
    var sliderValue by remember(currentGoal) { mutableFloatStateOf(currentGoal.toFloat().coerceIn(50f, 1000f)) }
    val displayPages = sliderValue.toInt()

    val presets = remember { listOf(100, 200, 350, 500) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
        modifier = Modifier.testTag("weekly_goal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weekly Reading Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Target: $displayPages pages / week",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Text(
                text = "Pace yourself with realistic milestones for textbook chapters.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = displayPages == preset
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary else colors.surfaceTint,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { sliderValue = preset.toFloat() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$preset p",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.onPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive M3 Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = (it / 10).toInt() * 10f },
                valueRange = 50f..1000f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceTint
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "50 pages", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text(text = "1,000 pages", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onSaveGoal(displayPages)
                    onDismiss()
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_weekly_goal")
            ) {
                Text(
                    text = "Save Goal ($displayPages pages)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
