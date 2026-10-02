package com.example.data.repository

import android.content.Context
import androidx.core.util.AtomicFile
import com.example.data.MulberryDatabase
import com.example.data.model.AgendaTaskEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookStateItem
import com.example.data.model.LibraryStateConfig
import com.example.data.model.StudyTaskItem
import com.example.data.model.StudyTasksConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VaultMetadataManager {
    @Volatile
    var appContext: Context? = null

    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getMetadataDir(vaultRootPath: String): File {
        val dir = File(vaultRootPath, ".mulberry")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun writeAtomic(targetFile: File, content: String) {
        val atomicFile = AtomicFile(targetFile)
        var fos: FileOutputStream? = null
        try {
            fos = atomicFile.startWrite()
            fos.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(fos)
        } catch (e: Exception) {
            if (fos != null) {
                atomicFile.failWrite(fos)
            }
            e.printStackTrace()
        }
    }

    private fun readAtomic(file: File): String? {
        if (!file.exists()) return null
        return try {
            val atomicFile = AtomicFile(file)
            atomicFile.readFully().toString(Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                file.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun hydrateDatabaseFromMetadata(
        vaultRootPath: String,
        vaultId: String,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val ctx = context ?: appContext ?: return@withContext
        appContext = ctx.applicationContext
        val rootDir = File(vaultRootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext

        val db = MulberryDatabase.getInstance(ctx)
        val metadataDir = File(vaultRootPath, ".mulberry")
        if (!metadataDir.exists()) return@withContext

        // 1. Hydrate Library State
        val libraryFile = File(metadataDir, "library_state.json")
        val libraryJson = readAtomic(libraryFile)
        if (!libraryJson.isNullOrBlank()) {
            try {
                val config = gson.fromJson(libraryJson, LibraryStateConfig::class.java)
                if (config?.books != null && config.books.isNotEmpty()) {
                    val dbBooks = db.bookDao().getAllBooksSync()
                    val booksMap = dbBooks.associateBy { it.uriString }

                    val toUpdate = mutableListOf<BookEntity>()
                    for (item in config.books) {
                        val absPath = try {
                            File(vaultRootPath, item.relativePath).canonicalPath
                        } catch (_: Exception) {
                            File(vaultRootPath, item.relativePath).absolutePath
                        }
                        val existing = booksMap[absPath] ?: dbBooks.find {
                            it.fileName == File(item.relativePath).name
                        }
                        if (existing != null) {
                            val updated = existing.copy(
                                isFavorite = item.isFavorite || existing.isFavorite,
                                lastReadPage = maxOf(existing.lastReadPage, item.lastPage),
                                pageCount = if (item.totalPages > 1) item.totalPages else existing.pageCount,
                                progressPercent = maxOf(existing.progressPercent, item.progressPercent.toInt()),
                                lastReadTimestamp = maxOf(existing.lastReadTimestamp, item.lastReadTimestamp)
                            )
                            toUpdate.add(updated)
                        }
                    }
                    if (toUpdate.isNotEmpty()) {
                        db.bookDao().updateBooks(toUpdate)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Hydrate Study Tasks
        val tasksFile = File(metadataDir, "study_tasks.json")
        val tasksJson = readAtomic(tasksFile)
        if (!tasksJson.isNullOrBlank()) {
            try {
                val config = gson.fromJson(tasksJson, StudyTasksConfig::class.java)
                if (config?.tasks != null && config.tasks.isNotEmpty()) {
                    val existingTasks = db.agendaTaskDao().getAllTasksSync()
                    val existingTitles = existingTasks.map { it.title.trim().lowercase() }.toSet()
                    val allBooks = db.bookDao().getAllBooksSync()

                    for (taskItem in config.tasks) {
                        if (!existingTitles.contains(taskItem.title.trim().lowercase())) {
                            val linkedBook = if (taskItem.linkedBookRelativePath != null) {
                                val linkedAbs = try {
                                    File(vaultRootPath, taskItem.linkedBookRelativePath).canonicalPath
                                } catch (_: Exception) {
                                    File(vaultRootPath, taskItem.linkedBookRelativePath).absolutePath
                                }
                                allBooks.find { it.uriString == linkedAbs || it.fileName == File(taskItem.linkedBookRelativePath).name }
                            } else null

                            db.agendaTaskDao().insertTask(
                                AgendaTaskEntity(
                                    title = taskItem.title,
                                    isCompleted = taskItem.isCompleted,
                                    dateString = "Vault Sync",
                                    linkedBookId = linkedBook?.id,
                                    linkedBookTitle = linkedBook?.title,
                                    linkedChapterPage = taskItem.linkedPage
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun persistVaultMetadata(
        vaultRootPath: String,
        vaultId: String,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val ctx = context ?: appContext ?: return@withContext
        appContext = ctx.applicationContext
        if (vaultRootPath.isBlank()) return@withContext

        val rootDir = File(vaultRootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext

        val db = MulberryDatabase.getInstance(ctx)
        val metadataDir = getMetadataDir(vaultRootPath)

        // 1. Persist Books & Library State
        val vaultIdInt = vaultId.toIntOrNull() ?: -1
        val dbBooks = if (vaultIdInt != -1) {
            db.bookDao().getBooksByVaultSync(vaultIdInt)
        } else {
            db.bookDao().getAllBooksSync().filter { it.uriString.startsWith(vaultRootPath) }
        }

        val stateItems = dbBooks.map { book ->
            val relPath = try {
                File(book.uriString).toRelativeString(rootDir)
            } catch (_: Exception) {
                book.fileName
            }
            BookStateItem(
                relativePath = relPath,
                lastPage = book.lastReadPage,
                totalPages = book.pageCount,
                progressPercent = book.progressPercent.toFloat(),
                isFavorite = book.isFavorite,
                lastReadTimestamp = book.lastReadTimestamp
            )
        }

        val libraryConfig = LibraryStateConfig(
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            books = stateItems
        )
        val libraryJson = gson.toJson(libraryConfig)
        writeAtomic(File(metadataDir, "library_state.json"), libraryJson)

        // 2. Persist Tasks
        val allTasks = db.agendaTaskDao().getAllTasksSync()
        val taskItems = allTasks.map { task ->
            val relPath = if (task.linkedBookId != null) {
                val book = dbBooks.find { it.id == task.linkedBookId }
                book?.let {
                    try {
                        File(it.uriString).toRelativeString(rootDir)
                    } catch (_: Exception) {
                        null
                    }
                }
            } else null

            StudyTaskItem(
                id = task.id.toString(),
                title = task.title,
                isCompleted = task.isCompleted,
                dueDate = null,
                linkedBookRelativePath = relPath,
                linkedPage = task.linkedChapterPage
            )
        }

        val tasksConfig = StudyTasksConfig(
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            tasks = taskItems
        )
        val tasksJson = gson.toJson(tasksConfig)
        writeAtomic(File(metadataDir, "study_tasks.json"), tasksJson)
    }
}
