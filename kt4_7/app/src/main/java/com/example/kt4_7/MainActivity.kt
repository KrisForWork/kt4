package com.example.kt4_7

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kt4_7.ui.theme.Kt4_7Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Kt4_7Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isBound by remember { mutableStateOf(false) }
    var currentNumber by remember { mutableStateOf(0) }
    var randomNumberService by remember { mutableStateOf<RandomNumberService?>(null) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as RandomNumberService.LocalBinder
                randomNumberService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isBound = false
                randomNumberService = null
            }
        }
    }

    // Функции для подключения/отключения
    fun bindService() {
        if (!isBound) {
            Intent(context, RandomNumberService::class.java).also { intent ->
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    // Обновление числа каждую секунду, когда сервис подключен
    LaunchedEffect(isBound) {
        while (isActive && isBound) {
            currentNumber = randomNumberService?.getCurrentNumber() ?: 0
            delay(1000L)
        }
    }

    // Отключаемся при выходе
    DisposableEffect(Unit) {
        onDispose {
            if (isBound) {
                context.unbindService(connection)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Случайное число",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isBound) "$currentNumber" else "---",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = if (isBound)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { bindService() },
                enabled = !isBound
            ) {
                Text("Подключиться")
            }

            Button(
                onClick = { unbindService() },
                enabled = isBound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Отключиться")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isBound)
                "Сервис подключен"
            else
                "Сервис отключен",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}