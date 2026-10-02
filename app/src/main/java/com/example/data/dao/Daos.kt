package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AgendaTaskEntity
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.ExamEntity
import com.example.data.model.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults ORDER BY addedAt DESC")
    fun getAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults ORDER BY addedAt DESC")
    suspend fun getAllVaultsSync(): List<VaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: VaultEntity): Long

    @Update
    suspend fun updateVault(vault: VaultEntity)

    @Delete
    suspend fun deleteVault(vault: VaultEntity)

    @Query("DELETE FROM vaults WHERE id = :id")
    suspend fun deleteVaultById(id: Int)
}

@Dao
interface AgendaTaskDao {
    @Query("SELECT * FROM agenda_tasks ORDER BY isCompleted ASC, orderIndex ASC, id ASC")
    fun getAllTasks(): Flow<List<AgendaTaskEntity>>

    @Query("SELECT * FROM agenda_tasks ORDER BY isCompleted ASC, orderIndex ASC, id ASC")
    suspend fun getAllTasksSync(): List<AgendaTaskEntity>

    @Query("SELECT * FROM agenda_tasks WHERE dateString = :date ORDER BY isCompleted ASC, orderIndex ASC")
    fun getTasksByDate(date: String): Flow<List<AgendaTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgendaTaskEntity): Long

    @Update
    suspend fun updateTask(task: AgendaTaskEntity)

    @Query("UPDATE agenda_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setTaskCompleted(id: Int, isCompleted: Boolean)

    @Delete
    suspend fun deleteTask(task: AgendaTaskEntity)

    @Query("DELETE FROM agenda_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDateMillis ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Int)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND pageIndex = :pageIndex ORDER BY timestamp ASC")
    fun getAnnotationsForPage(bookId: Int, pageIndex: Int): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Int)

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND pageIndex = :pageIndex ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastAnnotation(bookId: Int, pageIndex: Int): AnnotationEntity?

    @Query("DELETE FROM annotations WHERE id IN (SELECT id FROM annotations WHERE bookId = :bookId AND pageIndex = :pageIndex ORDER BY timestamp DESC LIMIT 1)")
    suspend fun undoLastAnnotation(bookId: Int, pageIndex: Int)

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getAllAnnotationsForBook(bookId: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getAnnotationsListForBook(bookId: Int): List<AnnotationEntity>

    @Query("DELETE FROM annotations WHERE bookId = :bookId")
    suspend fun deleteAllAnnotationsForBook(bookId: Int)
}
