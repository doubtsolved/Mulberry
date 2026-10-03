package com.example.data.desk

import android.content.Context
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DeskRepository(private val context: Context) {

    private var currentVaultDir: File = resolveDefaultVaultDir()
    val vaultDir: File get() = currentVaultDir

    val mulberryDir: File get() = File(currentVaultDir, ".mulberry").apply { mkdirs() }
    val notesDir: File get() = File(mulberryDir, "notes").apply { mkdirs() }
    val snipsDir: File get() = File(mulberryDir, "snips").apply { mkdirs() }
    private val bindingsFile: File get() = File(mulberryDir, "folder_bindings.json")

    init {
        ensureStarterVaultSeededIfEmpty(currentVaultDir)
    }

    private fun resolveDefaultVaultDir(): File {
        val ext = Environment.getExternalStorageDirectory()
        val candidate = File(ext, "MedicalVault")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            candidate.mkdirs()
            candidate
        } else if (candidate.exists() && candidate.canWrite()) {
            candidate
        } else {
            val appStorage = context.getExternalFilesDir(null) ?: context.filesDir
            val fallback = File(appStorage, "MedicalVault")
            fallback.mkdirs()
            fallback
        }
    }

    /**
     * Dynamically switches the active vault directory.
     * Seeds sample notes strictly if the target vault's .mulberry/notes/ directory is empty.
     */
    fun setVaultDirectory(dir: File) {
        if (dir.exists() || dir.mkdirs()) {
            currentVaultDir = dir
            ensureStarterVaultSeededIfEmpty(dir)
        }
    }

    /**
     * Seeds initial medical folders, markdown notes, and sample snip files ONLY if the notes directory is completely empty.
     */
    fun ensureStarterVaultSeededIfEmpty(targetDir: File) {
        try {
            val mDir = File(targetDir, ".mulberry").apply { mkdirs() }
            val nDir = File(mDir, "notes").apply { mkdirs() }
            val sDir = File(mDir, "snips").apply { mkdirs() }

            val existing = nDir.listFiles { f -> !f.name.startsWith(".") && (f.isDirectory || f.extension.equals("md", true)) }
            if (existing.isNullOrEmpty()) {
                // Seed Anatomy
                val anatomyDir = File(nDir, "Gross Anatomy").apply { mkdirs() }
                File(anatomyDir, "Upper Limb & Brachial Plexus.md").writeText(
                    """
                    # Upper Limb & Brachial Plexus

                    The brachial plexus supplies somatic motor and sensory innervation to the upper limb.

                    ## Clinical Checklist
                    - [ ] Memorize cords relative to axillary artery [[Grays_Anatomy#p.412]] @due(2026-10-15) #HighYield
                    - [x] Review Erb-Duchenne palsy (waiter's tip hand) @due(2026-10-10) #Clinical
                    - [ ] Auscultate radial nerve injury in spiral groove [[Guyton_Physiology#p.214]]

                    ## Visual Anatomy
                    ![[snip_brachial_plexus.webp|Brachial Plexus Cords & Branches]]
                    """.trimIndent()
                )
                File(anatomyDir, "Thorax & Mediastinum.md").writeText(
                    """
                    # Thorax & Mediastinum

                    Boundaries of the superior and inferior mediastinum with sternal angle plane at T4/T5.

                    ## High-Yield Points
                    - [ ] Trace route of left recurrent laryngeal nerve under aortic arch [[Grays_Anatomy#p.188]] @due(2026-10-18) #Anatomy
                    - [x] Identify coronary sulcus and anterior interventricular groove
                    """.trimIndent()
                )

                // Seed Physiology
                val physioDir = File(nDir, "Systemic Physiology").apply { mkdirs() }
                File(physioDir, "Cardiac Cycle & Heart Sounds.md").writeText(
                    """
                    # Cardiac Cycle & Heart Sounds

                    Understanding mechanical and electrical events across ventricular systole and diastole.

                    ## Clinical Checklist
                    - [ ] Review aortic valve auscultation site [[Guyton_Physiology#p.214]] @due(2026-10-12) #Cardio
                    - [ ] Study Wiggers diagram pressure volume curves [[Guyton_Physiology#p.115]] #HighYield
                    - [x] Calculate ejection fraction and stroke volume formulas

                    ## Diagram Reference
                    ![[snip_wiggers_diagram.webp|Wiggers Diagram Cardiac Cycle]]
                    """.trimIndent()
                )

                // Seed General
                val generalDir = File(nDir, "General Coursework").apply { mkdirs() }
                File(generalDir, "Syllabus & Exam Deadlines.md").writeText(
                    """
                    # Medical Term 1 Syllabus & Deadlines

                    - [ ] Gross anatomy cadaveric dissection practical @due(2026-10-22)
                    - [ ] Systemic physiology tutorial problem set 1-4
                    """.trimIndent()
                )

                // Bind Gray's Anatomy to Gross Anatomy
                bindBookToFolder("Gross Anatomy", "Grays_Anatomy.pdf")
                bindBookToFolder("Systemic Physiology", "Guyton_Physiology.pdf")

                // Create placeholder snip files if snips folder is empty
                createSampleSnipIfMissing(sDir, "snip_brachial_plexus.webp")
                createSampleSnipIfMissing(sDir, "snip_wiggers_diagram.webp")
            }
        } catch (_: Exception) {}
    }

    private fun createSampleSnipIfMissing(sDir: File, fileName: String) {
        val file = File(sDir, fileName)
        if (!file.exists()) {
            try {
                FileOutputStream(file).use { out ->
                    val dummy = byteArrayOf(
                        0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte(),
                        0x24.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                        0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte()
                    )
                    out.write(dummy)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun getFolders(): List<DeskFolder> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<DeskFolder>()
        val dirs = notesDir.listFiles { f -> f.isDirectory && !f.name.startsWith(".") } ?: emptyArray()

        val bindings = loadBindings()

        for (dir in dirs.sortedBy { it.name.lowercase() }) {
            val notes = dir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) } ?: emptyArray()
            var pendingTasks = 0
            var completedTasks = 0

            for (noteFile in notes) {
                try {
                    noteFile.forEachLine { line ->
                        val match = DeskMarkdownPatterns.TASK_PATTERN.find(line)
                        if (match != null) {
                            val mark = match.groupValues[2].trim()
                            if (mark.equals("x", ignoreCase = true)) {
                                completedTasks++
                            } else {
                                pendingTasks++
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            folders.add(
                DeskFolder(
                    id = dir.name,
                    name = dir.name,
                    directory = dir,
                    noteCount = notes.size,
                    pendingTasksCount = pendingTasks,
                    completedTasksCount = completedTasks,
                    boundBookFileName = bindings.optString(dir.name, null)
                )
            )
        }
        folders
    }

    suspend fun getNotesInFolder(folderName: String): List<DeskNote> = withContext(Dispatchers.IO) {
        val dir = File(notesDir, folderName)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) } ?: emptyArray()
        files.map { file ->
            var firstHeading: String? = null
            var excerpt = ""
            var tasksCount = 0
            var completedCount = 0

            try {
                val lines = file.readLines()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (firstHeading == null && trimmed.startsWith("# ")) {
                        firstHeading = trimmed.removePrefix("# ").trim()
                    } else if (excerpt.isEmpty() && trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("- [") && !trimmed.startsWith("![")) {
                        excerpt = trimmed
                    }

                    val match = DeskMarkdownPatterns.TASK_PATTERN.find(line)
                    if (match != null) {
                        tasksCount++
                        if (match.groupValues[2].trim().equals("x", ignoreCase = true)) {
                            completedCount++
                        }
                    }
                }
            } catch (_: Exception) {}

            DeskNote(
                file = file,
                folderName = folderName,
                title = firstHeading ?: file.nameWithoutExtension,
                lastModified = file.lastModified(),
                excerpt = excerpt,
                tasksCount = tasksCount,
                completedTasksCount = completedCount
            )
        }.sortedByDescending { it.lastModified }
    }

    suspend fun getNoteContent(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun saveNote(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createNote(folderName: String, title: String, initialContent: String = ""): File = withContext(Dispatchers.IO) {
        val dir = File(notesDir, folderName).apply { mkdirs() }
        val safeName = title.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_").ifEmpty { "Untitled" }
        var targetFile = File(dir, "$safeName.md")
        var counter = 1
        while (targetFile.exists()) {
            targetFile = File(dir, "$safeName ($counter).md")
            counter++
        }
        val text = initialContent.ifEmpty { "# $title\n\n" }
        targetFile.writeText(text)
        targetFile
    }

    suspend fun deleteNote(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createFolder(folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val safe = folderName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (safe.isEmpty()) return@withContext false
            val dir = File(notesDir, safe)
            if (dir.exists()) return@withContext false
            dir.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFolder(oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val safe = newName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (safe.isEmpty() || safe == oldName) return@withContext false
            val oldDir = File(notesDir, oldName)
            val newDir = File(notesDir, safe)
            if (!oldDir.exists() || newDir.exists()) return@withContext false
            val renamed = oldDir.renameTo(newDir)
            if (renamed) {
                val bindings = loadBindings()
                val boundBook = bindings.optString(oldName, null)
                if (boundBook != null) {
                    bindings.remove(oldName)
                    bindings.put(safe, boundBook)
                    saveBindings(bindings)
                }
            }
            renamed
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFolder(folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(notesDir, folderName)
            val deleted = dir.deleteRecursively()
            if (deleted) {
                val bindings = loadBindings()
                bindings.remove(folderName)
                saveBindings(bindings)
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }

    fun bindBookToFolder(folderName: String, bookFileName: String?) {
        try {
            val bindings = loadBindings()
            if (bookFileName.isNullOrEmpty()) {
                bindings.remove(folderName)
            } else {
                bindings.put(folderName, bookFileName)
            }
            saveBindings(bindings)
        } catch (_: Exception) {}
    }

    private fun loadBindings(): JSONObject {
        return try {
            if (bindingsFile.exists()) JSONObject(bindingsFile.readText()) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun saveBindings(obj: JSONObject) {
        try {
            bindingsFile.writeText(obj.toString())
        } catch (_: Exception) {}
    }

    suspend fun harvestAllTasks(): List<DeskTask> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<DeskTask>()
        val dirs = notesDir.listFiles { f -> f.isDirectory && !f.name.startsWith(".") } ?: emptyArray()

        for (dir in dirs) {
            val folderName = dir.name
            val noteFiles = dir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) } ?: emptyArray()

            for (file in noteFiles) {
                try {
                    val lines = file.readLines()
                    var firstH1: String? = null

                    lines.forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (firstH1 == null && trimmed.startsWith("# ")) {
                            firstH1 = trimmed.removePrefix("# ").trim()
                        }

                        val match = DeskMarkdownPatterns.TASK_PATTERN.find(line)
                        if (match != null) {
                            val mark = match.groupValues[2].trim()
                            val isCompleted = mark.equals("x", ignoreCase = true)
                            val rawContent = match.groupValues[3].trim()

                            val dueMatch = DeskMarkdownPatterns.DUE_DATE_PATTERN.find(rawContent)
                            val dueDate = dueMatch?.groupValues?.get(1)

                            val tags = DeskMarkdownPatterns.TAG_PATTERN.findAll(rawContent)
                                .map { it.groupValues[1] }
                                .toList()

                            val bookMatch = DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.find(rawContent)
                            val linkedBook = bookMatch?.groupValues?.get(1)?.trim()
                            val linkedPage = bookMatch?.groupValues?.getOrNull(2)?.toIntOrNull()

                            var cleanContent = rawContent
                                .replace(DeskMarkdownPatterns.DUE_DATE_PATTERN, "")
                                .replace(DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN, "")
                                .replace(DeskMarkdownPatterns.TAG_PATTERN, "")
                                .trim()

                            if (cleanContent.isEmpty()) {
                                cleanContent = rawContent
                            }

                            tasks.add(
                                DeskTask(
                                    id = "${file.absolutePath}_$index",
                                    sourceFile = file,
                                    folderName = folderName,
                                    noteTitle = firstH1 ?: file.nameWithoutExtension,
                                    lineIndex = index,
                                    rawText = line,
                                    content = cleanContent,
                                    isCompleted = isCompleted,
                                    linkedBook = linkedBook,
                                    linkedPage = linkedPage,
                                    dueDate = dueDate,
                                    tags = tags
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        tasks
    }

    suspend fun toggleTaskStatus(task: DeskTask): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!task.sourceFile.exists()) return@withContext false
            val lines = task.sourceFile.readLines().toMutableList()

            var targetIndex = -1
            if (task.lineIndex in lines.indices && lines[task.lineIndex].contains("- [")) {
                targetIndex = task.lineIndex
            } else {
                targetIndex = lines.indexOfFirst { it.contains(task.content) && it.contains("- [") }
            }

            if (targetIndex != -1) {
                val originalLine = lines[targetIndex]
                val updatedLine = if (task.isCompleted) {
                    originalLine.replaceFirst(Regex("""-\s*\[[xX]\]"""), "- [ ]")
                } else {
                    originalLine.replaceFirst(Regex("""-\s*\[\s*\]"""), "- [x]")
                }
                lines[targetIndex] = updatedLine
                task.sourceFile.writeText(lines.joinToString("\n"))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSnips(): List<File> = withContext(Dispatchers.IO) {
        snipsDir.listFiles { f ->
            f.isFile && (f.extension.equals("webp", true) || f.extension.equals("png", true) || f.extension.equals("jpg", true))
        }?.toList() ?: emptyList()
    }
}
