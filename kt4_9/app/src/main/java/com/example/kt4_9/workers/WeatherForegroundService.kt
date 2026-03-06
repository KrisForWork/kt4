package com.example.kt4_9.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.kt4_9.App
import com.example.kt4_9.MainActivity
import com.example.kt4_9.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private const val TAG = "WeatherForegroundService"

class WeatherForegroundService : LifecycleService() {

    private lateinit var workManager: WorkManager
    private val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")
    private val completedCities = mutableListOf<String>()
    private val df = DecimalFormat("#.#")
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        workManager = WorkManager.getInstance(this)

        createNotificationChannel()

        try {
            val notification = createNotification("Начинаем загрузку...", 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    App.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(App.NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Service started in foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                App.CHANNEL_ID,
                "Weather Report",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows weather download progress"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "START_DOWNLOAD" -> {
                if (!isRunning) {
                    Log.d(TAG, "Starting weather download")
                    isRunning = true
                    startWeatherDownload()
                }
            }
            "STOP_SERVICE" -> {
                Log.d(TAG, "Stopping service")
                isRunning = false
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startWeatherDownload() {
        lifecycleScope.launch {
            try {
                // Создаем параллельные воркеры для городов
                val weatherWorkers = cities.mapIndexed { index, city ->
                    OneTimeWorkRequestBuilder<WeatherWorker>()
                        .setInputData(workDataOf("city_name" to city))
                        .addTag("weather_$index")
                        .build()
                }

                // Создаем финальный воркер для отчета
                val reportWorker = OneTimeWorkRequestBuilder<ReportWorker>()
                    .addTag("report")
                    .build()

                // Формируем цепочку
                workManager
                    .beginWith(weatherWorkers)
                    .then(reportWorker)
                    .enqueue()

                Log.d(TAG, "Workers enqueued, starting progress tracking")

                // Отслеживаем прогресс
                trackProgress(weatherWorkers, reportWorker)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting download", e)
                stopSelf()
            }
        }
    }

    private suspend fun trackProgress(
        weatherWorkers: List<WorkRequest>,
        reportWorker: WorkRequest
    ) {
        val totalCities = weatherWorkers.size
        var isReportStarted = false

        while (isRunning) {
            try {
                // Получаем статусы всех воркеров
                val weatherStates = weatherWorkers.map { request ->
                    workManager.getWorkInfoById(request.id).get()
                }

                val reportState = workManager.getWorkInfoById(reportWorker.id).get()

                // Проверяем завершение
                if (reportState?.state == WorkInfo.State.SUCCEEDED) {
                    val output = reportState.outputData
                    val avgTemp = output.getDouble("average_temp", 0.0)
                    val citiesList = output.getString("cities") ?: ""

                    Log.d(TAG, "Report SUCCEEDED - avgTemp=$avgTemp, cities='$citiesList'")
                    Log.d(TAG, "All output keys: ${output.keyValueMap.keys}")

                    updateNotification(
                        "Отчёт готов! Средняя температура ${df.format(avgTemp)}°C",
                        totalCities,
                        completedCities
                    )
                    Log.d(TAG, "Report ready, stopping service in 3 seconds")
                    delay(3000)
                    isRunning = false
                    stopSelf()
                    break
                }

                if (reportState?.state == WorkInfo.State.FAILED) {
                    Log.e(TAG, "Report worker failed")
                    updateNotification("Ошибка при формировании отчёта", totalCities, completedCities)
                    delay(3000)
                    isRunning = false
                    stopSelf()
                    break
                }

                // Обновляем список завершенных городов
                completedCities.clear()
                weatherWorkers.forEachIndexed { index, request ->
                    val state = workManager.getWorkInfoById(request.id).get()
                    if (state?.state == WorkInfo.State.SUCCEEDED) {
                        val cityName = state.outputData.getString("city_name") ?: cities[index]
                        if (!completedCities.contains(cityName)) {
                            completedCities.add(cityName)
                        }
                    }
                }

                // Обновляем уведомление
                when {
                    reportState?.state == WorkInfo.State.RUNNING && !isReportStarted -> {
                        isReportStarted = true
                        updateNotification(
                            "Все данные получены, формируем отчёт...",
                            totalCities,
                            completedCities
                        )
                    }
                    completedCities.size < totalCities -> {
                        val message = when (completedCities.size) {
                            0 -> "Загружаем погоду для $totalCities городов..."
                            else -> "Готово: ${completedCities.joinToString(", ")}, " +
                                    "осталось ${totalCities - completedCities.size}"
                        }
                        updateNotification(message, totalCities, completedCities)
                    }
                }

                delay(1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking progress", e)
                delay(1000)
            }
        }
    }

    private fun updateNotification(message: String, total: Int, completed: List<String>) {
        try {
            val progress = if (total > 0) (completed.size * 100) / total else 0
            val notification = createNotification(message, progress)

            // ИСПРАВЛЕНИЕ: Проверяем разрешение перед показом уведомления
            if (hasNotificationPermission()) {
                val manager = NotificationManagerCompat.from(this)
                manager.notify(App.NOTIFICATION_ID, notification)
                Log.d(TAG, "Notification updated: $message ($progress%)")
            } else {
                Log.w(TAG, "No notification permission, skipping update")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when showing notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    // ИСПРАВЛЕНИЕ: Метод проверки разрешения
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // До Android 13 разрешение не требуется
        }
    }

    private fun createNotification(message: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WeatherForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }

        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle("Прогноз погоды")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отмена", stopPendingIntent)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}