package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AishaMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM aisha_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<AishaMemory>>

    @Query("SELECT * FROM aisha_memories ORDER BY timestamp DESC")
    suspend fun getAllMemoriesList(): List<AishaMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AishaMemory): Long

    @Update
    suspend fun updateMemory(memory: AishaMemory)

    @Delete
    suspend fun deleteMemory(memory: AishaMemory)

    @Query("DELETE FROM aisha_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM aisha_memories")
    suspend fun clearAllMemories()

    @Query("SELECT COUNT(*) FROM aisha_memories")
    suspend fun getMemoryCount(): Int
}
