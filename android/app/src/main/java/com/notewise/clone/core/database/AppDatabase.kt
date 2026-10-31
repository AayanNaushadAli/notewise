package com.notewise.clone.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [NotebookEntity::class, PageEntity::class, FolderEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(StrokeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notebookDao(): NotebookDao
    abstract fun pageDao(): PageDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notewise_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
