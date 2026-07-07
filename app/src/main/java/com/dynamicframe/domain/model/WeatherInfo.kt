package com.dynamicframe.domain.model

/** Condiciones actuales para el widget Paradise (capa 4). */
data class WeatherInfo(
    val temperatureFahrenheit: Int,
    val conditionLabel: String,
    val cityName: String,
    val conditionEmoji: String,
)
