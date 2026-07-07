package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.model.WeatherInfo
import com.dynamicframe.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
) {
    /** Devuelve clima cacheado o recién obtenido; `null` si no hay datos válidos. */
    suspend operator fun invoke(): WeatherInfo? =
        weatherRepository.getCurrentWeather().getOrNull()
}
