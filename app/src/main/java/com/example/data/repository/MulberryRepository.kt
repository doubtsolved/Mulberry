package com.example.data.repository

import com.example.data.dao.AgendaTaskDao
import com.example.data.dao.AnnotationDao
import com.example.data.dao.BookDao
import com.example.data.dao.ExamDao
import com.example.data.dao.VaultDao
import com.example.data.model.AgendaTaskEntity
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.ExamEntity
import com.example.data.model.VaultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MulberryRepository(
    private val vaultDao: VaultDao,
    private val bookDao: BookDao,
    private val agendaTaskDao: AgendaTaskDao,
    private val examDao: ExamDao,
    private val annotationDao: AnnotationDao
) {
    private val bookRepository = BookRepository(bookDao, vaultDao)

    val allVaults: Flow<List<VaultEntity>> = vaultDao.getAllVaults()
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val continueReadingBooks: Flow<List<BookEntity>> = bookDao.getContinueReadingBooks()
    val allTasks: Flow<List<AgendaTaskEntity>> = agendaTaskDao.getAllTasks()
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()

    suspend fun syncVaultWithDisk(vaultPath: String, vaultId: String) =
        bookRepository.syncVaultWithDisk(vaultPath, vaultId)

    fun getBooksByVault(vaultId: Int): Flow<List<BookEntity>> = bookDao.getBooksByVault(vaultId)

    suspend fun getAllBooksSync(): List<BookEntity> = bookDao.getAllBooksSync()

    suspend fun getBookById(id: Int): BookEntity? = bookDao.getBookById(id)

    suspend fun addVault(name: String, uriString: String, pathDisplay: String): Long {
        val vault = VaultEntity(
            name = name,
            uriString = uriString,
            pathDisplay = pathDisplay,
            bookCount = 0
        )
        return vaultDao.insertVault(vault)
    }

    suspend fun getAllVaultsSync(): List<VaultEntity> = vaultDao.getAllVaultsSync()

    suspend fun updateVault(vault: VaultEntity) = vaultDao.updateVault(vault)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteVault(vault: VaultEntity) {
        bookDao.deleteBooksByVault(vault.id)
        vaultDao.deleteVault(vault)
    }

    suspend fun addBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun addBooks(books: List<BookEntity>) = bookDao.insertBooks(books)

    suspend fun toggleFavorite(bookId: Int, isFavorite: Boolean) =
        bookDao.updateFavorite(bookId, isFavorite)

    suspend fun updateReadingProgress(bookId: Int, page: Int, percent: Int) =
        bookDao.updateReadingProgress(bookId, page, percent)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun addTask(task: AgendaTaskEntity): Long = agendaTaskDao.insertTask(task)

    suspend fun updateTask(task: AgendaTaskEntity) = agendaTaskDao.updateTask(task)

    suspend fun toggleTaskCompleted(taskId: Int, isCompleted: Boolean) =
        agendaTaskDao.setTaskCompleted(taskId, isCompleted)

    suspend fun deleteTask(taskId: Int) = agendaTaskDao.deleteTaskById(taskId)

    // Exams
    suspend fun addExam(exam: ExamEntity): Long = examDao.insertExam(exam)

    suspend fun updateExam(exam: ExamEntity) = examDao.updateExam(exam)

    suspend fun deleteExam(examId: Int) = examDao.deleteExamById(examId)

    // Annotations
    fun getAnnotations(bookId: Int, pageIndex: Int): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForPage(bookId, pageIndex)

    suspend fun saveAnnotation(annotation: AnnotationEntity) =
        annotationDao.insertAnnotation(annotation)

    suspend fun getLastAnnotation(bookId: Int, pageIndex: Int): AnnotationEntity? =
        annotationDao.getLastAnnotation(bookId, pageIndex)

    suspend fun deleteAnnotationById(id: Int) =
        annotationDao.deleteAnnotationById(id)

    suspend fun undoLastAnnotation(bookId: Int, pageIndex: Int) =
        annotationDao.undoLastAnnotation(bookId, pageIndex)

    fun getAllAnnotationsForBook(bookId: Int): Flow<List<AnnotationEntity>> =
        annotationDao.getAllAnnotationsForBook(bookId)

    suspend fun getAnnotationsListForBook(bookId: Int): List<AnnotationEntity> =
        annotationDao.getAnnotationsListForBook(bookId)

    suspend fun deleteAllAnnotationsForBook(bookId: Int) =
        annotationDao.deleteAllAnnotationsForBook(bookId)

    // Seed academic sample bundle if user requests demo data
    suspend fun seedSampleMedicalBundle() {
        val vaultId = addVault(
            name = "MBBS Vault",
            uriString = "content://com.android.externalstorage.documents/tree/primary%3AMBBS%20Vault",
            pathDisplay = "/storage/emulated/0/MBBS Vault"
        ).toInt()

        val sampleBooks = listOf(
            BookEntity(
                vaultId = vaultId,
                title = "Guyton and Hall Textbook of Medical Physiology",
                author = "John E. Hall, PhD",
                fileName = "Guyton_and_Hall_Physiology_14th.pdf",
                uriString = "sample://guyton_hall",
                pageCount = 1152,
                fileSizeBytes = 85_983_232L,
                isFavorite = true,
                lastReadPage = 142,
                progressPercent = 34,
                lastReadTimestamp = System.currentTimeMillis() - 120_000,
                parentFolder = "Physiology"
            ),
            BookEntity(
                vaultId = vaultId,
                title = "Costanzo Physiology 7th Edition",
                author = "Linda S. Costanzo, PhD",
                fileName = "Costanzo_Physiology_7th.pdf",
                uriString = "sample://costanzo_phys",
                pageCount = 520,
                fileSizeBytes = 35_651_584L,
                isFavorite = true,
                lastReadPage = 405,
                progressPercent = 78,
                lastReadTimestamp = System.currentTimeMillis() - 860_000,
                parentFolder = "Physiology"
            ),
            BookEntity(
                vaultId = vaultId,
                title = "Gray's Anatomy for Students",
                author = "Richard Drake, A. Wayne Vogl",
                fileName = "Grays_Anatomy_Students_4th.pdf",
                uriString = "sample://grays_anatomy",
                pageCount = 1160,
                fileSizeBytes = 94_371_840L,
                isFavorite = false,
                lastReadPage = 112,
                progressPercent = 12,
                lastReadTimestamp = System.currentTimeMillis() - 3_600_000,
                parentFolder = "Anatomy"
            ),
            BookEntity(
                vaultId = vaultId,
                title = "Concepts of Physics (Vol. 1)",
                author = "Dr. H.C. Verma",
                fileName = "Concepts_of_Physics_Vol1.pdf",
                uriString = "sample://hcv_physics",
                pageCount = 462,
                fileSizeBytes = 28_311_552L,
                isFavorite = true,
                lastReadPage = 42,
                progressPercent = 90,
                lastReadTimestamp = System.currentTimeMillis() - 86_400_000,
                parentFolder = "Physics"
            ),
            BookEntity(
                vaultId = vaultId,
                title = "Lehninger Principles of Biochemistry",
                author = "David L. Nelson, Michael M. Cox",
                fileName = "Lehninger_Biochemistry_8th.pdf",
                uriString = "sample://lehninger_biochem",
                pageCount = 1264,
                fileSizeBytes = 112_197_632L,
                isFavorite = false,
                lastReadPage = 1,
                progressPercent = 0,
                lastReadTimestamp = 0L,
                parentFolder = "Biochemistry"
            )
        )
        addBooks(sampleBooks)

        // Seed initial Agenda Tasks
        addTask(
            AgendaTaskEntity(
                title = "Read Concepts of Physics",
                isCompleted = false,
                dateString = "18/06/2026",
                linkedBookId = 4,
                linkedBookTitle = "Concepts of Physics, Ch. 4",
                linkedChapterPage = 42,
                linkedLectureUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                linkedLectureTitle = "Lecture: Projectile Motion"
            )
        )
        addTask(
            AgendaTaskEntity(
                title = "Revise Upper Limb Anatomy",
                isCompleted = true,
                dateString = "18/06/2026",
                linkedBookId = 3,
                linkedBookTitle = "Gray's Anatomy, p. 112",
                linkedChapterPage = 112,
                linkedLectureUrl = null,
                linkedLectureTitle = null
            )
        )
        addTask(
            AgendaTaskEntity(
                title = "Cardiovascular Mechanics Notes",
                isCompleted = true,
                dateString = "18/06/2026",
                linkedBookId = 1,
                linkedBookTitle = "Guyton & Hall, Ch. 9",
                linkedChapterPage = 108,
                linkedLectureUrl = null,
                linkedLectureTitle = null
            )
        )
        addTask(
            AgendaTaskEntity(
                title = "Practice Renal Clearance Formulas",
                isCompleted = true,
                dateString = "18/06/2026",
                linkedBookId = 2,
                linkedBookTitle = "Costanzo Physiology, Ch. 5",
                linkedChapterPage = 250,
                linkedLectureUrl = null,
                linkedLectureTitle = null
            )
        )
        addTask(
            AgendaTaskEntity(
                title = "Solve Wave Optics Problem Set",
                isCompleted = false,
                dateString = "18/06/2026",
                linkedBookId = 4,
                linkedBookTitle = "Concepts of Physics, Ch. 17",
                linkedChapterPage = 310,
                linkedLectureUrl = null,
                linkedLectureTitle = null
            )
        )

        // Seed Upcoming Exam
        val syllabusJson = JSONArray().apply {
            put(JSONObject().put("title", "Cardiac Muscle Physiology").put("completed", true))
            put(JSONObject().put("title", "Rhythmic Excitation of the Heart").put("completed", true))
            put(JSONObject().put("title", "Electrocardiogram Interpretation").put("completed", false))
            put(JSONObject().put("title", "Vascular Distensibility & Arterial Pressure").put("completed", false))
            put(JSONObject().put("title", "Renal Regulation of Fluid Balance").put("completed", false))
            put(JSONObject().put("title", "Glomerular Filtration Rate").put("completed", false))
            put(JSONObject().put("title", "Pulmonary Ventilation Mechanics").put("completed", false))
            put(JSONObject().put("title", "Acid-Base Regulation").put("completed", false))
        }.toString()

        addExam(
            ExamEntity(
                title = "Semester 1 Finals",
                examDateMillis = System.currentTimeMillis() + (20L * 24 * 60 * 60 * 1000),
                totalChaptersCount = 8,
                remainingChaptersCount = 6,
                syllabusJson = syllabusJson
            )
        )
    }

    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Mulberry")

        val booksArray = JSONArray()
        bookDao.getAllBooksSync().forEach { b ->
            val obj = JSONObject()
            obj.put("title", b.title)
            obj.put("author", b.author)
            obj.put("uriString", b.uriString)
            obj.put("pageCount", b.pageCount)
            obj.put("isFavorite", b.isFavorite)
            obj.put("lastReadPage", b.lastReadPage)
            obj.put("progressPercent", b.progressPercent)
            booksArray.put(obj)
        }
        root.put("books", booksArray)

        val tasksArray = JSONArray()
        agendaTaskDao.getAllTasksSync().forEach { t ->
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("isCompleted", t.isCompleted)
            obj.put("dateString", t.dateString)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        root.toString(2)
    }

    suspend fun importJsonBackup(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val tasksArray = root.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val t = tasksArray.getJSONObject(i)
                    addTask(
                        AgendaTaskEntity(
                            title = t.optString("title", "Imported Task"),
                            isCompleted = t.optBoolean("isCompleted", false),
                            dateString = t.optString("dateString", "")
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
