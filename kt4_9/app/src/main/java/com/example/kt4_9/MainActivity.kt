package com.example.kt4_9

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.kt4_9.ui.theme.Kt4_9Theme
import com.example.kt4_9.workers.WeatherForegroundService

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Разрешение на уведомления необходимо для отслеживания прогресса",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Kt4_9Theme {
                MainScreen()
            }
        }
    }

    fun startWeatherService() {
        // Проверяем разрешение для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Запускаем сервис
        val intent = Intent(this, WeatherForegroundService::class.java).apply {
            action = "START_DOWNLOAD"
        }
        startService(intent)
    }
}