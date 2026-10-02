package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import com.example.ui.reader.ReaderActivity
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaTaskEntity
import com.example.data.model.ExamEntity
import com.example.ui.components.FluentIcons
import com.example.ui.components.MarkdownSyllabusView
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.toCleanBookTitle
import com.example.viewmodel.MulberryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: MulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val view = LocalView.current
    val context = LocalContext.current

    val tasks by viewModel.allTasks.collectAsState()
    val exams by viewModel.allExams.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<AgendaTaskEntity?>(null) }

    var showAddExamDialog by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<ExamEntity?>(null) }

    // Safe date formatting without java.time
    val formattedToday = remember {
        try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "18/06/2026"
        }
    }

    // Safe Progress Fraction guarded against divide-by-zero & NaN
    val completedTasksCount = tasks.count { it.isCompleted }
    val totalTasksCount = tasks.size
    val progressFraction = if (totalTasksCount > 0) {
        val calc = completedTasksCount.toFloat() / totalTasksCount.toFloat()
        if (calc.isNaN() || calc.isInfinite()) 0f else calc.coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Date Header & Daily Progress Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedToday,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        color = colors.textPrimary,
                        letterSpacing = (-0.25).sp
                    )
                    Text(
                        text = "Daily Study & Syllabus Tracker",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                // Daily Progress Pill with circular arc
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceTint,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.size(18.dp),
                                color = colors.primary,
                                trackColor = colors.primary.copy(alpha = 0.2f),
                                strokeWidth = 2.5.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$completedTasksCount/$totalTasksCount Done",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.primary
                        )
                    }
                }
            }
        }

        // 2. Section: Today's Agenda Checklist
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Agenda",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceTint,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.testTag("add_agenda_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No study tasks scheduled for today.",
                            fontFamily = InterFamily,
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddTaskDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Create Daily Task", fontFamily = InterFamily, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(tasks, key = { "task_${it.id}" }) { task ->
                AgendaTaskItem(
                    task = task,
                    onToggleComplete = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.toggleTaskCompleted(task)
                    },
                    onEditTask = {
                        taskToEdit = task
                    },
                    onDeleteTask = {
                        viewModel.deleteTask(task.id)
                    },
                    onOpenLinkedDocument = {
                        val book = allBooks.find { it.id == task.linkedBookId }
                            ?: allBooks.find { it.title.contains(task.linkedBookTitle ?: "", ignoreCase = true) }
                        if (book != null) {
                            ReaderActivity.launch(context, book.uriString, book.title, book.id)
                        }
                    },
                    onOpenLecture = {
                        if (!task.linkedLectureUrl.isNullOrEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.linkedLectureUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            }
        }

        // 3. Section: Upcoming Exams
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Exams & Syllabi",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceTint,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { showAddExamDialog = true },
                        modifier = Modifier.testTag("add_exam_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Exam",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (exams.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No upcoming exams entered.",
                            fontFamily = InterFamily,
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(exams, key = { "exam_${it.id}" }) { exam ->
                UpcomingExamCard(
                    exam = exam,
                    onEditExam = {
                        examToEdit = exam
                    },
                    onDeleteExam = {
                        viewModel.deleteExam(exam.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Modal: Task Composer (Add or Edit)
    if (showAddTaskDialog || taskToEdit != null) {
        val editing = taskToEdit
        var taskTitle by remember(editing) { mutableStateOf(editing?.title ?: "") }
        var linkedBookName by remember(editing) { mutableStateOf(editing?.linkedBookTitle ?: "") }
        var linkedLectureName by remember(editing) { mutableStateOf(editing?.linkedLectureTitle ?: "") }
        var linkedUrl by remember(editing) { mutableStateOf(editing?.linkedLectureUrl ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddTaskDialog = false
                taskToEdit = null
            },
            title = {
                Text(
                    text = if (editing != null) "Edit Agenda Task" else "Add Agenda Task",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkedBookName,
                        onValueChange = { linkedBookName = it },
                        label = { Text("Linked Document (e.g. Guyton Ch. 4)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkedLectureName,
                        onValueChange = { linkedLectureName = it },
                        label = { Text("Linked Lecture Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkedUrl,
                        onValueChange = { linkedUrl = it },
                        label = { Text("Lecture URL (YouTube / Video)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            val matchedBook = allBooks.find {
                                it.title.contains(linkedBookName, ignoreCase = true)
                            }
                            if (editing != null) {
                                viewModel.updateTask(
                                    editing.copy(
                                        title = taskTitle.trim(),
                                        linkedBookId = matchedBook?.id ?: editing.linkedBookId,
                                        linkedBookTitle = linkedBookName.ifBlank { null },
                                        linkedLectureUrl = linkedUrl.ifBlank { null },
                                        linkedLectureTitle = linkedLectureName.ifBlank { null }
                                    )
                                )
                            } else {
                                viewModel.addNewTask(
                                    title = taskTitle.trim(),
                                    linkedBookId = matchedBook?.id,
                                    linkedBookTitle = linkedBookName.ifBlank { null },
                                    linkedChapterPage = matchedBook?.lastReadPage ?: 1,
                                    linkedLectureUrl = linkedUrl.ifBlank { null },
                                    linkedLectureTitle = linkedLectureName.ifBlank { null }
                                )
                            }
                            showAddTaskDialog = false
                            taskToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (editing != null) "Update" else "Add Task", fontFamily = InterFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTaskDialog = false
                    taskToEdit = null
                }) {
                    Text("Cancel", fontFamily = InterFamily)
                }
            }
        )
    }

    // Modal: Exam Builder (Add or Edit with M3 DatePicker and Live Markdown Syllabus)
    if (showAddExamDialog || examToEdit != null) {
        val editingExam = examToEdit
        var examTitle by remember(editingExam) { mutableStateOf(editingExam?.title ?: "") }
        var selectedDateMillis by remember(editingExam) {
            mutableLongStateOf(editingExam?.examDateMillis ?: (System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000))
        }
        var syllabusMarkdown by remember(editingExam) {
            mutableStateOf(
                editingExam?.syllabusJson ?: "# Exam Syllabus\n!important High yield chapters\n\n- Cell Physiology\n- Membrane Potentials\n- Cardiac Output\n\n> Revise past questions"
            )
        }

        var showDatePicker by remember { mutableStateOf(false) }
        var currentTabIdx by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = Live Preview

        val dateDisplayStr = remember(selectedDateMillis) {
            try {
                SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            } catch (e: Exception) {
                "Selected Date"
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            selectedDateMillis = picked
                        }
                        showDatePicker = false
                    }) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddExamDialog = false
                examToEdit = null
            },
            title = {
                Text(
                    text = if (editingExam != null) "Edit Exam & Syllabus" else "Add Exam & Syllabus",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = { examTitle = it },
                        label = { Text("Exam Name (e.g. Finals)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date Picker Trigger Row
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date: $dateDisplayStr", fontFamily = InterFamily)
                    }

                    // Markdown Syllabus Tabs: Edit vs Live Preview
                    TabRow(
                        selectedTabIndex = currentTabIdx,
                        containerColor = colors.surfaceTint,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = currentTabIdx == 0,
                            onClick = { currentTabIdx = 0 },
                            text = { Text("Markdown Editor", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTabIdx == 1,
                            onClick = { currentTabIdx = 1 },
                            text = { Text("Live Preview", fontSize = 12.sp) }
                        )
                    }

                    if (currentTabIdx == 0) {
                        OutlinedTextField(
                            value = syllabusMarkdown,
                            onValueChange = { syllabusMarkdown = it },
                            label = { Text("Syllabus Markdown (# Heading, - List, !important)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 8
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(4.dp)
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                MarkdownSyllabusView(markdown = syllabusMarkdown)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (examTitle.isNotBlank()) {
                            if (editingExam != null) {
                                viewModel.updateExam(
                                    editingExam.copy(
                                        title = examTitle.trim(),
                                        examDateMillis = selectedDateMillis,
                                        syllabusJson = syllabusMarkdown
                                    )
                                )
                            } else {
                                viewModel.addNewExam(examTitle.trim(), selectedDateMillis, syllabusMarkdown)
                            }
                            showAddExamDialog = false
                            examToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (editingExam != null) "Update" else "Save Exam", fontFamily = InterFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddExamDialog = false
                    examToEdit = null
                }) {
                    Text("Cancel", fontFamily = InterFamily)
                }
            }
        )
    }
}

@Composable
fun AgendaTaskItem(
    task: AgendaTaskEntity,
    onToggleComplete: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onOpenLinkedDocument: () -> Unit,
    onOpenLecture: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val rowAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.45f else 1f,
        label = "taskRowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
    ) {
        // Main Task Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Checkbox: 20.dp rounded square (4.dp corner radius)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (task.isCompleted) colors.primary else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (task.isCompleted) colors.primary else colors.borderSubtle,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable(onClick = onToggleComplete)
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Label: Inter Medium 15.sp with strikethrough if completed
            Text(
                text = task.title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = colors.textPrimary,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )

            // Edit Task Button
            IconButton(
                onClick = onEditTask,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete Task Button
            IconButton(
                onClick = onDeleteTask,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Connected Resource Branches (├──, └──)
        val hasDoc = !task.linkedBookTitle.isNullOrEmpty()
        val hasLecture = !task.linkedLectureTitle.isNullOrEmpty()

        if (hasDoc || hasLecture) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp)
            ) {
                if (hasDoc) {
                    val isOnlyDoc = !hasLecture
                    ResourceBranchRow(
                        isLastBranch = isOnlyDoc,
                        lineColor = colors.borderSubtle
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceTint,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenLinkedDocument)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📘 ${task.linkedBookTitle?.toCleanBookTitle() ?: "Book"}",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (hasLecture) {
                    ResourceBranchRow(
                        isLastBranch = true,
                        lineColor = colors.borderSubtle
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenLecture)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📹 ${task.linkedLectureTitle}",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceBranchRow(
    isLastBranch: Boolean,
    lineColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(22.dp)
                .height(34.dp)
        ) {
            if (size.width > 0f && size.height > 0f) {
                val strokePx = 1.5.dp.toPx()
                val midY = size.height / 2f
                val startX = 0f

                drawLine(
                    color = lineColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX, if (isLastBranch) midY else size.height),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = lineColor,
                    start = Offset(startX, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        content()
    }
}

@Composable
fun UpcomingExamCard(
    exam: ExamEntity,
    onEditExam: () -> Unit,
    onDeleteExam: () -> Unit
) {
    val colors = LocalMulberryColors.current
    var isExpanded by remember { mutableStateOf(false) }

    // Dynamic days left calculation from current date
    val daysLeft = try {
        ((exam.examDateMillis - System.currentTimeMillis()) / (24L * 60 * 60 * 1000L)).coerceAtLeast(0)
    } catch (e: Exception) {
        0L
    }

    val dateFormatted = remember(exam.examDateMillis) {
        try {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exam.examDateMillis))
        } catch (e: Exception) {
            ""
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceTint,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Clock glyph + Title + Edit & Delete action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FluentIcons.Clock24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = exam.title,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.textPrimary
                        )
                        if (dateFormatted.isNotBlank()) {
                            Text(
                                text = dateFormatted,
                                fontFamily = InterFamily,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditExam,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exam",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeleteExam,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Exam",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Countdown Badge: Solid accent pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary
                ) {
                    Text(
                        text = "$daysLeft Days Left",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "•   Target Date",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Syllabus Drawer Accordion Trigger
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Live Syllabus Markdown" else "View Live Syllabus Markdown",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expandable inline syllabus rendered with live Markdown parser
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        MarkdownSyllabusView(markdown = exam.syllabusJson)
                    }
                }
            }
        }
    }
}
