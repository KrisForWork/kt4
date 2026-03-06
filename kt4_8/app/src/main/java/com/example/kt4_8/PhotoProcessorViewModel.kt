package com.example.kt4_8

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoProcessorViewModel : ViewModel() {

    private val workManager = WorkManager.getInstance()

    private val _photoState = MutableStateFlow(PhotoData())
    val photoState: StateFlow<PhotoData> = _photoState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun startProcessing() {
        _isProcessing.value = true
        _photoState.value = PhotoData(step = 1)

        // Создаем воркеры
        val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
            .setInputData(workDataOf("photo_uri" to "content://media/external/images/media/1"))
            .addTag("photo_processing")
            .addTag("compress") // Добавляем простые теги
            .build()

        val watermarkRequest = OneTimeWorkRequestBuilder<WatermarkWorker>()
            .addTag("photo_processing")
            .addTag("watermark")
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("photo_processing")
            .addTag("upload")
            .build()

        // Создаем уникальную цепочку
        workManager.beginUniqueWork(
            "photo_processing_chain",
            ExistingWorkPolicy.REPLACE,
            compressRequest
        ).then(watermarkRequest)
            .then(uploadRequest)
            .enqueue()

        // Наблюдаем за статусами
        observeWorkInfo()
    }

    private fun observeWorkInfo() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagLiveData("photo_processing")
                .asFlow()
                .collect { workInfos ->
                    if (workInfos.isNotEmpty()) {
                        updateStateFromWorkInfos(workInfos)
                    }
                }
        }
    }

    private fun updateStateFromWorkInfos(workInfos: List<WorkInfo>) {
        // Проверяем на ошибки
        val failedWork = workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }
        if (failedWork != null) {
            _photoState.value = photoState.value.copy(
                error = failedWork.outputData.getString("error") ?: "Unknown error",
                step = -1
            )
            _isProcessing.value = false
            return
        }

        // Проверяем, все ли завершены успешно
        val allSucceeded = workInfos.all { it.state == WorkInfo.State.SUCCEEDED }
        if (allSucceeded && workInfos.size == 3) {
            // Находим UploadWorker по тегу "upload"
            val uploadInfo = workInfos.firstOrNull { it.tags.contains("upload") }
            _photoState.value = photoState.value.copy(
                step = 4,
                uploadedUrl = uploadInfo?.outputData?.getString("uploaded_url") ?: "https://cloud.com/photo.jpg",
                progress = 100
            )
            _isProcessing.value = false
            return
        }

        // Находим активный воркер
        val activeWorkInfo = workInfos.firstOrNull {
            it.state == WorkInfo.State.RUNNING
        }

        if (activeWorkInfo != null) {
            val step = when {
                activeWorkInfo.tags.contains("compress") -> 1
                activeWorkInfo.tags.contains("watermark") -> 2
                activeWorkInfo.tags.contains("upload") -> 3
                else -> 0
            }

            val progress = activeWorkInfo.progress.getInt("progress", 0)

            _photoState.value = photoState.value.copy(
                step = step,
                progress = progress
            )
        }
    }

    fun cancelProcessing() {
        workManager.cancelUniqueWork("photo_processing_chain")
        _isProcessing.value = false
        _photoState.value = PhotoData()
    }
}