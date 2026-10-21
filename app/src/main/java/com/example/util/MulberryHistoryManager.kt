package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.BookEntity
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Calendar
import java.util.UUID

/**
 * Immutable activity / movement event recorded in /sdcard/Mulberry/.history/activity_events.json
 */
data class UserMovementEvent(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("eventType") val eventType: String, // OPEN_BOOK, READING_PROGRESS, START_SESSION, COMPLETE_SESSION, ADD_FILE, CREATE_COLLECTION
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("bookSlug") val bookSlug: String = "",
    @SerializedName("bookTitle") val bookTitle: String = "",
    @SerializedName("bookAuthor") val bookAuthor: String = "",
    @SerializedName("bookSubject") val bookSubject: String = "",
    @SerializedName("page") val page: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("progressPercent") val progressPercent: Int = 0,
    @SerializedName("durationMinutes") val durationMinutes: Int = 0,
    @SerializedName("durationSeconds") val durationSeconds: Long = 0L,
    @SerializedName("collectionName") val collectionName: String = "",
    @SerializedName("metadata") val metadata: Map<String, String> = emptyMap()
)

/**
 * Root history ledger stored at /sdcard/Mulberry/.history/activity_events.json
 */
data class ActivityEventsIndex(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("storageDirectory") val storageDirectory: String = "/sdcard/Mulberry/.history",
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("totalEvents") val totalEvents: Int = 0,
    @SerializedName("weeklyGoalMinutes") val weeklyGoalMinutes: Int = 420, // default 7 hours (420 mins)
    @SerializedName("events") val events: List<UserMovementEvent> = emptyList()
)

/**
 * Custom collections created by user, stored in /sdcard/Mulberry/.history/collections.json
 */
data class CustomCollectionItem(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("resourceCount") val resourceCount: Int = 0,
    @SerializedName("bookSlugs") val bookSlugs: List<String> = emptyList(),
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)

data class CollectionsIndex(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("collections") val collections: List<CustomCollectionItem> = emptyList()
)

/**
 * Manages user telemetry, activity ledger, and custom collections in /sdcard/Mulberry/.history/
 */
object MulberryHistoryManager {
    private const val TAG = "MulberryHistoryManager"
    private const val HISTORY_FILE_NAME = "activity_events.json"
    private const val COLLECTIONS_FILE_NAME = "collections.json"

    private val gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }

    private val _historyFlow = MutableStateFlow<ActivityEventsIndex?>(null)
    val historyFlow: StateFlow<ActivityEventsIndex?> = _historyFlow.asStateFlow()

    private val _collectionsFlow = MutableStateFlow<List<CustomCollectionItem>>(emptyList())
    val collectionsFlow: StateFlow<List<CustomCollectionItem>> = _collectionsFlow.asStateFlow()

    fun getHistoryDirectory(context: Context): File {
        val baseDir = MulberryStorageManager.getMulberryDirectory(context)
        val historyDir = File(baseDir, ".history")
        if (!historyDir.exists()) {
            historyDir.mkdirs()
        }
        return historyDir
    }

    fun getHistoryJsonFile(context: Context): File {
        return File(getHistoryDirectory(context), HISTORY_FILE_NAME)
    }

    fun getCollectionsJsonFile(context: Context): File {
        return File(getHistoryDirectory(context), COLLECTIONS_FILE_NAME)
    }

    /**
     * Loads the history index synchronously from disk.
     */
    fun loadHistoryIndex(context: Context): ActivityEventsIndex {
        val file = getHistoryJsonFile(context)
        if (!file.exists() || file.length() == 0L) {
            val empty = ActivityEventsIndex(
                storageDirectory = getHistoryDirectory(context).absolutePath
            )
            _historyFlow.value = empty
            return empty
        }
        return try {
            FileReader(file).use { reader ->
                val index = gson.fromJson(reader, ActivityEventsIndex::class.java)
                    ?: ActivityEventsIndex(storageDirectory = getHistoryDirectory(context).absolutePath)
                _historyFlow.value = index
                index
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading history index: ${e.message}", e)
            val fallback = ActivityEventsIndex(storageDirectory = getHistoryDirectory(context).absolutePath)
            _historyFlow.value = fallback
            fallback
        }
    }

    /**
     * Records a new activity/movement event and writes atomically to /sdcard/Mulberry/.history/activity_events.json
     */
    suspend fun recordMovement(
        context: Context,
        eventType: String,
        book: BookEntity? = null,
        page: Int = 0,
        durationMinutes: Int = 0,
        durationSeconds: Long = 0L,
        collectionName: String = "",
        extraMetadata: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val current = loadHistoryIndex(context)
            val totalPages = book?.pageCount ?: 0
            val percent = if (totalPages > 0) ((page.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(0, 100) else 0
            val effSeconds = if (durationSeconds > 0L) durationSeconds else (durationMinutes * 60L)
            val effMinutes = if (durationMinutes > 0) durationMinutes else (effSeconds / 60L).toInt().coerceAtLeast(1)

            val event = UserMovementEvent(
                eventType = eventType,
                timestamp = System.currentTimeMillis(),
                bookSlug = book?.slug ?: "",
                bookTitle = book?.title ?: "",
                bookAuthor = book?.author ?: "",
                bookSubject = book?.subject?.takeIf { it.isNotBlank() && !it.equals("General", ignoreCase = true) } ?: "Medical Core",
                page = page,
                totalPages = totalPages,
                progressPercent = percent,
                durationMinutes = effMinutes,
                durationSeconds = effSeconds,
                collectionName = collectionName,
                metadata = extraMetadata
            )

            val updatedList = current.events + event
            val updatedIndex = current.copy(
                updatedAt = System.currentTimeMillis(),
                totalEvents = updatedList.size,
                events = updatedList
            )

            val file = getHistoryJsonFile(context)
            val tempFile = File(file.parentFile, "$HISTORY_FILE_NAME.tmp")
            FileWriter(tempFile).use { writer ->
                gson.toJson(updatedIndex, writer)
                writer.flush()
            }
            if (file.exists()) file.delete()
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }

            _historyFlow.value = updatedIndex
            Log.d(TAG, "Recorded movement: $eventType for ${book?.title} (subject=${event.bookSubject}, duration=${effSeconds}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record movement event: ${e.message}", e)
        }
    }

    /**
     * Discovers recently opened books purely from /sdcard/Mulberry/.history/activity_events.json
     */
    fun getRecentlyOpenedBooks(context: Context, allBooks: List<BookEntity>): List<BookEntity> {
        val index = _historyFlow.value ?: loadHistoryIndex(context)
        val readEvents = index.events
            .filter { it.bookTitle.isNotBlank() || it.bookSlug.isNotBlank() }
            .sortedByDescending { it.timestamp }

        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<BookEntity>()

        for (event in readEvents) {
            val key = event.bookSlug.ifEmpty { event.bookTitle.lowercase().trim() }
            if (seenKeys.add(key)) {
                val matched = allBooks.find {
                    (it.slug.isNotBlank() && it.slug == event.bookSlug) ||
                    it.title.equals(event.bookTitle, ignoreCase = true)
                }
                if (matched != null) {
                    val updated = matched.copy(
                        lastReadPage = if (event.page > 0) event.page else matched.lastReadPage,
                        progressPercent = if (event.progressPercent > 0) event.progressPercent else matched.progressPercent
                    )
                    result.add(updated)
                } else if (event.bookTitle.isNotBlank()) {
                    result.add(
                        BookEntity(
                            id = (event.bookSlug.hashCode() and 0x7FFFFFFF),
                            title = event.bookTitle,
                            author = event.bookAuthor,
                            subject = event.bookSubject.ifEmpty { "Medical" },
                            lastReadPage = event.page,
                            pageCount = event.totalPages.coerceAtLeast(1),
                            progressPercent = event.progressPercent,
                            isDownloaded = true
                        )
                    )
                }
            }
        }
        return result
    }

    /**
     * Calculates weekly completed reading minutes solely from history events.
     */
    fun calculateWeeklyReadingMinutes(context: Context): Int {
        val index = _historyFlow.value ?: loadHistoryIndex(context)
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = cal.timeInMillis

        return index.events
            .filter { it.timestamp >= startOfWeek }
            .sumOf { it.durationMinutes }
    }

    /**
     * Returns a 7-element boolean array for Mon-Sun representing whether reading occurred on each day of current week.
     */
    fun getWeeklyDayActivity(context: Context): List<Boolean> {
        val index = _historyFlow.value ?: loadHistoryIndex(context)
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)

        val daysWithActivity = BooleanArray(7) { false }

        for (event in index.events) {
            cal.timeInMillis = event.timestamp
            if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear) {
                // Calendar.MONDAY is 2, SUNDAY is 1. Convert to 0=Mon, 1=Tue, ..., 6=Sun
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val mappedIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                daysWithActivity[mappedIndex] = true
            }
        }
        return daysWithActivity.toList()
    }

    /**
     * Loads custom collections from /sdcard/Mulberry/.history/collections.json
     * Strictly NO preloaded collections!
     */
    fun loadCollections(context: Context): List<CustomCollectionItem> {
        val file = getCollectionsJsonFile(context)
        if (!file.exists() || file.length() == 0L) {
            val emptyList = emptyList<CustomCollectionItem>()
            saveCollections(context, emptyList)
            _collectionsFlow.value = emptyList
            return emptyList
        }
        return try {
            FileReader(file).use { reader ->
                val index = gson.fromJson(reader, CollectionsIndex::class.java)
                val rawList = index?.collections ?: emptyList()
                // Filter out any unwanted preloaded collections ("Must Read" and "Exam Prep")
                val cleanedList = rawList.filterNot { col ->
                    col.name.equals("Must Read", ignoreCase = true) ||
                    col.name.equals("Exam Prep", ignoreCase = true) ||
                    col.id == "ee42f806-e7e5-400e-a87d-6962b83915d0" ||
                    col.id == "d758cefb-81c0-4a8a-a508-708a53942595"
                }
                if (cleanedList.size != rawList.size) {
                    saveCollections(context, cleanedList)
                }
                _collectionsFlow.value = cleanedList
                cleanedList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading collections: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Saves updated custom collections to disk.
     */
    fun saveCollections(context: Context, collections: List<CustomCollectionItem>) {
        try {
            val file = getCollectionsJsonFile(context)
            val index = CollectionsIndex(collections = collections)
            FileWriter(file).use { writer ->
                gson.toJson(index, writer)
                writer.flush()
            }
            _collectionsFlow.value = collections
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving collections: ${e.message}", e)
        }
    }

    /**
     * Adds a new collection.
     */
    fun addCollection(context: Context, name: String, description: String = "") {
        val current = loadCollections(context).toMutableList()
        val newCol = CustomCollectionItem(
            name = name.trim(),
            description = description.trim(),
            resourceCount = 0,
            bookSlugs = emptyList()
        )
        current.add(newCol)
        saveCollections(context, current)
    }

    /**
     * Adds a book slug to a specific collection.
     */
    fun addBookToCollection(context: Context, collectionId: String, bookSlug: String) {
        val current = loadCollections(context).toMutableList()
        val index = current.indexOfFirst { it.id == collectionId }
        if (index != -1) {
            val col = current[index]
            if (!col.bookSlugs.contains(bookSlug)) {
                val updatedSlugs = col.bookSlugs + bookSlug
                current[index] = col.copy(
                    bookSlugs = updatedSlugs,
                    resourceCount = updatedSlugs.size
                )
                saveCollections(context, current)
            }
        }
    }

    /**
     * Removes a book slug from a specific collection.
     */
    fun removeBookFromCollection(context: Context, collectionId: String, bookSlug: String) {
        val current = loadCollections(context).toMutableList()
        val index = current.indexOfFirst { it.id == collectionId }
        if (index != -1) {
            val col = current[index]
            val updatedSlugs = col.bookSlugs.filterNot { it == bookSlug }
            current[index] = col.copy(
                bookSlugs = updatedSlugs,
                resourceCount = updatedSlugs.size
            )
            saveCollections(context, current)
        }
    }

    /**
     * Deletes a collection completely.
     */
    fun deleteCollection(context: Context, collectionId: String) {
        val current = loadCollections(context).filterNot { it.id == collectionId }
        saveCollections(context, current)
    }
}
