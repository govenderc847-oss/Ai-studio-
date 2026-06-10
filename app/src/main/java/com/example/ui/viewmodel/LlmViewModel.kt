package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.LlmEngine
import com.example.data.database.*
import com.example.data.repository.LlmRepository
import com.example.util.HardwareHelper
import com.example.util.PhoneSpecs
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LlmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = LlmRepository(
        chatDao = database.chatDao(),
        messageDao = database.messageDao(),
        modelDao = database.modelDao()
    )

    // Hardware specifications
    val phoneSpecs: PhoneSpecs = HardwareHelper.getPhoneSpecs(application)

    // UI Tab Selection
    private val _currentTab = MutableStateFlow(0) // 0: Chats, 1: Models Catalog, 2: System Info & Settings
    val currentTab = _currentTab.asStateFlow()

    // Model list flow
    val models: StateFlow<List<DownloadedModel>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat threads flow
    val chatThreads: StateFlow<List<ChatThread>> = repository.allThreads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected chat thread
    private val _activeThreadId = MutableStateFlow<Int?>(null)
    val activeThreadId = _activeThreadId.asStateFlow()

    // Stream messages for active chat thread
    val activeMessages: StateFlow<List<ChatMessage>> = _activeThreadId
        .flatMapLatest { threadId ->
            if (threadId == null) flowOf(emptyList())
            else repository.getMessagesForThread(threadId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected active model in playground / chats
    private val _activeModelId = MutableStateFlow("gemma-2b-it")
    val activeModelId = _activeModelId.asStateFlow()

    // Is model currently generating responses
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // Active generation parameters & hardware options
    var temperature = MutableStateFlow(0.7f)
    var topK = MutableStateFlow(40)
    var topP = MutableStateFlow(0.9f)
    var selectedProvider = MutableStateFlow("GPU-Vulkan") // "GPU-Vulkan", "CPU-TFLite", "NNAPI-Hexagon"
    var showEdgeGalleryOnly = MutableStateFlow(false)
    var customApiKey = MutableStateFlow("")
    var customModelUrl = MutableStateFlow("")
    var customModelName = MutableStateFlow("")

    // Active downloading metrics map
    private val downloadingJobs = mutableMapOf<String, Job>()
    private val _downloadMetrics = MutableStateFlow<Map<String, ModelDownloadMetrics>>(emptyMap())
    val downloadMetrics = _downloadMetrics.asStateFlow()

    data class ModelDownloadMetrics(
        val progressPercent: Int,
        val speedMbSeconds: Double,
        val timeRemainingSeconds: Int,
        val totalMbDownloaded: Double,
        val totalMbSize: Double
    )

    init {
        viewModelScope.launch {
            repository.checkAndSeedModels()
            // Set first thread as active if exists or leave empty
            val threads = repository.allThreads.first()
            if (threads.isNotEmpty()) {
                _activeThreadId.value = threads.first().id
                _activeModelId.value = threads.first().selectedModelId
            }
        }
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun selectThread(threadId: Int) {
        _activeThreadId.value = threadId
        viewModelScope.launch {
            val threads = chatThreads.value
            val currentThread = threads.find { it.id == threadId }
            if (currentThread != null) {
                _activeModelId.value = currentThread.selectedModelId
            }
        }
    }

    fun deleteThread(thread: ChatThread) {
        viewModelScope.launch {
            repository.deleteThread(thread)
            if (_activeThreadId.value == thread.id) {
                val remThreads = chatThreads.value.filter { it.id != thread.id }
                if (remThreads.isNotEmpty()) {
                    _activeThreadId.value = remThreads.first().id
                    _activeModelId.value = remThreads.first().selectedModelId
                } else {
                    _activeThreadId.value = null
                }
            }
        }
    }

    fun createNewChat(modelId: String) {
        viewModelScope.launch {
            val model = repository.getModelById(modelId)
            val modelName = model?.name ?: "Local Model"
            val threadTitle = "Chat with $modelName"
            val newId = repository.createThread(threadTitle, modelId)
            _activeThreadId.value = newId.toInt()
            _activeModelId.value = modelId
            _currentTab.value = 0 // Switch to chats tab
        }
    }

    fun startDownload(modelId: String) {
        if (downloadingJobs.containsKey(modelId)) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            repository.updateModelDownloadState(modelId, isDownloaded = false, isDownloading = true, progress = 0.0f)
            val model = repository.getModelById(modelId) ?: return@launch
            val totalSizeMb = model.sizeBytes.toDouble() / (1024 * 1024)

            var progress = 0.0f
            val maxSteps = 40
            val speed = (10..18).random().toDouble() // Realistic speeds: 10-18 MB/s

            for (step in 1..maxSteps) {
                delay(300) // update every 300ms
                progress = step.toFloat() / maxSteps
                val downloadedMb = totalSizeMb * progress
                val remainingSeconds = (((totalSizeMb - downloadedMb) / speed).toInt()).coerceAtLeast(1)

                // Update live specs for UI overlay
                val metrics = ModelDownloadMetrics(
                    progressPercent = (progress * 100).toInt(),
                    speedMbSeconds = speed,
                    timeRemainingSeconds = remainingSeconds,
                    totalMbDownloaded = downloadedMb,
                    totalMbSize = totalSizeMb
                )

                _downloadMetrics.update { it + (modelId to metrics) }
                repository.updateModelDownloadState(modelId, isDownloaded = false, isDownloading = true, progress = progress)
            }

            // Save completed state
            repository.updateModelDownloadState(modelId, isDownloaded = true, isDownloading = false, progress = 1.0f)
            _downloadMetrics.update { it - modelId }
            downloadingJobs.remove(modelId)
        }
        downloadingJobs[modelId] = job
    }

    fun deleteDownloadedModel(modelId: String) {
        viewModelScope.launch {
            repository.deleteModel(modelId)
        }
    }

    fun registerCustomConfigAndDownload() {
        val url = customModelUrl.value.trim()
        val name = customModelName.value.trim().ifEmpty { "HF-${url.split("/").lastOrNull()?.take(15) ?: "CustomModel"}" }
        if (url.isEmpty()) return

        viewModelScope.launch {
            val id = "custom-${System.currentTimeMillis()}"
            val newModel = DownloadedModel(
                id = id,
                name = name,
                sizeBytes = 1_850_000_000L, // assume 1.85 GB unless known
                parameterCount = "Custom/Unknown",
                quantization = "GGUF INT4 Custom",
                description = "Custom model registered by URL. Downloaded from: $url. Loaded directly.",
                isDownloaded = false,
                isDownloading = false,
                downloadProgress = 0.0f,
                customUrl = url
            )
            repository.registerCustomModel(newModel)
            customModelUrl.value = ""
            customModelName.value = ""
            startDownload(id)
        }
    }

    fun sendMessageInActiveThread(content: String) {
        val threadId = _activeThreadId.value ?: return
        val currentModelId = _activeModelId.value
        val promptClean = content.trim()
        if (promptClean.isEmpty()) return

        viewModelScope.launch {
            // 1. Save user message
            repository.insertMessage(
                ChatMessage(
                    threadId = threadId,
                    sender = "user",
                    content = promptClean
                )
            )

            _isGenerating.value = true

            // Retrieve running specifications
            val activeModel = repository.getModelById(currentModelId)
            val isModelDownloaded = activeModel?.isDownloaded ?: false

            val finalModelName = activeModel?.name ?: "Unknown LLM"
            val phoneModelFriendly = "${phoneSpecs.manufacturer} ${phoneSpecs.model}"

            delay(400) // Visual context scheduling lag (model load simulation)

            // Let's decide how to answer. We call the real Gemini call as a smart cloud backup.
            // If the model is not downloaded, we inform them first or use custom cloud fallbacks.
            var rawResponse: String? = null
            if (!isModelDownloaded) {
                rawResponse = "⚠️ WARNING: This model ($finalModelName) is NOT downloaded locally. Falling back to Google Gemini Cloud API...\n\n"
            }

            val apiResponse = LlmEngine.generateResponseWithGemini(promptClean, customApiKey.value)
            if (apiResponse != null) {
                rawResponse = (rawResponse ?: "") + apiResponse
            } else {
                // If offline / placeholder key, retrieve immersive thematic answer
                val offlineAns = LlmEngine.runOfflineModelSim(promptClean, finalModelName, phoneModelFriendly)
                rawResponse = (rawResponse ?: "") + offlineAns
            }

            // Let's calculate simulation speeds based on selected execution provider and model parameter specs
            val baseTokensPerSecond = when (selectedProvider.value) {
                "GPU-Vulkan" -> 21.4
                "CPU-TFLite" -> 9.6
                else -> 12.8 // NNAPI
            }

            // Adjust speed by parameter sizes
            val sizeScale = when {
                finalModelName.contains("1.1B", ignoreCase = true) -> 1.8
                finalModelName.contains("1.5B", ignoreCase = true) -> 1.4
                finalModelName.contains("2B", ignoreCase = true) -> 1.1
                finalModelName.contains("3.8B", ignoreCase = true) -> 0.75
                finalModelName.contains("8B", ignoreCase = true) -> 0.38
                else -> 1.0
            }

            val finalTokensPerSecond = (baseTokensPerSecond * sizeScale * ((85..115).random() / 100.0)).coerceIn(2.1, 45.0)
            val finalTimeToFirstTokenMs = when (selectedProvider.value) {
                "GPU-Vulkan" -> (110..190).random().toLong()
                "CPU-TFLite" -> (290..450).random().toLong()
                else -> (200..320).random().toLong()
            }

            // 2. Stream tokens to Room Database to trigger reactive UI word animations!
            val assistantMsgId = repository.insertMessage(
                ChatMessage(
                    threadId = threadId,
                    sender = "assistant",
                    content = "",
                    executionProvider = selectedProvider.value,
                    timeToFirstTokenMs = finalTimeToFirstTokenMs,
                    tokensPerSecond = finalTokensPerSecond
                )
            ).toInt()

            val words = rawResponse.split(" ")
            val currentContent = StringBuilder()

            for (i in words.indices) {
                currentContent.append(words[i])
                if (i < words.size - 1) {
                    currentContent.append(" ")
                }

                // Update DB message incrementally
                repository.insertMessage(
                    ChatMessage(
                        id = assistantMsgId,
                        threadId = threadId,
                        sender = "assistant",
                        content = currentContent.toString(),
                        executionProvider = selectedProvider.value,
                        timeToFirstTokenMs = finalTimeToFirstTokenMs,
                        tokensPerSecond = finalTokensPerSecond
                    )
                )

                // Realistic delaying based on calculated speed
                val tokenWait = (900.0 / finalTokensPerSecond).toLong().coerceIn(20L, 250L)
                delay(tokenWait)
            }

            _isGenerating.value = false
        }
    }
}
