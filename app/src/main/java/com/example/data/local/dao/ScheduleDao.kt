package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.DailyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM daily_schedules WHERE targetDateEpochDay = :epochDay ORDER BY id ASC")
    fun getSchedulesForDay(epochDay: Long): Flow<List<DailyScheduleEntity>>

    @Query("SELECT * FROM daily_schedules ORDER BY targetDateEpochDay DESC, id ASC")
    fun getAllSchedules(): Flow<List<DailyScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: DailyScheduleEntity): Long

    @Query("UPDATE daily_schedules SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM daily_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Long)
}
