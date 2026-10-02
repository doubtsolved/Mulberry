package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Query("SELECT * FROM books WHERE vaultId = :vaultId ORDER BY title ASC")
    fun getBooksByVault(vaultId: Int): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE vaultId = :vaultId ORDER BY title ASC")
    suspend fun getBooksByVaultSync(vaultId: Int): List<BookEntity>

    @Query("SELECT * FROM books WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC LIMIT 6")
    fun getContinueReadingBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Update
    suspend fun updateBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE uriString IN (:paths)")
    suspend fun deleteBooksByPaths(paths: List<String>)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE books SET lastReadPage = :page, progressPercent = :percent, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Int, page: Int, percent: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET lastReadPage = :lastPage, pageCount = :totalPages, progressPercent = CAST(:progressPercent AS INTEGER), lastReadTimestamp = :lastReadTimestamp WHERE uriString = :bookPath")
    suspend fun updateProgress(bookPath: String, lastPage: Int, totalPages: Int, progressPercent: Float, lastReadTimestamp: Long)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE vaultId = :vaultId")
    suspend fun deleteBooksByVault(vaultId: Int)

    @Query("DELETE FROM books")
    suspend fun clearAllBooks()
}
