@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors

@Composable
fun SchedulerBottomSheet(
    downloadedBooks: List<BookEntity>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onScheduleAdded: (bookId: String, bookTitle: String, chapter: String, startPage: Int, endPage: Int) -> Unit
) {
    val colors = LocalMulberryColors.current

    var selectedBook by remember { mutableStateOf(downloadedBooks.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var chapterName by remember { mutableStateOf("") }
    var startPageText by remember { mutableStateOf("1") }
    var endPageText by remember { mutableStateOf("25") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderSubtle)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Add to Today's Focus",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Schedule a chapter reading goal for today's study desk",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Book Selection Dropdown
            Text(
                text = "Select Textbook",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (downloadedBooks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No books downloaded yet. Download books from the Library first.",
                        fontSize = 13.sp,
                        color = colors.textMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedBook?.title ?: "Select a book",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        downloadedBooks.forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book.title, fontSize = 13.sp) },
                                onClick = {
                                    selectedBook = book
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chapter / Topic Input
            Text(
                text = "Target Chapter or Topic",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = chapterName,
                onValueChange = { chapterName = it },
                placeholder = { Text("e.g. Cardiac Cycle & Murmurs", fontSize = 13.sp, color = colors.textMuted) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.surfaceCard,
                    unfocusedContainerColor = colors.surfaceCard,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Page range row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Page",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = startPageText,
                        onValueChange = { startPageText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "End Page",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = endPageText,
                        onValueChange = { endPageText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val book = selectedBook ?: return@Button
                    val start = startPageText.toIntOrNull() ?: 1
                    val end = (endPageText.toIntOrNull() ?: start).coerceAtLeast(start)
                    onScheduleAdded(book.id, book.title, chapterName.ifBlank { "Chapter Reading" }, start, end)
                    onDismiss()
                },
                enabled = selectedBook != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Add to Today's Goals",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
    }
}
