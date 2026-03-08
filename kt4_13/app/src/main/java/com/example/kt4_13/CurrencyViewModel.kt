package com.example.kt4_13

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class CurrencyViewModel : ViewModel() {

    private val _usdRate = MutableStateFlow(90.0)
    val usdRate: StateFlow<Double> = _usdRate.asStateFlow()

    private val _previousRate = MutableStateFlow(90.0)
    val previousRate: StateFlow<Double> = _previousRate.asStateFlow()

    init {
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Обновление каждые 5 секунд
                generateNewRate()
            }
        }
    }

    fun refreshRate() {
        viewModelScope.launch {
            generateNewRate()
        }
    }

    private fun generateNewRate() {
        val currentRate = _usdRate.value
        val change = Random.nextDouble(-2.0, 2.0)
        val newRate = (currentRate + change).coerceIn(70.0, 120.0)

        _previousRate.update { currentRate }
        _usdRate.update { newRate }
    }
}