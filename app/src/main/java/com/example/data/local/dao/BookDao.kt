package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY subject ASC, title ASC")
    fun getAllCatalogBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE prof = :prof ORDER BY subject ASC, title ASC")
    fun getBooksByProf(prof: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDownloaded = 1 ORDER BY lastAccessedTimestamp DESC")
    fun getShelfBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("UPDATE books SET isDownloaded = :isDownloaded, localBinPath = :path WHERE id = :id")
    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, path: String?)

    @Query("UPDATE books SET lastReadPage = :page, readingProgressPercent = :progress, lastAccessedTimestamp = :time WHERE id = :id")
    suspend fun updateProgress(id: String, page: Int, progress: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isDownloaded = 0, localBinPath = NULL WHERE id = :id")
    suspend fun removeBookFromShelf(id: String)
}
