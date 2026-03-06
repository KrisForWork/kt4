package com.example.kt4_9.workers

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(coroutineContext) {
            try {
                // Получаем данные из входных параметров
                // ВНИМАНИЕ: В цепочке WorkManager, результаты предыдущих воркеров
                // передаются через inputData объединенные вместе

                val cities = mutableListOf<String>()
                var totalTemp = 0.0
                var count = 0

                // Ищем все ключи, начинающиеся с city_name_
                inputData.keyValueMap.forEach { (key, value) ->
                    when {
                        key.startsWith("city_name_") -> {
                            val index = key.substringAfter("city_name_")
                            val cityName = value.toString()
                            val tempKey = "temperature_$index"
                            val temperature = inputData.getDouble(tempKey, 0.0)

                            cities.add(cityName)
                            totalTemp += temperature
                            count++
                        }
                    }
                }

                // Если данные не найдены, используем тестовые
                if (cities.isEmpty()) {
                    // Это может случиться при первом запуске
                    cities.addAll(listOf("Москва", "Лондон", "Нью-Йорк", "Токио"))
                    totalTemp = 5.2 + 12.8 + 8.5 + 15.3
                    count = 4
                }

                // Имитация формирования отчета
                delay(10000)

                val averageTemp = if (count > 0) totalTemp / count else 0.0
                val resultData = workDataOf(
                    "average_temp" to averageTemp,
                    "cities" to cities.joinToString(", "),
                    "count" to count
                )

                Result.success(resultData)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }
}