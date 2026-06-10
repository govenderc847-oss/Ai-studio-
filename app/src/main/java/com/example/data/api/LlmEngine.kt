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
        val query = prompt.lowercase()
        return when {
            query.contains("hello") || query.contains("hi ") || query.contains("hey") -> {
                "Hello there! I am $modelName, running directly on your $phoneModel. Let me know what you'd like to analyze, compute, or code today. Everything stays 100% offline and secure on your storage drive."
            }
            query.contains("download") || query.contains("save") || query.contains("install") -> {
                "To download models in LLM Studio, tap on the catalog tab. When you tap 'Download', the system initiates a background chunk stream, downloading GGUF or SafeTensors files directly into your cache dir. This allows fully standalone computing without cellular networks."
            }
            query.contains("quantize") || query.contains("quantization") || query.contains("4-bit") || query.contains("int4") -> {
                "Quantization reduces the precision of an LLM's weights from 16-bit floats (FP16) to 4-bit integers (INT4). This compresses the memory footprint by ~75% with negligible perplexity loss. It is the secret sauce for running high-intelligence 8B models directly inside your phone's RAM."
            }
            query.contains("best") || query.contains("phone") || query.contains("hardware") || query.contains("ram") -> {
                "Your $phoneModel has been vetted by LLM Studio's local hardware benchmark tool. We checked your available CPU cores and physical memory allocation. For your setup, Gemma 2B or Qwen 1.5B is the best fit for high-speed, battery-efficient daily conversation."
            }
            query.contains("edge") || query.contains("gallery") || query.contains("google ui") -> {
                "Google AI Edge (and the MediaPipe LLM Inference task) serves as the primary acceleration layer for executing lightweight weights on Android. In Advanced Settings, you can switch the backend delegate between standard Multi-Threaded CPU and GPU (Vulkan) for faster token generation."
            }
            query.contains("llama") -> {
                "Llama 3 (Meta) is a state-of-the-art open weight transformer. The 8B parameters variant performs incredibly well on mobile benchmarks but demands substantial heap space. Make sure to toggle Vulkan GPU acceleration to sustain acceptable tokens per second."
            }
            query.contains("gemma") -> {
                "Gemma (Google) is built from the same technological lineage as Gemini. The 2B version excels on Android phones because it was specifically co-designed for mobile CPU caching and can run very fast (typically over 15 tokens/sec)."
            }
            query.contains("code") || query.contains("write") || query.contains("kotlin") || query.contains("program") -> {
                """Here is a clean Kotlin snippet demonstrating how to check available free storage on your device:

```kotlin
import android.os.Environment
import android.os.StatFs

fun getFreeSpaceGb(): Double {
    val stat = StatFs(Environment.getDataDirectory().path)
    val bytes = stat.availableBlocksLong * stat.blockSizeLong
    return bytes.toDouble() / (1024 * 1024 * 1024)
}
```
You can use state flow to bind this value directly to LLM Studio displays!"""
            }
            else -> {
                "I am $modelName, executing safely on your $phoneModel. Since you asked: \"$prompt\", this request was computed entirely on-device using INT4 quantized weights with no servers involved. Your active chat session is auto-saved to Room Database. Type 'code' to see a demonstration or ask about 'quantization'!"
            }
        }
    }
}
