package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.ScheduleDao
import com.example.data.local.dao.SnipDao
import com.example.data.local.entities.BookEntity
import com.example.data.local.entities.DailyScheduleEntity
import com.example.data.local.entities.SnipEntity

@Database(
    entities = [
        BookEntity::class,
        DailyScheduleEntity::class,
        SnipEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MulberryDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun snipDao(): SnipDao

    companion object {
        @Volatile
        private var INSTANCE: MulberryDatabase? = null

        fun getInstance(context: Context): MulberryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MulberryDatabase::class.java,
                    "mulberry_ephemeral_db.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
