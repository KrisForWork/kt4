package com.example.kt4_9

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kt4_9.workers.WeatherForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class CityWeather(
    val name: String,
    val status: CityStatus = CityStatus.WAITING,
    val temperature: Double? = null,
    val condition: String = ""
)

enum class CityStatus {
    WAITING,    // Ожидание
    LOADING,    // Загружается
    COMPLETED   // Готово
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val state by viewModel.state.collectAsState()

    // Состояние городов
    var cities by remember {
        mutableStateOf(
            listOf(
                CityWeather("Москва"),
                CityWeather("Лондон"),
                CityWeather("Нью-Йорк"),
                CityWeather("Токио")
            )
        )
    }

    // Слушаем изменения в состоянии загрузки
    LaunchedEffect(state.isLoading, state.result) {
        if (!state.isLoading && state.result.isNotEmpty()) {
            // Загрузка завершена успешно - обновляем города с реальными температурами
            val avgTemp = state.result.substringAfter("Средняя температура: ").substringBefore("°C").toDoubleOrNull() ?: 0.0

            // Создаем разные температуры для каждого города на основе средней
            cities = cities.mapIndexed { index, city ->
                city.copy(
                    status = CityStatus.COMPLETED,
                    temperature = avgTemp + (index - 1.5) * 5, // Разброс температур
                    condition = if (index % 2 == 0) "ясно" else "дождь"
                )
            }
        } else if (!state.isLoading && state.result.isEmpty()) {
            // Сброс - все в ожидании
            cities = cities.map { it.copy(status = CityStatus.WAITING, temperature = null) }
        }
    }

    LaunchedEffect(state.progress) {
        if (state.isLoading) {
            val completedCount = (state.progress * cities.size).toInt()
            cities = cities.mapIndexed { index, city ->
                if (index < completedCount) {
                    city.copy(status = CityStatus.COMPLETED)
                } else {
                    city.copy(status = CityStatus.LOADING)
                }
            }
            // Небольшая задержка для визуализации
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Прогноз погоды", fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Статус загрузки
            if (state.isLoading) {
                val inProgress = cities.count { it.status == CityStatus.LOADING }
                val completedCount = cities.count { it.status == CityStatus.COMPLETED }
                Text(
                    text = "Загрузка... ($inProgress в процессе, $completedCount готово)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (state.result.isNotEmpty()) {
                Text(
                    text = "Все данные получены!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Text(
                    text = "Готов начать",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Список городов
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cities) { city ->
                    CityCard(city)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Итоговый отчет
            if (state.result.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Итоговый прогноз:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        cities.forEach { city ->
                            if (city.temperature != null) {
                                Text(
                                    text = "${city.name}: ${city.temperature.toInt()}°C, ${city.condition}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))

                        Spacer(modifier = Modifier.height(8.dp))

                        val avgTemp = cities
                            .mapNotNull { it.temperature }
                            .average()
                            .toInt()

                        Text(
                            text = "Средняя температура: $avgTemp°C",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.isLoading) {
                    OutlinedButton(
                        onClick = {
                            // Отмена загрузки
                            context.stopService(Intent(context, WeatherForegroundService::class.java))
                            viewModel.cancelLoading()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Отменить")
                    }
                } else {
                    Button(
                        onClick = {
                            // Сбрасываем города перед новой загрузкой
                            cities = cities.map { it.copy(status = CityStatus.WAITING, temperature = null) }
                            activity?.startWeatherService()
                            viewModel.startLoading()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        Text(if (state.result.isNotEmpty()) "Собрать заново" else "Собрать прогноз")
                    }
                }
            }
        }
    }
}

@Composable
fun CityCard(city: CityWeather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (city.status) {
                CityStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                CityStatus.LOADING -> MaterialTheme.colorScheme.tertiaryContainer
                CityStatus.WAITING -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = city.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (city.status) {
                        CityStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                        CityStatus.LOADING -> MaterialTheme.colorScheme.onTertiaryContainer
                        CityStatus.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = when (city.status) {
                        CityStatus.COMPLETED -> "Готово"
                        CityStatus.LOADING -> "Загружается..."
                        CityStatus.WAITING -> "Ожидание"
                    },
                    fontSize = 14.sp,
                    color = when (city.status) {
                        CityStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        CityStatus.LOADING -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        CityStatus.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }

            if (city.status == CityStatus.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (city.status == CityStatus.COMPLETED && city.temperature != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Готово",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${city.temperature.toInt()}°C",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}