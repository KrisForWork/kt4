package com.example.kt4_9

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private const val TAG = "MainViewModel"

data class MainState(
    val isLoading: Boolean = false,
    val status: String = "Нажмите кнопку для начала",
    val details: String = "",
    val result: String = "",
    val progress: Float = 0f
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state

    private val workManager = WorkManager.getInstance()
    private val df = DecimalFormat("#.#")
    private var currentChainId: String? = null

    fun startLoading() {
        Log.d(TAG, "startLoading() called")
        currentChainId = System.currentTimeMillis().toString()
        Log.d(TAG, "New chain ID: $currentChainId")

        _state.value = MainState(
            isLoading = true,
            status = "Загружаем погоду...",
            progress = 0f
        )

        observeWork()
    }

    fun cancelLoading() {
        Log.d(TAG, "cancelLoading() called")

        // Отменяем все воркеры с тегом report (цепочка)
        workManager.cancelAllWorkByTag("report")

        // Также отменяем все воркеры погоды
        workManager.cancelAllWorkByTag("weather_0")
        workManager.cancelAllWorkByTag("weather_1")
        workManager.cancelAllWorkByTag("weather_2")
        workManager.cancelAllWorkByTag("weather_3")

        // Сбрасываем состояние
        _state.value = MainState(
            isLoading = false,
            status = "Готов начать",
            progress = 0f
        )

        Log.d(TAG, "Loading cancelled, state reset")
    }

    private fun observeWork() {
        Log.d(TAG, "observeWork() started for chain: $currentChainId")

        // Наблюдаем за отчётом
        viewModelScope.launch {
            Log.d(TAG, "Starting report observation")
            workManager.getWorkInfosByTagLiveData("report")
                .asFlow()
                .collectLatest { list ->
                    Log.d(TAG, "Report LiveData update: list size=${list.size}")

                    // Берем ПОСЛЕДНИЙ (самый новый) workInfo
                    val latestReport = list
                        .filter { it.tags.contains("report") }
                        .maxByOrNull { it.id.toString() }

                    latestReport?.let { workInfo ->
                        Log.d(TAG, "Latest report workInfo: id=${workInfo.id}, state=${workInfo.state}")
                        Log.d(TAG, "Latest report outputData keys: ${workInfo.outputData.keyValueMap.keys}")

                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> {
                                if (_state.value.isLoading) {
                                    _state.value = _state.value.copy(
                                        status = "Формируем отчёт...",
                                        details = "Собираем данные с городов"
                                    )
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                Log.d(TAG, "Latest report worker SUCCEEDED")

                                val outputData = workInfo.outputData
                                Log.d(TAG, "Output data contents:")
                                outputData.keyValueMap.forEach { (key, value) ->
                                    Log.d(TAG, "  $key = $value")
                                }

                                val avgTemp = outputData.getDouble("average_temp", 0.0)
                                val cities = outputData.getString("cities") ?: ""

                                Log.d(TAG, "Parsed values: avgTemp=$avgTemp, cities='$cities'")

                                val newState = MainState(
                                    isLoading = false,
                                    status = "Готово!",
                                    result = "Средняя температура: ${df.format(avgTemp)}°C\nГорода: $cities",
                                    progress = 1f,
                                    details = ""
                                )
                                Log.d(TAG, "Setting success state: $newState")
                                _state.value = newState
                            }
                            WorkInfo.State.FAILED -> {
                                Log.e(TAG, "Latest report worker FAILED")
                                // Только если это действительно последний и он FAILED
                                if (_state.value.isLoading) {
                                    val newState = MainState(
                                        isLoading = false,
                                        status = "Ошибка загрузки",
                                        result = "Не удалось получить прогноз погоды",
                                        progress = 0f,
                                        details = ""
                                    )
                                    Log.e(TAG, "Setting error state: $newState")
                                    _state.value = newState
                                }
                            }
                            else -> {}
                        }
                    } ?: Log.d(TAG, "No report workInfo found")
                }
        }

        // Отслеживаем прогресс с искусственной задержкой
        viewModelScope.launch {
            var lastProgress = 0f
            while (_state.value.isLoading) {
                updateProgress()

                // Искусственно замедляем обновление прогресса
                val currentProgress = _state.value.progress
                if (currentProgress > lastProgress) {
                    lastProgress = currentProgress
                    // Добавляем дополнительную задержку для визуализации
                    when (currentProgress) {
                        in 0.0..0.25 -> delay(3000)  // Первый город
                        in 0.26..0.5 -> delay(3000)  // Второй город
                        in 0.51..0.75 -> delay(3000) // Третий город
                        in 0.76..0.99 -> delay(3000) // Четвертый город
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    private suspend fun updateProgress() {
        val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")
        val completed = mutableListOf<String>()

        Log.d(TAG, "updateProgress() - checking worker statuses")

        cities.forEachIndexed { index, city ->
            try {
                val infos = workManager.getWorkInfosByTag("weather_$index").get()
                // Берем последний успешный
                val latestSuccess = infos
                    .filter { it.state == WorkInfo.State.SUCCEEDED }
                    .maxByOrNull { it.id.toString() }

                if (latestSuccess != null) {
                    completed.add(city)
                    Log.d(TAG, "  city $city completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking weather_$index", e)
            }
        }

        val progress = if (cities.isNotEmpty()) completed.size.toFloat() / cities.size else 0f
        val details = if (completed.isNotEmpty()) {
            "Готово: ${completed.joinToString(", ")}"
        } else {
            "Ожидание ответа от серверов..."
        }

        Log.d(TAG, "updateProgress: completed=${completed.size}/4, progress=$progress")

        if (_state.value.isLoading) {
            _state.value = _state.value.copy(
                progress = progress,
                details = details,
                status = "Загружаем погоду для ${cities.size} городов..."
            )
        }
    }
}