package com.example.kt4_14

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class CompassSensor {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var listener: SensorEventListener? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val accelData = FloatArray(3)
    private val magnetData = FloatArray(3)
    private var accelSet = false
    private var magnetSet = false

    // Оптимизированный фильтр
    private var filteredAzimuth = 0f
    private val filterAlpha = 0.2f // Увеличили для лучшей реакции
    private var lastUpdateTime = 0L
    private val minTimeBetweenUpdates = 16L // ~60 FPS

    fun checkSensorAvailability(context: Context): Boolean {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        return accelerometer != null && magnetometer != null
    }

    fun startListening(context: Context, onAzimuthChanged: (Float) -> Unit) {
        stopListening()

        if (!checkSensorAvailability(context)) return

        accelSet = false
        magnetSet = false
        filteredAzimuth = 0f

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentTime = System.currentTimeMillis()

                // Ограничиваем частоту обновлений для оптимизации
                if (currentTime - lastUpdateTime < minTimeBetweenUpdates) {
                    return
                }
                lastUpdateTime = currentTime

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelData, 0, 3)
                        accelSet = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetData, 0, 3)
                        magnetSet = true
                    }
                }

                if (accelSet && magnetSet) {
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData)

                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        var azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        azimuthInDegrees = (azimuthInDegrees + 360) % 360

                        // Оптимизированный фильтр
                        if (filteredAzimuth == 0f) {
                            filteredAzimuth = azimuthInDegrees
                        } else {
                            var diff = azimuthInDegrees - filteredAzimuth
                            if (diff > 180) diff -= 360
                            else if (diff < -180) diff += 360

                            filteredAzimuth += diff * filterAlpha

                            if (filteredAzimuth < 0) filteredAzimuth += 360
                            else if (filteredAzimuth >= 360) filteredAzimuth -= 360
                        }

                        onAzimuthChanged(filteredAzimuth)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Можно добавить уведомление о низкой точности
            }
        }

        // Регистрируем слушатели с оптимальной задержкой
        accelerometer?.let {
            sensorManager?.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME // Используем GAME вместо FASTEST
            )
        }

        magnetometer?.let {
            sensorManager?.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stopListening() {
        listener?.let {
            sensorManager?.unregisterListener(it)
        }
        listener = null
        accelSet = false
        magnetSet = false
    }
}