package com.example.kt4_6

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = 0
    private var isForegroundService = false

    companion object {
        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val TAG = "TimerService"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        NotificationHelper.createNotificationChannel(this)
        val channelValid = NotificationHelper.verifyNotificationChannel(this)
        Log.d(TAG, "Channel valid: $channelValid")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                Log.d(TAG, "Starting timer with $seconds seconds")
                if (seconds > 0) {
                    startTimer(seconds)
                }
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stopping service by request")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        remainingSeconds = seconds

        try {
            val notification = NotificationHelper.createTimerRunningNotification(this, remainingSeconds)
            startForeground(NOTIFICATION_ID, notification)
            isForegroundService = true
            Log.d(TAG, "startForeground() called successfully")

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification also sent via NotificationManager")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
            stopSelf()
            return
        }

        countDownTimer = object : CountDownTimer((seconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()

                try {
                    if (hasNotificationPermission()) {
                        val notification = NotificationHelper.createTimerRunningNotification(this@TimerService, remainingSeconds)
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                        Log.d(TAG, "Notification updated: $remainingSeconds")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update notification: ${e.message}")
                }
            }

            override fun onFinish() {
                Log.d(TAG, "Timer finished")
                serviceScope.launch {
                    withContext(Dispatchers.Main) {
                        if (hasNotificationPermission()) {
                            NotificationHelper.createTimerFinishedNotification(this@TimerService)
                            Log.d(TAG, "Finished notification sent")
                        }
                        stopSelf()
                    }
                }
            }
        }.start()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        countDownTimer?.cancel()
        serviceScope.cancel()

        if (isForegroundService) {
            stopForeground(true)
            isForegroundService = false
        }
    }
}