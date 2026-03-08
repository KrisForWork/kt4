package com.example.kt4_11

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ReminderState(
    val isEnabled: Boolean = false,
    val nextReminderTime: String? = null
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ReminderState())
    val state: StateFlow<ReminderState> = _state

    private val prefs = application.getSharedPreferences("reminder_prefs", Application.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    init {
        loadState()
    }

    private fun loadState() {
        val isEnabled = prefs.getBoolean("reminder_enabled", false)
        val nextTime = if (isEnabled) {
            prefs.getLong("next_reminder_time", 0).takeIf { it > 0 }?.let { timestamp ->
                dateFormat.format(Date(timestamp))
            }
        } else null

        _state.value = ReminderState(isEnabled, nextTime)
    }

    fun enableReminder() {
        viewModelScope.launch {
            val nextTime = ReminderManager.scheduleDailyReminder(getApplication())
            prefs.edit()
                .putBoolean("reminder_enabled", true)
                .putLong("next_reminder_time", nextTime)
                .apply()

            _state.update {
                it.copy(
                    isEnabled = true,
                    nextReminderTime = dateFormat.format(Date(nextTime))
                )
            }
        }
    }

    fun disableReminder() {
        viewModelScope.launch {
            ReminderManager.cancelReminder(getApplication())
            prefs.edit()
                .putBoolean("reminder_enabled", false)
                .remove("next_reminder_time")
                .apply()

            _state.update {
                it.copy(isEnabled = false, nextReminderTime = null)
            }
        }
    }
}