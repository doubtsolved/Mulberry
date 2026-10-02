package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        VaultEntity::class,
        BookEntity::class,
        AgendaTaskEntity::class,
        ExamEntity::class,
        AnnotationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MulberryDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun bookDao(): BookDao
    abstract fun agendaTaskDao(): AgendaTaskDao
    abstract fun examDao(): ExamDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: MulberryDatabase? = null

        fun getInstance(context: Context): MulberryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MulberryDatabase::class.java,
                    "mulberry_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
