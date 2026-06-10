package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LlmRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val modelDao: ModelDao
) {
    val allThreads: Flow<List<ChatThread>> = chatDao.getAllThreads()
    val allModels: Flow<List<DownloadedModel>> = modelDao.getAllModels()

    suspend fun checkAndSeedModels() {
        val existing = modelDao.getAllModelsDirect()
        if (existing.isEmpty()) {
            val starterModels = listOf(
                DownloadedModel(
                    id = "tinyllama-1.1b-instruct",
                    name = "TinyLlama 1.1B Instruct",
                    parameterCount = "1.1 Billion",
                    quantization = "Q4_K_M (INT4)",
                    sizeBytes = 680_000_000L,
                    description = "Ultra-lightweight chatbot. Optimized to run fluidly on low-end ARM CPUs. Consumes ~800MB RAM.",
                    isDownloaded = false
                ),
                DownloadedModel(
                    id = "qwen2.5-1.5b-chat",
                    name = "Qwen 2.5 1.5B Chat",
                    parameterCount = "1.5 Billion",
                    quantization = "Q4_0 (INT4)",
                    sizeBytes = 980_000_000L,
                    description = "Highly capable multilingual model. Excellent reasoning-to-size ratio. Consumes ~1.2GB RAM.",
                    isDownloaded = false
                ),
                DownloadedModel(
                    id = "gemma-2b-it",
                    name = "Gemma 2B Instruct (Google)",
                    parameterCount = "2.1 Billion",
                    quantization = "Q4_K_S (INT4)",
                    sizeBytes = 1_450_000_000L,
                    description = "Google's open-weights model. Fantastic command following and general tasks. Consumes ~1.8GB RAM.",
                    isDownloaded = false
                ),
                DownloadedModel(
                    id = "phi-3-mini",
                    name = "Phi-3 Mini Instruct (Microsoft)",
                    parameterCount = "3.8 Billion",
                    quantization = "Q4_K_M (INT4)",
                    sizeBytes = 2_200_000_000L,
                    description = "Microsoft's high-intelligence edge model. Incredible logic, math, and code. Consumes ~2.8GB RAM.",
                    isDownloaded = false
                ),
                DownloadedModel(
                    id = "llama-3-8b-it",
                    name = "Llama 3 8B Chat (Meta)",
                    parameterCount = "8.0 Billion",
                    quantization = "Q4_0 (INT4)",
                    sizeBytes = 4_700_000_000L,
                    description = "Flagship edge model by Meta. Unmatched depth and expressiveness. Recommended only for flagships with 12GB+ RAM.",
                    isDownloaded = false
                )
            )
            modelDao.insertModels(starterModels)
        }
    }

    suspend fun getModelById(id: String): DownloadedModel? {
        return modelDao.getModelById(id)
    }

    suspend fun createThread(title: String, modelId: String): Long {
        return chatDao.insertThread(ChatThread(title = title, selectedModelId = modelId))
    }

    suspend fun deleteThread(thread: ChatThread) {
        messageDao.deleteMessagesForThread(thread.id)
        chatDao.deleteThread(thread)
    }

    suspend fun deleteThreadById(threadId: Int) {
        messageDao.deleteMessagesForThread(threadId)
        chatDao.deleteThreadById(threadId)
    }

    fun getMessagesForThread(threadId: Int): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForThread(threadId)
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun updateModelDownloadState(id: String, isDownloaded: Boolean, isDownloading: Boolean, progress: Float) {
        modelDao.updateDownloadState(id, isDownloaded, isDownloading, progress)
    }

    suspend fun registerCustomModel(model: DownloadedModel) {
        modelDao.insertModel(model)
    }

    suspend fun deleteModel(id: String) {
        val model = modelDao.getModelById(id)
        if (model != null) {
            modelDao.updateDownloadState(id, isDownloaded = false, isDownloading = false, progress = 0f)
        }
    }
}
