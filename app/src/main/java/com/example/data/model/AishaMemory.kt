package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aisha_memories")
data class AishaMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "general", // "preference", "relationship", "routine", "fact"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isUser: Boolean,
    val text: String,
    val emotion: String = "Loving",
    val commandExecuted: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
