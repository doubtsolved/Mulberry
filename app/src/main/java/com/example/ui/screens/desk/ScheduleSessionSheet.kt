@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.data.schedule.model.ReadingSession
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DateQuickChoice {
    TODAY,
    TOMORROW,
    CUSTOM
}

enum class ScheduleSheetStep {
    CONFIG,
    BOOK_SEARCH
}

@Composable
fun ScheduleSessionSheet(
    sheetState: SheetState,
    vaultBooks: List<BookEntity>,
    existingSession: ReadingSession? = null,
    onDismiss: () -> Unit,
    onSchedule: (
        book: BookEntity,
        startPage: Int,
        endPage: Int,
        scheduledEpochMillis: Long,
        reminderMinutesBefore: Int,
        microNote: String
    ) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current

    var currentStep by remember { mutableStateOf(ScheduleSheetStep.CONFIG) }
    var bookSearchQuery by remember { mutableStateOf("") }

    var selectedBook by remember {
        mutableStateOf(
            if (existingSession != null) {
                vaultBooks.find { (it.slug.isNotBlank() && it.slug == existingSession.bookSlug) || it.title == existingSession.bookTitle || it.fileName == existingSession.bookFileName }
                    ?: vaultBooks.firstOrNull()
            } else {
                vaultBooks.firstOrNull()
            }
        )
    }

    var startPageText by remember {
        mutableStateOf(
            existingSession?.startPage?.toString()
                ?: (selectedBook?.let { "${it.lastReadPage.coerceAtLeast(1)}" } ?: "1")
        )
    }
    var endPageText by remember {
        mutableStateOf(
            existingSession?.endPage?.toString()
                ?: (selectedBook?.let { "${(it.lastReadPage + 15).coerceAtMost(if (it.pageCount > 1) it.pageCount else 100)}" } ?: "16")
        )
    }

    var dateChoice by remember {
        mutableStateOf(if (existingSession != null) DateQuickChoice.CUSTOM else DateQuickChoice.TODAY)
    }
    var customDateEpochMillis by remember {
        mutableLongStateOf(existingSession?.scheduledEpochMillis ?: System.currentTimeMillis())
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var selectedHour by remember {
        val cal = Calendar.getInstance().apply {
            if (existingSession != null) timeInMillis = existingSession.scheduledEpochMillis
            else add(Calendar.HOUR_OF_DAY, 1)
        }
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember {
        val cal = Calendar.getInstance().apply {
            if (existingSession != null) timeInMillis = existingSession.scheduledEpochMillis
        }
        mutableIntStateOf(if (existingSession != null) cal.get(Calendar.MINUTE) else 0)
    }

    var reminderMinutesBefore by remember {
        mutableIntStateOf(existingSession?.reminderMinutesBefore ?: 15)
    }
    var showReminderPickerSheet by remember { mutableStateOf(false) }

    var microNote by remember { mutableStateOf(existingSession?.microNote ?: "") }

    val startPage = startPageText.toIntOrNull() ?: 1
    val endPage = endPageText.toIntOrNull() ?: startPage
    val totalPages = (endPage - startPage + 1).coerceAtLeast(1)
    val estMinutes = totalPages * 2

    // Date calculations
    val scheduledCalendar = remember(dateChoice, customDateEpochMillis, selectedHour, selectedMinute) {
        Calendar.getInstance().apply {
            when (dateChoice) {
                DateQuickChoice.TODAY -> {
                    // today
                }
                DateQuickChoice.TOMORROW -> {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                DateQuickChoice.CUSTOM -> {
                    timeInMillis = customDateEpochMillis
                }
            }
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(selectedHour, selectedMinute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        timeFormat.format(cal.time)
    }

    // Intercept back button when inside dedicated book search view
    BackHandler(enabled = currentStep == ScheduleSheetStep.BOOK_SEARCH) {
        currentStep = ScheduleSheetStep.CONFIG
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
        modifier = Modifier.testTag("schedule_session_sheet")
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScheduleSheetStepAnimation"
        ) { step ->
            when (step) {
                ScheduleSheetStep.BOOK_SEARCH -> {
                    DedicatedBookSearchView(
                        vaultBooks = vaultBooks,
                        searchQuery = bookSearchQuery,
                        selectedBook = selectedBook,
                        onSearchQueryChange = { bookSearchQuery = it },
                        onBookSelected = { book ->
                            selectedBook = book
                            if (existingSession == null) {
                                startPageText = "${book.lastReadPage.coerceAtLeast(1)}"
                                endPageText = "${(book.lastReadPage + 15).coerceAtMost(if (book.pageCount > 1) book.pageCount else 100)}"
                            }
                            currentStep = ScheduleSheetStep.CONFIG
                        },
                        onBack = { currentStep = ScheduleSheetStep.CONFIG }
                    )
                }
                ScheduleSheetStep.CONFIG -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (existingSession != null) "Edit Reading Session" else "Schedule Reading Session",
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Target Textbook Selector (Expressive Card triggering search)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target Textbook",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            if (vaultBooks.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        bookSearchQuery = ""
                                        currentStep = ScheduleSheetStep.BOOK_SEARCH
                                    }
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Search24Regular,
                                        contentDescription = "Search",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Search all books",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        if (vaultBooks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceTint.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No PDF textbooks detected in this vault. Add PDFs to schedule sessions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceTint,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        bookSearchQuery = ""
                                        currentStep = ScheduleSheetStep.BOOK_SEARCH
                                    }
                                    .testTag("btn_select_book")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = FluentIcons.Book24Filled,
                                            contentDescription = "Book",
                                            tint = colors.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedBook?.title ?: "Select a book...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val subtitle = if ((selectedBook?.pageCount ?: 0) > 1) {
                                            "${selectedBook?.pageCount} pages • Last read p. ${selectedBook?.lastReadPage ?: 1}"
                                        } else {
                                            "Last read p. ${selectedBook?.lastReadPage ?: 1}"
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = FluentIcons.Search24Regular,
                                        contentDescription = "Change book",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. Page Range Fields
                        Text(
                            text = "Page Range",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = startPageText,
                                onValueChange = { startPageText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Start Page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_start_page")
                            )

                            Text(
                                text = "to",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )

                            OutlinedTextField(
                                value = endPageText,
                                onValueChange = { endPageText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("End Page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_end_page")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Info24Regular,
                                contentDescription = "Estimated reading time",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalPages pages total (~$estMinutes min estimated reading time)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Date Quick Pills
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DateQuickChoice.entries.forEach { choice ->
                                val isSelected = choice == dateChoice
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) colors.primary else colors.surfaceTint,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable {
                                            dateChoice = choice
                                            if (choice == DateQuickChoice.CUSTOM) {
                                                showDatePickerDialog = true
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (choice == DateQuickChoice.CUSTOM) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = FluentIcons.CalendarLtr24Regular,
                                                    contentDescription = "Custom Date",
                                                    tint = if (isSelected) colors.onPrimary else colors.textPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Custom",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) colors.onPrimary else colors.textPrimary
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = when (choice) {
                                                    DateQuickChoice.TODAY -> "Today"
                                                    DateQuickChoice.TOMORROW -> "Tomorrow"
                                                    DateQuickChoice.CUSTOM -> "Custom"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) colors.onPrimary else colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4. Start Time & Reminder Controls
                        Text(
                            text = "Start Time & Reminder",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Time Button (Opens Native TimePickerDialog)
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            selectedHour = h
                                            selectedMinute = m
                                        },
                                        selectedHour,
                                        selectedMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Clock24Regular,
                                    contentDescription = "Time",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }

                            // Expressive Reminder Offset Selector
                            OutlinedButton(
                                onClick = { showReminderPickerSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(52.dp)
                                    .testTag("btn_select_reminder")
                            ) {
                                val hasReminder = reminderMinutesBefore >= 0
                                Icon(
                                    imageVector = if (hasReminder) FluentIcons.Alert24Regular else FluentIcons.AlertOff24Regular,
                                    contentDescription = "Reminder",
                                    tint = if (hasReminder) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val label = when (reminderMinutesBefore) {
                                    0 -> "At start"
                                    15 -> "15m before"
                                    30 -> "30m before"
                                    60 -> "1h before"
                                    -1 -> "None"
                                    else -> "${reminderMinutesBefore}m before"
                                }
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 5. Micro-Note Input
                        Text(
                            text = "Session Focus (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = microNote,
                            onValueChange = { microNote = it },
                            placeholder = { Text("e.g., Aortic valve murmurs & apex beat") },
                            leadingIcon = {
                                Icon(
                                    imageVector = FluentIcons.Tag24Regular,
                                    contentDescription = "Focus",
                                    tint = colors.primary
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.borderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_micro_note")
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // 6. Submit Button
                        Button(
                            onClick = {
                                val book = selectedBook ?: return@Button
                                val finalStart = startPage.coerceAtLeast(1)
                                val finalEnd = endPage.coerceAtLeast(finalStart)
                                onSchedule(
                                    book,
                                    finalStart,
                                    finalEnd,
                                    scheduledCalendar.timeInMillis,
                                    reminderMinutesBefore,
                                    microNote
                                )
                                onDismiss()
                            },
                            enabled = selectedBook != null,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_submit_schedule")
                        ) {
                            Icon(
                                imageVector = FluentIcons.CalendarAdd24Regular,
                                contentDescription = "Schedule",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (existingSession != null) "Update Session" else "Schedule Session",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDateEpochMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            customDateEpochMillis = millis
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Expressive Reminder Offset Bottom Sheet
    if (showReminderPickerSheet) {
        val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ReminderOffsetSheet(
            sheetState = reminderSheetState,
            currentMinutes = reminderMinutesBefore,
            onDismiss = { showReminderPickerSheet = false },
            onSelect = { minutes ->
                reminderMinutesBefore = minutes
                showReminderPickerSheet = false
            }
        )
    }
}

@Composable
private fun DedicatedBookSearchView(
    vaultBooks: List<BookEntity>,
    searchQuery: String,
    selectedBook: BookEntity?,
    onSearchQueryChange: (String) -> Unit,
    onBookSelected: (BookEntity) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val filteredBooks = remember(vaultBooks, searchQuery) {
        if (searchQuery.isBlank()) {
            vaultBooks
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            vaultBooks.filter { book ->
                book.title.lowercase(Locale.getDefault()).contains(q) ||
                book.fileName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        // Top navigation bar with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_to_config")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowLeft24Regular,
                    contentDescription = "Back to Schedule",
                    tint = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Select Textbook",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${filteredBooks.size} books",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Fluent Icons
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by title or filename...") },
            leadingIcon = {
                Icon(
                    imageVector = FluentIcons.Search24Regular,
                    contentDescription = "Search",
                    tint = colors.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = FluentIcons.Dismiss24Regular,
                            contentDescription = "Clear search",
                            tint = colors.textSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.borderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_book_search")
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = FluentIcons.Search24Regular,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No textbooks in library" else "No books match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    val isCurrentSelection = book.id == selectedBook?.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrentSelection) colors.primary.copy(alpha = 0.08f) else colors.surfaceTint,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrentSelection) colors.primary else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onBookSelected(book) }
                            .testTag("book_item_${book.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrentSelection) colors.primary else colors.surfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Book24Filled,
                                    contentDescription = "Book icon",
                                    tint = if (isCurrentSelection) colors.onPrimary else colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = if (book.pageCount > 1) {
                                    "${book.pageCount} pages  •  Last read p. ${book.lastReadPage}"
                                } else {
                                    "Last read p. ${book.lastReadPage}"
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }

                            if (isCurrentSelection) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Checkmark24Regular,
                                        contentDescription = "Selected",
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(16.dp)
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

@Composable
private fun ReminderOffsetSheet(
    sheetState: SheetState,
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val colors = LocalMulberryColors.current
    var selectedMinutes by remember { mutableIntStateOf(currentMinutes) }
    var customSliderValue by remember {
        mutableFloatStateOf(if (currentMinutes > 0) currentMinutes.toFloat().coerceIn(5f, 120f) else 15f)
    }

    val presets = remember {
        listOf(
            -1 to "None",
            0 to "At start",
            15 to "15m before",
            30 to "30m before",
            60 to "1h before"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
        modifier = Modifier.testTag("reminder_offset_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FluentIcons.Alert24Regular,
                        contentDescription = "Reminder",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Session Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose when to receive an alert before your reading session.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Chips Grid
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (minutes, label) ->
                    val isSelected = selectedMinutes == minutes
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) colors.primary else colors.surfaceTint,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) colors.primary else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                selectedMinutes = minutes
                                if (minutes > 0) {
                                    customSliderValue = minutes.toFloat().coerceIn(5f, 120f)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.onPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Minutes Slider
            val isCustomActive = selectedMinutes > 0 && selectedMinutes !in listOf(15, 30, 60)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom Reminder Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${customSliderValue.toInt()} minutes before",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = customSliderValue,
                onValueChange = {
                    customSliderValue = it
                    selectedMinutes = it.toInt()
                },
                valueRange = 5f..120f,
                steps = 22, // 5m intervals: 5, 10, 15... 120
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.borderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSelect(selectedMinutes) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_confirm_reminder")
            ) {
                Icon(
                    imageVector = FluentIcons.Checkmark24Regular,
                    contentDescription = "Confirm",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply Reminder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
