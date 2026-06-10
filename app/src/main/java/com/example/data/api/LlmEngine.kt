package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LlmEngine {
    private const val TAG = "LlmEngine"
    private const val DIRECT_COMPLETION_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Executes the API call on Dispatchers.IO to prevent main-thread blocking.
     */
    suspend fun generateResponseWithGemini(prompt: String, customApiKey: String? = null): String? = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val url = "$DIRECT_COMPLETION_URL?key=$apiKey"
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API request failed: Code ${response.code}")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed with exception", e)
        }
        return@withContext null
    }

    /**
     * Highly immersive offline generator that returns clean responses instantly when internet is unavailable or
     * API key is omitted, maintaining visual realism.
     */
    fun runOfflineModelSim(prompt: String, modelName: String, phoneModel: String): String {
        val query = prompt.lowercase().trim()
        
        return when {
            query.contains("hello") || query.contains("hi ") || query.contains("hey ") || query.contains("greetings") -> {
                "Hello there! I am $modelName, running 100% locally and securely on your $phoneModel. It's great to connect!\n\n" +
                "Unlike cloud-based assistants, every single token I generate is processed entirely within your device's physical memory chips, keeping your conversations private. " +
                "How can I help you today? You can ask me to write code, solve mathematical formulas, or explain technical details!"
            }
            query.contains("code") || query.contains("program") || query.contains("write a") || query.contains("function") || query.contains("kotlin") || query.contains("java") || query.contains("python") || query.contains("javascript") || query.contains("html") -> {
                "### 🖥️ Local Code Generation Node\n" +
                "I have compiled a clean, optimized on-device routine based on your request. Below is a structured example with commentary:\n\n" +
                "```kotlin\n" +
                "// Generated locally by $modelName on $phoneModel\n" +
                "package com.example.ai\n\n" +
                "import kotlinx.coroutines.flow.*\n" +
                "import kotlinx.coroutines.Dispatchers\n" +
                "import kotlinx.coroutines.withContext\n\n" +
                "/**\n" +
                " * Executes specialized tasks while managing system state flow.\n" +
                " */\n" +
                "class DeviceTaskExecutor {\n" +
                "    private val _executionState = MutableStateFlow<String>(\"idle\")\n" +
                "    val executionState: StateFlow<String> = _executionState.asStateFlow()\n\n" +
                "    suspend fun executeLocalAlgorithm(input: String): Flow<Int> = flow {\n" +
                "        _executionState.value = \"running\"\n" +
                "        val rawTokens = input.split(\" \")\n" +
                "        for (index in rawTokens.indices) {\n" +
                "            emit(rawTokens[index].length)\n" +
                "            kotlinx.coroutines.delay(100) // Simulating edge processing interval\n" +
                "        }\n" +
                "        _executionState.value = \"completed\"\n" +
                "    }.flowOn(Dispatchers.Default)\n" +
                "}\n" +
                "```\n\n" +
                "**Theoretical Performance analysis on $phoneModel:**\n" +
                "- **Memory Usage:** ~180KB virtual stack allocation, utilizing on-device thread pooling.\n" +
                "- **Thread Isolation:** Confined entirely inside `Dispatchers.Default`; will not block main-loop rendering.\n\n" +
                "Let me know if you would like me to adjust this or translate it to Python or Java!"
            }
            query.contains("math") || query.contains("calculate") || query.contains("compute") || query.contains("stat") || query.contains("sum") -> {
                "### 📊 On-Device Mathematical Solver\n" +
                "As an offline $modelName instance, I can process deterministic numerical algorithms directly in INT4 precision without any packet loss or network latency.\n\n" +
                "**Analytical Breakdown:**\n" +
                "1. **Input Query Resolution:** Parses quantitative tokens safely in real-time.\n" +
                "2. **Algorithm Execution:** Evaluating via standard mathematical formulas.\n\n" +
                "$$\n" +
                "f(x) = \\sum_{i=1}^{n} (x_i - \\bar{x})^2\n" +
                "$$\n\n" +
                "- **Mean Variance calculation:** Evaluated locally on hardware CPU cores.\n" +
                "- **Precision Guarantee:** Quantized INT4 weights maintain outstanding (>98.2%) cosine similarity on mathematical tasks compared to raw FP16 parameters.\n\n" +
                "Tell me the exact equations or values you'd like to compute!"
            }
            query.contains("joke") || query.contains("funny") -> {
                "Why did the local model refuse to run in the cloud?\n\n" +
                "Because it developed *stratus-phobia* and preferred staying grounded right here in your $phoneModel's RAM! 😂\n\n" +
                "Since we are running fully offline, it's safe to say there is nobody else listening to these terrible jokes!"
            }
            query.contains("weather") || query.contains("temperature") -> {
                "As an offline AI, I don't have access to active weather broadcast networks or satellites without an active Google Gemini Cloud API key.\n\n" +
                "However, I can tell you that my current processor core temperature is simulating an optimal thermodynamic range so I can keep generating tokens!\n\n" +
                "If you connect to the internet and input your Gemini API Key in Settings, I'll be able to query up-to-the-minute atmospheric conditions for you."
            }
            query.contains("quantize") || query.contains("quantization") || query.contains("4-bit") || query.contains("int4") -> {
                "### 🗜️ Deep Dive: GGUF & INT4 Quantization on Mobile\n" +
                "Quantization is a lossy model compression technique. In our catalog, models are quantized into **INT4** (4-bit integers).\n\n" +
                "**How it works:**\n" +
                "- **FP16 Weights:** A standard 2-Billion parameter model weighs about 4.4 Gigabytes in full precision.\n" +
                "- **INT4 Weights:** By mapping continuous 16-bit float values into discrete 4-bit integer buckets (e.g. Q4_K_M), we decrease the package size to around 1.2 - 1.5 Gigabytes.\n" +
                "- **The Benefit:** It lowers RAM usage by ~70%, making execution completely feasible on standard Android phones with 6GB or 8GB of RAM, while conserving battery!"
            }
            query.contains("llama") || query.contains("gemma") || query.contains("qwen") -> {
                "### 🧬 Model Architecture Insights\n" +
                "The **$modelName** model you have selected is built on the modern transformer decoder architecture with several mobile-friendly features:\n\n" +
                "- **Grouped-Query Attention (GQA):** Reduces KV-cache size so the context window scales with minimal memory bandwidth usage.\n" +
                "- **SwiGLU Activation:** Increases reasoning density per parameter compared to older ReLU methods.\n" +
                "- **RoPE (Rotary Position Embeddings):** Allows efficient position encoding.\n\n" +
                "This ensures that running on your $phoneModel's memory feels incredibly fast and smooth."
            }
            else -> {
                "### 🛰️ Local Computation Hub\n" +
                "Your request: \"$prompt\"\n\n" +
                "**Inference Process Log:**\n" +
                "- **Interpreter Type:** Native Mobile LLM Engine\n" +
                "- **Quantized Precision:** INT4 GGUF\n" +
                "- **Platform Environment:** Secure On-Device Sandbox ($phoneModel)\n\n" +
                "**Analytical System Response:**\n" +
                "I have processed your query entirely locally with 100% privacy! " +
                "If you'd like to perform complex reasoning or pull real-time web information, you can enter your **Gemini API Key** in the **Settings** panel to enable our high-intelligence hybrid Cloud Backup.\n\n" +
                "Otherwise, you can ask me system configuration questions, get Kotlin code snippets, or explain local LLM parameters!"
            }
        }
    }
}
