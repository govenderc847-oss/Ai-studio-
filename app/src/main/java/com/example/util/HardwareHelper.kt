package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class PhoneSpecs(
    val manufacturer: String,
    val model: String,
    val totalRamGb: Double,
    val freeStorageGb: Double,
    val cpuCores: Int,
    val cpuArch: String,
    val recommendedModelId: String,
    val recommendedModelName: String
)

object HardwareHelper {
    fun getPhoneSpecs(context: Context): PhoneSpecs {
        // Manufacturer and Model
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val ramGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)

        // Storage info
        val filesDir = context.filesDir
        val freeBytes = filesDir.freeSpace
        val freeGb = freeBytes.toDouble() / (1024 * 1024 * 1024)

        // CPU specs
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = System.getProperty("os.arch") ?: "arm64"

        // Determine recommended model based on total RAM
        val (recommendedId, recommendedName) = when {
            ramGb < 4.0 -> Pair("tinyllama-1.1b-instruct", "TinyLlama 1.1B (Ultra Light)")
            ramGb < 8.0 -> Pair("gemma-2b-it", "Gemma 2B (Optimized for Multi-Core CPUs)")
            ramGb < 12.0 -> Pair("phi-3-mini", "Phi-3 Mini 3.8B (High Intelligence)")
            else -> Pair("llama-3-8b-it", "Llama 3 8B (Maximum Capacity)")
        }

        return PhoneSpecs(
            manufacturer = manufacturer,
            model = model,
            totalRamGb = ramGb,
            freeStorageGb = freeGb,
            cpuCores = cores,
            cpuArch = arch,
            recommendedModelId = recommendedId,
            recommendedModelName = recommendedName
        )
    }
}
