package com.example.kt4_8

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class CompressWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val inputUri = inputData.getString("photo_uri") ?: "default_uri"

            // Имитация сжатия с прогрессом
            for (i in 1..10) {
                if (!currentCoroutineContext().isActive) return Result.failure()
                delay(300)
                setProgress(workDataOf("progress" to i * 10))
                Log.d("WorkManager", "Compress: ${i * 10}%")
            }

            val outputPath = "/storage/compressed_${System.currentTimeMillis()}.jpg"

            Result.success(workDataOf(
                "compressed_path" to outputPath,
                "original_uri" to inputUri
            ))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to "Compression failed: ${e.message}"))
        }
    }
}