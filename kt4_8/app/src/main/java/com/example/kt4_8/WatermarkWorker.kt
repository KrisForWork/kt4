package com.example.kt4_8

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class WatermarkWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val compressedPath = inputData.getString("compressed_path") ?: return Result.failure()

            // Имитация добавления водяного знака
            for (i in 1..10) {
                if (!currentCoroutineContext().isActive) return Result.failure()
                delay(250)
                setProgress(workDataOf("progress" to i * 10))
                Log.d("WorkManager", "Watermark: ${i * 10}%")
            }

            val watermarkedPath = "/storage/watermarked_${System.currentTimeMillis()}.jpg"

            Result.success(workDataOf(
                "watermarked_path" to watermarkedPath,
                "compressed_path" to compressedPath
            ))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to "Watermark failed: ${e.message}"))
        }
    }
}