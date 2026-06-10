package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads ORDER BY createdAt DESC")
    fun getAllThreads(): Flow<List<ChatThread>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThread): Long

    @Delete
    suspend fun deleteThread(thread: ChatThread)

    @Query("DELETE FROM chat_threads WHERE id = :threadId")
    suspend fun deleteThreadById(threadId: Int)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: Int)
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM downloaded_models")
    fun getAllModels(): Flow<List<DownloadedModel>>

    @Query("SELECT * FROM downloaded_models")
    suspend fun getAllModelsDirect(): List<DownloadedModel>

    @Query("SELECT * FROM downloaded_models WHERE id = :id")
    suspend fun getModelById(id: String): DownloadedModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: DownloadedModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<DownloadedModel>)

    @Update
    suspend fun updateModel(model: DownloadedModel)

    @Query("UPDATE downloaded_models SET isDownloaded = :isDownloaded, isDownloading = :isDownloading, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, isDownloading: Boolean, progress: Float)
}

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(result: BenchmarkResult): Long

    @Delete
    suspend fun deleteBenchmark(result: BenchmarkResult)

    @Query("DELETE FROM benchmark_results")
    suspend fun clearAllBenchmarks()
}
