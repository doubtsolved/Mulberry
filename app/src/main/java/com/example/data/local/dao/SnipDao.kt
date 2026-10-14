package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SnipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnipDao {
    @Query("SELECT * FROM snips ORDER BY createdAt DESC")
    fun getAllSnips(): Flow<List<SnipEntity>>

    @Query("SELECT * FROM snips WHERE subject = :subject ORDER BY createdAt DESC")
    fun getSnipsBySubject(subject: String): Flow<List<SnipEntity>>

    @Query("SELECT * FROM snips WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getSnipsForBook(bookId: String): Flow<List<SnipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnip(snip: SnipEntity): Long

    @Query("DELETE FROM snips WHERE id = :id")
    suspend fun deleteSnip(id: Long)
}
