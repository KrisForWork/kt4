package com.example.kt4_5

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var seconds = 0
    private var isRunning = false
    private val tag = "TIMER_SERVICE_DEBUG"

    companion object {
        const val ACTION_UPDATE = "com.example.kt4_5.UPDATE"
        const val EXTRA_SECONDS = "extra_seconds"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "=== onCreate() ===")

        NotificationHelper.createNotificationChannel(this)

        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "=== onStartCommand() ===")

        startForegroundService()

        startTimer()

        return START_STICKY
    }

    private fun startForegroundService() {
        Log.d(tag, "--- startForegroundService() ---")

        try {
            val notification = NotificationHelper.createNotification(this, seconds).build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }

            Log.d(tag, "startForeground() выполнен успешно")
        } catch (e: Exception) {
            Log.e(tag, "Ошибка startForeground: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showNotification() {
        if (isRunning) {
            updateNotification()
        }
    }

    private fun startTimer() {
        if (isRunning) return

        isRunning = true
        Log.d(tag, "Таймер запущен")

        serviceScope.launch {
            while (isRunning) {
                delay(1000L)
                seconds++
                Log.d(tag, "Секунд: $seconds")

                withContext(Dispatchers.Main) {
                    updateNotification()
                    sendUpdateToActivity()
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val notification = NotificationHelper.createNotification(this, seconds).build()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
            Log.d(tag, "Уведомление обновлено: $seconds сек")
        } catch (e: Exception) {
            Log.e(tag, "Ошибка обновления уведомления: ${e.message}")
        }
    }

    private fun sendUpdateToActivity() {
        val intent = Intent(ACTION_UPDATE).apply {
            putExtra(EXTRA_SECONDS, seconds)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        Log.d(tag, "Broadcast отправлен: $seconds сек")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "=== onDestroy() ===")
        isRunning = false
        serviceScope.cancel()
    }
}