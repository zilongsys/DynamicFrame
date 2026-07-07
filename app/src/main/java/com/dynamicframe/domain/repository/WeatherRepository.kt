package com.dynamicframe.domain.repository

import com.dynamicframe.domain.model.WeatherInfo

interface WeatherRepository {
    /** Clima actual; falla si no hay red, API error o caché expirado sin red. */
    suspend fun getCurrentWeather(): Result<WeatherInfo>
}
