package com.dynamicframe.data.remote.openmeteo

import com.google.gson.annotations.SerializedName

data class OpenMeteoForecastResponse(
    @SerializedName("current") val current: OpenMeteoCurrent? = null,
)

data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperatureFahrenheit: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
)

data class OpenMeteoGeocodingResponse(
    @SerializedName("results") val results: List<OpenMeteoGeocodingResult>? = null,
)

data class OpenMeteoGeocodingResult(
    @SerializedName("name") val name: String? = null,
    @SerializedName("admin1") val admin1: String? = null,
)

data class IpWhoResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("city") val city: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
)

data class WeatherCachePayload(
    val temperatureFahrenheit: Int,
    val conditionLabel: String,
    val cityName: String,
    val conditionEmoji: String,
)
