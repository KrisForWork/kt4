package com.example.kt4_8

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(viewModel: PhotoProcessorViewModel) {
    val photoState by viewModel.photoState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Статус
        Text(
            text = getStatusText(photoState),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Прогресс-бар
        if (isProcessing) {
            LinearProgressIndicator(
                progress = { photoState.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "${photoState.progress}%",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка - ИСПРАВЛЕНО УСЛОВИЕ
        Button(
            onClick = {
                if (isProcessing) {
                    viewModel.cancelProcessing()
                } else {
                    viewModel.startProcessing()
                }
            },
            enabled = !isProcessing || photoState.step > 0, // ИСПРАВЛЕНО: используем > 0 вместо compareTo
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isProcessing) "Отмена" else "Начать обработку")
        }

        // Результат или ошибка
        if (photoState.error.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Ошибка: ${photoState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else if (photoState.step == 4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✓ Готово! Фото загружено",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Путь: ${photoState.uploadedUrl}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun getStatusText(state: PhotoData): String {
    return when (state.step) {
        1 -> "Сжимаем фото... ${state.progress}%"
        2 -> "Добавляем водяной знак... ${state.progress}%"
        3 -> "Загружаем... ${state.progress}%"
        4 -> "Готово!"
        -1 -> "Ошибка"
        else -> "Нажмите кнопку для начала"
    }
}