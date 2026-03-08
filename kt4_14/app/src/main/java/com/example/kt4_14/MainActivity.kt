package com.example.kt4_14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.kt4_14.ui.theme.Kt4_14Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Kt4_14Theme {
                CompassApp()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CompassApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val compassSensor = remember { CompassSensor() }
    var azimuth by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    // Используем производные состояния для оптимизации рекомпозиции
    val displayAzimuth = remember(azimuth) {
        ((azimuth + 360) % 360).roundToInt()
    }

    // Проверка наличия сенсоров при запуске (выполняем в фоне)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            hasSensor = compassSensor.checkSensorAvailability(context)
        }
    }

    // Управление жизненным циклом
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (hasSensor) {
                        compassSensor.startListening(context) { angle ->
                            azimuth = angle
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    compassSensor.stopListening()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            compassSensor.stopListening()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasSensor) {
                // Используем ключ для оптимизации рекомпозиции
                key(displayAzimuth) {
                    CompassView(azimuth = azimuth)
                }
            } else {
                Text(
                    text = "Устройство не поддерживает датчик ориентации",
                    color = Color.Red,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun CompassView(azimuth: Float) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val compassSize = screenWidth * 0.7f

    // Оптимизированная анимация с использованием remember
    val rotation by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "compass_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Компас",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(compassSize)
                .padding(16.dp)
        ) {
            // Внешний круг компаса
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Оптимизация рендеринга
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    }
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 2 - 4f

                // Рисуем круг
                drawCircle(
                    color = Color.DarkGray,
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // Рисуем метки сторон света (оптимизировано)
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val sin = Math.sin(angle).toFloat()
                    val cos = Math.cos(angle).toFloat()

                    val startX = centerX + (radius - 10f) * sin
                    val startY = centerY - (radius - 10f) * cos
                    val endX = centerX + radius * sin
                    val endY = centerY - radius * cos

                    drawLine(
                        color = Color.Gray,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 2 == 0) 2f else 1f
                    )
                }
            }

            // Буквы сторон света (вынесены отдельно для оптимизации)
            CompassLabels()

            // Стрелка компаса с оптимизированным поворотом
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .graphicsLayer {
                        rotationZ = rotation
                        // Оптимизация рендеринга
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val centerY = height / 2

                // Толщина стрелки
                val arrowWidth = width * 0.06f
                val arrowLength = height * 0.9f
                val halfLength = arrowLength / 2

                // Северная часть (красная)
                drawLine(
                    color = Color.Red,
                    start = Offset(centerX, centerY - halfLength),
                    end = Offset(centerX, centerY),
                    strokeWidth = arrowWidth
                )

                // Южная часть (серая)
                drawLine(
                    color = Color.Gray,
                    start = Offset(centerX, centerY + halfLength),
                    end = Offset(centerX, centerY),
                    strokeWidth = arrowWidth
                )

                // Центральный круг
                drawCircle(
                    color = Color.White,
                    radius = arrowWidth * 1.2f,
                    center = Offset(centerX, centerY)
                )
            }
        }

        Text(
            text = "Азимут: ${((azimuth + 360) % 360).roundToInt()}°",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 32.dp)
        )

        Text(
            text = "Поверните устройство в виде восьмерки для калибровки",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun CompassLabels() {
    // Выносим метки в отдельную композицию для оптимизации
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "N",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
        )
        Text(
            text = "E",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        Text(
            text = "S",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "W",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}