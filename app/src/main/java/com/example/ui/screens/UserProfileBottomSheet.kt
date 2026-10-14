package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileBottomSheet(
    userProfile: UserProfile,
    onDismissRequest: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var academicRole by remember(userProfile) { mutableStateOf(userProfile.academicRole) }
    var targetExamOrSubject by remember(userProfile) { mutableStateOf(userProfile.targetExamOrSubject) }
    var institution by remember(userProfile) { mutableStateOf(userProfile.institution) }
    var dailyPageGoalText by remember(userProfile) { mutableStateOf(userProfile.dailyPageGoal.toString()) }
    var selectedProf by remember(userProfile) { mutableStateOf(userProfile.selectedProf) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edit Profile",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Personalize your study workspace and targets",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            // MBBS Professional Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MBBS Professional Year",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Configures your primary bookshelf, subjects, and study feed",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MBBS_CURRICULUM.forEach { prof ->
                        val isSelected = selectedProf.equals(prof.id, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.10f) else colors.surfaceCard,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.borderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedProf = prof.id }
                                .testTag("prof_select_${prof.id.replace(" ", "_").lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedProf = prof.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primary,
                                        unselectedColor = colors.textMuted
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prof.title,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) colors.primary else colors.textPrimary
                                    )
                                    Text(
                                        text = prof.subjects.joinToString(" • "),
                                        fontFamily = InterFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.borderSubtle,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = colors.primary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )

            // Field 1: Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                placeholder = { Text("e.g., Sahabul") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            // Field 2: Academic Stage / Role
            OutlinedTextField(
                value = academicRole,
                onValueChange = { academicRole = it },
                label = { Text("Academic Stage / Role") },
                placeholder = { Text("e.g., Medical Student, Aspirant") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_role_input")
            )

            // Field 3: Primary Subject / Exam Focus
            OutlinedTextField(
                value = targetExamOrSubject,
                onValueChange = { targetExamOrSubject = it },
                label = { Text("Primary Subject / Exam Focus") },
                placeholder = { Text("e.g., MBBS 1st Year, Anatomy & Biochem") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_subject_input")
            )

            // Field 4: Institution / College (Optional)
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution / College (Optional)") },
                placeholder = { Text("e.g., Medical College") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_institution_input")
            )

            // Field 5: Daily Page Target
            OutlinedTextField(
                value = dailyPageGoalText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        dailyPageGoalText = input
                    }
                },
                label = { Text("Daily Page Target") },
                placeholder = { Text("e.g., 20") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_goal_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Actions: Cancel & Save Changes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.testTag("profile_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = InterFamily,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val parsedGoal = dailyPageGoalText.toIntOrNull()?.coerceAtLeast(1) ?: 20
                        val updated = userProfile.copy(
                            displayName = displayName.trim().ifEmpty { "Sahabul" },
                            academicRole = academicRole.trim().ifEmpty { "Medical Student" },
                            targetExamOrSubject = targetExamOrSubject.trim().ifEmpty { "MBBS 1st Year" },
                            institution = institution.trim(),
                            dailyPageGoal = parsedGoal,
                            selectedProf = selectedProf
                        )
                        coroutineScope.launch {
                            onSaveProfile(updated)
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    modifier = Modifier.testTag("profile_save_button")
                ) {
                    Text(
                        text = "Save Changes",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
