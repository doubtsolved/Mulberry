package com.example.ui.screens.desk

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MulberryDatabase
import com.example.data.model.BookEntity
import com.example.data.model.UserProfile
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.BookContributionManager
import com.example.util.ExternalBookMetadata
import com.example.util.MulberryStorageManager
import com.example.util.VaultCryptoManager
import com.example.util.generateWebpCover
import com.example.util.slugify
import com.example.util.toCleanBookTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToDeskBottomSheet(
    tempPdfFile: File,
    initialFileName: String,
    totalPages: Int,
    userProfile: UserProfile,
    onDismissRequest: () -> Unit,
    onAddedToDesk: (slug: String, title: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    var bookTitle by remember { mutableStateOf(initialFileName.toCleanBookTitle()) }
    var selectedProf by remember { mutableStateOf(userProfile.selectedProf.ifEmpty { "1st Prof" }) }
    var selectedSubject by remember { mutableStateOf(getSubjectForProf(userProfile.selectedProf)) }
    var selectedAssetType by remember { mutableStateOf("Standard Textbook") }
    var contributeChecked by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val profOptions = listOf("1st Prof", "2nd Prof", "3rd Prof (Part 1)", "3rd Prof (Part 2)", "General & PG")
    val subjectOptions = listOf(
        "Anatomy", "Physiology", "Biochemistry",
        "Pathology", "Pharmacology", "Microbiology", "Forensic Medicine",
        "Community Medicine", "Ophthalmology", "ENT",
        "Medicine", "Surgery", "Obstetrics & Gynaecology", "Pediatrics",
        "Orthopedics", "Dermatology", "Psychiatry", "Radiology", "Anesthesiology"
    )
    val assetTypes = listOf(
        "Standard Textbook",
        "Handwritten Notes",
        "Question Bank (PYQ)",
        "Review Book",
        "Atlas",
        "Clinical Manual"
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismissRequest() },
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FluentIcons.ArrowDownload24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add to Desk",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Import external document into your encrypted library",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismissRequest,
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Book Title Input
            Text(
                text = "Book Title",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = bookTitle,
                onValueChange = { bookTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_to_desk_title_input"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isProcessing,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderSubtle
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prof and Subject Dropdowns Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Professional Year
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prof Year",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomDropdownSelector(
                        selectedValue = selectedProf,
                        options = profOptions,
                        onSelect = { selectedProf = it },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Subject
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subject",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomDropdownSelector(
                        selectedValue = selectedSubject,
                        options = subjectOptions,
                        onSelect = { selectedSubject = it },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Asset Type Dropdown
            Text(
                text = "Asset Type",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            CustomDropdownSelector(
                selectedValue = selectedAssetType,
                options = assetTypes,
                onSelect = { selectedAssetType = it },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Community Contribution Checkbox
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isProcessing) { contributeChecked = !contributeChecked }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = contributeChecked,
                        onCheckedChange = { contributeChecked = it },
                        enabled = !isProcessing,
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contribute to Mulberry Community",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Safely uploads encrypted replica so fellow medical students can study it offline.",
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Progress status display
            AnimatedVisibility(visible = isProcessing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage.ifEmpty { "Encrypting and adding to Desk..." },
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    if (bookTitle.isBlank()) {
                        Toast.makeText(context, "Please enter a valid title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    coroutineScope.launch {
                        processAddToDesk(
                            context = context,
                            tempPdfFile = tempPdfFile,
                            title = bookTitle.trim(),
                            prof = selectedProf,
                            subject = selectedSubject,
                            assetType = selectedAssetType,
                            totalPages = totalPages,
                            contribute = contributeChecked,
                            onStatusUpdate = { statusMessage = it },
                            onComplete = { slug, finalTitle ->
                                isProcessing = false
                                Toast.makeText(context, "Added \"$finalTitle\" to your Desk Shelf!", Toast.LENGTH_SHORT).show()
                                onAddedToDesk(slug, finalTitle)
                                onDismissRequest()
                            },
                            onError = { err ->
                                isProcessing = false
                                Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                enabled = !isProcessing && bookTitle.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_add_to_desk_button")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowDownload24Regular,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import & Add to Desk",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CustomDropdownSelector(
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedValue,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Icon(
                    imageVector = FluentIcons.ChevronDown16Regular,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surfaceCard)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = if (option == selectedValue) colors.primary else colors.textPrimary,
                            fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private suspend fun processAddToDesk(
    context: Context,
    tempPdfFile: File,
    title: String,
    prof: String,
    subject: String,
    assetType: String,
    totalPages: Int,
    contribute: Boolean,
    onStatusUpdate: (String) -> Unit,
    onComplete: (slug: String, title: String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        onStatusUpdate("Generating clean metadata...")
        val cleanTitle = title.toCleanBookTitle()
        val slug = cleanTitle.slugify()

        val mulberryDir = MulberryStorageManager.getMulberryDirectory(context)
        val targetBinFile = File(mulberryDir, "$slug.bin")
        val coversDir = File(mulberryDir, ".covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        val targetCoverFile = File(coversDir, "$slug.webp")

        // Step A: AES-256-GCM Encryption
        onStatusUpdate("Encrypting textbook into offline vault...")
        val encrypted = VaultCryptoManager.encryptFileToVault(tempPdfFile, targetBinFile)
        if (!encrypted || !targetBinFile.exists() || targetBinFile.length() == 0L) {
            withContext(Dispatchers.Main) {
                onError("Failed encrypting PDF to vault.")
            }
            return@withContext
        }

        // Step B: Render WebP Cover (150 DPI)
        onStatusUpdate("Rendering book cover at 150 DPI...")
        generateWebpCover(tempPdfFile, targetCoverFile, dpi = 150)

        // Step C: Atomic Indexing into /sdcard/Mulberry/downloaded_external.json
        onStatusUpdate("Updating local book index...")
        val fileSizeBytes = targetBinFile.length()
        val sizeMb = (fileSizeBytes / (1024.0 * 1024.0) * 10.0).toInt() / 10.0
        val coverUrl = if (targetCoverFile.exists()) "file://${targetCoverFile.absolutePath}" else ""

        val externalMeta = ExternalBookMetadata(
            id = slug,
            title = cleanTitle,
            author = "Local Import",
            prof = prof,
            subject = subject,
            type = assetType,
            totalPages = totalPages.coerceAtLeast(1),
            sizeMb = sizeMb,
            localPath = targetBinFile.absolutePath,
            coverUrl = coverUrl,
            isExternal = true,
            contributedStatus = "NONE"
        )
        MulberryStorageManager.recordDownloadedExternal(context, externalMeta)

        // Step D: Room Upsert
        onStatusUpdate("Registering in Desk Shelf...")
        val db = MulberryDatabase.getInstance(context)
        val bookDao = db.bookDao()
        val existingBook = bookDao.getBookBySlug(slug)
        val generatedId = existingBook?.id ?: (slug.hashCode() and 0x7FFFFFFF)

        val newEntity = BookEntity(
            id = generatedId,
            vaultId = 1,
            title = cleanTitle,
            author = "Local Import",
            fileName = targetBinFile.name,
            uriString = targetBinFile.absolutePath,
            pageCount = totalPages.coerceAtLeast(1),
            fileSizeBytes = fileSizeBytes,
            slug = slug,
            prof = prof,
            subject = subject,
            type = assetType,
            sizeMb = sizeMb,
            coverUrl = coverUrl,
            isDownloaded = true,
            binFilePath = targetBinFile.absolutePath
        )
        bookDao.insertBook(newEntity)

        // Step E: Enqueue background contribution if selected
        if (contribute) {
            onStatusUpdate("Scheduling background contribution...")
            BookContributionManager.enqueueContribution(
                context = context,
                slug = slug,
                title = cleanTitle,
                author = "Local Import",
                prof = prof,
                subject = subject,
                type = assetType,
                totalPages = totalPages.coerceAtLeast(1),
                sizeMb = sizeMb
            )
        }

        withContext(Dispatchers.Main) {
            onComplete(slug, cleanTitle)
        }
    } catch (e: Exception) {
        android.util.Log.e("AddToDeskBottomSheet", "processAddToDesk failed", e)
        withContext(Dispatchers.Main) {
            onError(e.localizedMessage ?: "Unknown error occurred")
        }
    }
}

private fun getSubjectForProf(prof: String): String {
    return when {
        prof.contains("1st", ignoreCase = true) -> "Anatomy"
        prof.contains("2nd", ignoreCase = true) -> "Pharmacology"
        prof.contains("Part 1", ignoreCase = true) -> "Ophthalmology"
        prof.contains("Part 2", ignoreCase = true) -> "Medicine"
        else -> "Anatomy"
    }
}
