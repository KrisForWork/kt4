package com.example.kt4_11

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReminderScreen(viewModel: ReminderViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (state.isEnabled) Color.Green else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isEnabled) "Включено" else "Выключено",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Title
        Text(
            text = "Напоминание о таблетке",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Next reminder time
        if (state.isEnabled && state.nextReminderTime != null) {
            Card(
                modifier = Modifier.padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "Следующее напоминание: ${state.nextReminderTime}",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }
        }

        // Main button
        Button(
            onClick = {
                if (state.isEnabled) {
                    viewModel.disableReminder()
                } else {
                    viewModel.enableReminder()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isEnabled) Color.Red else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (state.isEnabled) "Выключить напоминание" else "Включить напоминание",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}