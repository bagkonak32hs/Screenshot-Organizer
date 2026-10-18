package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AnalysisResultEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ScreenshotEntity
import com.example.data.model.TagEntity

@Database(
    entities = [
        ScreenshotEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ReminderEntity::class,
        AnalysisResultEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ScreenshotDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenshotDatabase? = null

        fun getInstance(context: Context): ScreenshotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenshotDatabase::class.java,
                    "screenshot_organizer.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
