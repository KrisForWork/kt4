package com.example.kt4_7

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class RandomNumberService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentNumber = 0
    private var isGenerating = false

    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    override fun onCreate() {
        super.onCreate()
        startGenerating()
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun startGenerating() {
        isGenerating = true
        scope.launch {
            while (isGenerating) {
                currentNumber = (0..100).random()
                delay(1000L) // Каждую секунду
            }
        }
    }

    fun getCurrentNumber(): Int = currentNumber

    override fun onUnbind(intent: Intent?): Boolean {
        stopGenerating()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGenerating()
        scope.cancel()
    }

    private fun stopGenerating() {
        isGenerating = false
    }
}