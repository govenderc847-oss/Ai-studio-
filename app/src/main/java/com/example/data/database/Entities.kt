package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_threads")
data class ChatThread(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val selectedModelId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val threadId: Int,
    val sender: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSecond: Double? = null,
    val timeToFirstTokenMs: Long? = null,
    val executionProvider: String? = null // e.g. "GPU-Vulkan", "CPU-TFLite"
)

@Entity(tableName = "downloaded_models")
data class DownloadedModel(
    @PrimaryKey val id: String,
    val name: String,
    val sizeBytes: Long,
    val parameterCount: String,
    val quantization: String,
    val description: String,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0.0f,
    val customUrl: String? = null
)
