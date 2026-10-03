package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AishaMemory
import com.example.data.model.ChatMessageEntity

@Database(
    entities = [AishaMemory::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AishaDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AishaDatabase? = null

        fun getDatabase(context: Context): AishaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AishaDatabase::class.java,
                    "aisha_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
