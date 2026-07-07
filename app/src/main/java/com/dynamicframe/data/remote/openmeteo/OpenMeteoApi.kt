package com.dynamicframe.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
    ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/reverse")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("language") language: String,
        @Query("count") count: Int = 1,
    ): OpenMeteoGeocodingResponse
}

interface IpGeolocationApi {
    @GET("/")
    suspend fun locate(): IpWhoResponse
}
