package com.example.kt4_8

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val watermarkedPath = inputData.getString("watermarked_path") ?: "default_path"

            // Имитация загрузки
            for (i in 1..10) {
                if (!currentCoroutineContext().isActive) return Result.failure()
                delay(400)
                setProgress(workDataOf("progress" to i * 10))
                Log.d("WorkManager", "Upload: ${i * 10}%")
            }

            val uploadedUrl = "https://cloud.com/photos/photo_${System.currentTimeMillis()}.jpg"

            Result.success(workDataOf(
                "uploaded_url" to uploadedUrl,
                "final_path" to watermarkedPath
            ))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to "Upload failed: ${e.message}"))
        }
    }
}