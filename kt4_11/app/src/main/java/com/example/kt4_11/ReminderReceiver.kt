package com.example.kt4_11

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.example.reminder.ACTION_REMINDER") {
            // Show notification
            NotificationHelper.showNotification(context)

            // Schedule next reminder
            if (ReminderManager.isReminderEnabled(context)) {
                ReminderManager.scheduleDailyReminder(context)
            }
        }
    }
}