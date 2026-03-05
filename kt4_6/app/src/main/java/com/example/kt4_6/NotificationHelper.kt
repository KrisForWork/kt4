package com.example.kt4_6

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

object NotificationHelper {
    private const val CHANNEL_ID = "timer_channel"
    private const val CHANNEL_NAME = "Timer Service"
    private const val NOTIFICATION_ID = 1
    private const val FINISH_NOTIFICATION_ID = 2
    private const val TAG = "NotificationHelper"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)

            if (existingChannel == null) {
                Log.d(TAG, "Creating new notification channel")
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Timer service notifications"
                    enableVibration(true)
                    enableLights(true)

                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(soundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())

                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                    setShowBadge(true)
                    setAllowBubbles(true)
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Channel created")
            }
        }
    }

    fun createTimerRunningNotification(context: Context, seconds: Int): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("⏱️ Timer is running")
            .setContentText("Time remaining: $seconds seconds")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }

    fun createTimerFinishedNotification(context: Context) {
        if (hasNotificationPermission(context)) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("✅ Timer completed!")
                    .setContentText("Time is up!")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    .setOngoing(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(createDeleteIntent(context))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setFullScreenIntent(null, true)
                }

                val notification = builder.build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(FINISH_NOTIFICATION_ID, notification)

                Log.d(TAG, "Finished notification shown with ID: $FINISH_NOTIFICATION_ID")

            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to show notification: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
            }
        } else {
            Log.d(TAG, "Notification permission not granted")
        }
    }

    private fun createDeleteIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationDismissedReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun verifyNotificationChannel(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)

            if (channel == null) {
                Log.e(TAG, "Channel is null! Creating...")
                createNotificationChannel(context)
                return false
            }

            Log.d(TAG, "Channel verification:")
            Log.d(TAG, "  - Importance: ${channel.importance}")
            Log.d(TAG, "  - Sound: ${channel.sound}")
            Log.d(TAG, "  - Vibration: ${channel.shouldVibrate()}")
            Log.d(TAG, "  - Lockscreen: ${channel.lockscreenVisibility}")
            Log.d(TAG, "  - Bubbles: ${channel.canBubble()}")

            return channel.importance >= NotificationManager.IMPORTANCE_HIGH
        }
        return true
    }

    fun logNotificationChannelInfo(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "Channel exists: ${channel != null}")
            if (channel != null) {
                Log.d(TAG, "Channel importance: ${channel.importance}")
                Log.d(TAG, "Channel can show badge: ${channel.canShowBadge()}")
                Log.d(TAG, "Channel sound: ${channel.sound}")
                Log.d(TAG, "Channel vibration: ${channel.shouldVibrate()}")
                Log.d(TAG, "Channel lockscreen visibility: ${channel.lockscreenVisibility}")
            }

            val channels = notificationManager.notificationChannels
            Log.d(TAG, "All notification channels: ${channels.size}")
            channels.forEach {
                Log.d(TAG, "Channel ID: ${it.id}, Importance: ${it.importance}")
            }
        }
    }

    fun cancelFinishedNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(FINISH_NOTIFICATION_ID)
        Log.d(TAG, "Finished notification cancelled")
    }
}