package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LlmRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val modelDao: ModelDao,
    private val benchmarkDao: BenchmarkDao
) {
    val allThreads: Flow<List<ChatThread>> = chatDao.getAllThreads()
    val allModels: Flow<List<DownloadedModel>> = modelDao.getAllModels()
    val allBenchmarks: Flow<List<BenchmarkResult>> = benchmarkDao.getAllBenchmarks()

    suspend fun checkAndSeedModels() {
        val starterModels = listOf(
            DownloadedModel(
                id = "tinyllama-1.1b-instruct",
                name = "TinyLlama 1.1B Instruct",
                parameterCount = "1.1 Billion",
                quantization = "Q4_K_M (INT4)",
                sizeBytes = 680_000_000L,
                description = "Ultra-lightweight chatbot. Optimized to run fluidly on low-end ARM CPUs. Consumes ~800MB RAM.",
                isDownloaded = true,
                customUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
            ),
            DownloadedModel(
                id = "qwen2.5-1.5b-chat",
                name = "Qwen 2.5 1.5B Chat",
                parameterCount = "1.5 Billion",
                quantization = "Q4_0 (INT4)",
                sizeBytes = 980_000_000L,
                description = "Highly capable multilingual model. Excellent reasoning-to-size ratio. Consumes ~1.2GB RAM.",
                isDownloaded = true,
                customUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q2_k.gguf"
            ),
            DownloadedModel(
                id = "gemma-2b-it",
                name = "Gemma 2B Instruct (Google)",
                parameterCount = "2.1 Billion",
                quantization = "Q4_K_S (INT4)",
                sizeBytes = 1_450_000_000L,
                description = "Google's open-weights model. Fantastic command following and general tasks. Consumes ~1.8GB RAM.",
                isDownloaded = true,
                customUrl = "https://huggingface.co/lmstudio-community/gemma-2B-it-GGUF/resolve/main/gemma-2b-it-q4_k_m.gguf"
            ),
            DownloadedModel(
                id = "gemma-4-2b-it",
                name = "Gemma 4 2B IT (Beta)",
                parameterCount = "2.5 Billion",
                quantization = "Q4_0_F16 (INT4)",
                sizeBytes = 1_650_000_000L,
                description = "Next-generation Google edge model (Developer Beta). Revolutionary reasoning capabilities, multimodal parsing, and ultra-high coding accuracy. Consumes ~2.1GB RAM.",
                isDownloaded = false,
                customUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"
            ),
            DownloadedModel(
                id = "gemma-4-9b-it",
                name = "Gemma 4 9B IT (Preview)",
                parameterCount = "9.2 Billion",
                quantization = "Q4_K_M (INT4)",
                sizeBytes = 5_400_000_000L,
                description = "SOTA edge intelligence (Developer Preview). Advanced planning, deep logic, and coding prowess. Vetted for flagship chips. Consumes ~6.2GB RAM.",
                isDownloaded = false,
                customUrl = "https://huggingface.co/google/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf"
            ),
            DownloadedModel(
                id = "phi-3-mini",
                name = "Phi-3 Mini Instruct (Microsoft)",
                parameterCount = "3.8 Billion",
                quantization = "Q4_K_M (INT4)",
                sizeBytes = 2_200_000_000L,
                description = "Microsoft's high-intelligence edge model. Incredible logic, math, and code. Consumes ~2.8GB RAM.",
                isDownloaded = false,
                customUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"
            ),
            DownloadedModel(
                id = "llama-3-8b-it",
                name = "Llama 3 8B Chat (Meta)",
                parameterCount = "8.0 Billion",
                quantization = "Q4_0 (INT4)",
                sizeBytes = 4_700_000_000L,
                description = "Flagship edge model by Meta. Unmatched depth and expressiveness. Recommended only for flagships with 12GB+ RAM.",
                isDownloaded = false,
                customUrl = "https://huggingface.co/MaziyarPanahi/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct.Q4_K_M.gguf"
            )
        )
        // Seed or update them to database so current databases pick up the changes
        starterModels.forEach { model ->
            val existingModel = modelDao.getModelById(model.id)
            if (existingModel == null) {
                modelDao.insertModel(model)
            } else {
                // To keep database in sync with user's configurations we heal existing records to defaults if required
                val shouldForceDownloaded = model.id in listOf("tinyllama-1.1b-instruct", "qwen2.5-1.5b-chat", "gemma-2b-it")
                val finalDownloaded = if (shouldForceDownloaded) true else existingModel.isDownloaded
                modelDao.insertModel(existingModel.copy(
                    customUrl = model.customUrl,
                    isDownloaded = finalDownloaded
                ))
            }
        }
    }

    suspend fun insertBenchmark(result: BenchmarkResult) {
        benchmarkDao.insertBenchmark(result)
    }

    suspend fun deleteBenchmark(result: BenchmarkResult) {
        benchmarkDao.deleteBenchmark(result)
    }

    suspend fun clearAllBenchmarks() {
        benchmarkDao.clearAllBenchmarks()
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
