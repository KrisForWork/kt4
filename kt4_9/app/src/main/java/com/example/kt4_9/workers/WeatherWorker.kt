package com.example.kt4_9.workers

import android.content.Context
import androidx.work.*
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(coroutineContext) {
            try {
                val cityName = inputData.getString("city_name") ?: "Unknown"

                // Имитация загрузки погоды (3-7 секунд)
                val delayTime = Random.nextLong(10000, 15000)  // Увеличено с 3-7 до 10-15 секунд
                delay(delayTime)

                // Генерация случайной температуры
                val temperature = Random.nextDouble(-5.0, 25.0)

                // Создаем результат
                val resultData = workDataOf(
                    "city_name" to cityName,
                    "temperature" to temperature
                )

                Result.success(resultData)
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}