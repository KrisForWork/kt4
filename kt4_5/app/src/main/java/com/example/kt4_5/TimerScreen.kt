package com.example.kt4_5

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerScreen(
    seconds: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    isServiceRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = seconds.toString(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onStartClick,
            enabled = !isServiceRunning,
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Старт")
        }

        Button(
            onClick = onStopClick,
            enabled = isServiceRunning,
            modifier = Modifier.width(200.dp)
        ) {
            Text("Стоп")
        }
    }
}