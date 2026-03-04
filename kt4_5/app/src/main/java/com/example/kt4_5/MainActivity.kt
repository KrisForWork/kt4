package com.example.kt4_5

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.kt4_5.TimerScreen

class MainActivity : ComponentActivity() {

    private val tag = "MAIN_ACTIVITY_DEBUG"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(tag, "Разрешение на уведомления получено")
        } else {
            Log.d(tag, "Разрешение на уведомления не получено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "=== onCreate() Activity ===")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            TimerApp()
        }
    }
}

@Composable
fun TimerApp() {
    val context = LocalContext.current
    val tag = "COMPOSE_DEBUG"

    var seconds by remember { mutableStateOf(0) }
    var isServiceRunning by remember { mutableStateOf(false) }

    Log.d(tag, "Compose перерисовка, seconds = $seconds")

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TimerService.ACTION_UPDATE) {
                    seconds = intent.getIntExtra(TimerService.EXTRA_SECONDS, 0)
                    Log.d(tag, "Получено обновление: $seconds")
                }
            }
        }
    }

    DisposableEffect(context) {
        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.registerReceiver(receiver, IntentFilter(TimerService.ACTION_UPDATE))

        onDispose {
            localBroadcastManager.unregisterReceiver(receiver)
        }
    }

    TimerScreen(
        seconds = seconds,
        isServiceRunning = isServiceRunning,
        onStartClick = {
            Log.d(tag, "Старт")
            val intent = Intent(context, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isServiceRunning = true
        },
        onStopClick = {
            Log.d(tag, "Стоп")
            val intent = Intent(context, TimerService::class.java)
            context.stopService(intent)
            seconds = 0
            isServiceRunning = false
        }
    )
}