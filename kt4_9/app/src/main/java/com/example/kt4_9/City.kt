package com.example.kt4_9

data class City(
    val name: String,
    var temperature: Double? = null
)

data class WeatherResult(
    val cityName: String,
    val temperature: Double
)

data class ReportResult(
    val averageTemperature: Double,
    val cities: List<String>
)