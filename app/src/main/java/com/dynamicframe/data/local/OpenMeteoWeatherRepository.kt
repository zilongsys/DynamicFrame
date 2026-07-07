package com.dynamicframe.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.dynamicframe.data.remote.openmeteo.IpGeolocationApi
import com.dynamicframe.data.remote.openmeteo.OpenMeteoForecastApi
import com.dynamicframe.data.remote.openmeteo.OpenMeteoGeocodingApi
import com.dynamicframe.data.remote.openmeteo.WeatherCachePayload
import com.dynamicframe.data.remote.openmeteo.weatherConditionForCode
import com.dynamicframe.di.WeatherCacheDataStore
import com.dynamicframe.domain.model.WeatherInfo
import com.dynamicframe.domain.repository.WeatherRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class OpenMeteoWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @WeatherCacheDataStore private val dataStore: DataStore<Preferences>,
    private val forecastApi: OpenMeteoForecastApi,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val ipGeolocationApi: IpGeolocationApi,
    private val gson: Gson,
) : WeatherRepository {

    override suspend fun getCurrentWeather(): Result<WeatherInfo> {
        val cached = readCache()
        if (cached != null && !isCacheExpired(cached.cachedAtMillis)) {
            return Result.success(cached.toWeatherInfo())
        }
        if (!isNetworkAvailable()) {
            return Result.failure(NoNetworkException())
        }
        return runCatching {
            val location = resolveLocation()
            val forecast = forecastApi.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
            )
            val current = forecast.current
                ?: throw WeatherFetchException("Missing current weather")
            val temperature = current.temperatureFahrenheit
                ?: throw WeatherFetchException("Missing temperature")
            val weatherCode = current.weatherCode
                ?: throw WeatherFetchException("Missing weather code")
            val condition = weatherConditionForCode(weatherCode)
            val cityName = resolveCityName(
                latitude = location.latitude,
                longitude = location.longitude,
                fallbackCity = location.city,
            )
            val info = WeatherInfo(
                temperatureFahrenheit = temperature.toInt(),
                conditionLabel = condition.label,
                cityName = cityName,
                conditionEmoji = condition.emoji,
            )
            writeCache(info)
            info
        }
    }

    private suspend fun resolveLocation(): ResolvedLocation {
        val ip = ipGeolocationApi.locate()
        if (!ip.success) throw WeatherFetchException("IP geolocation failed")
        val lat = ip.latitude ?: throw WeatherFetchException("Missing latitude")
        val lon = ip.longitude ?: throw WeatherFetchException("Missing longitude")
        return ResolvedLocation(latitude = lat, longitude = lon, city = ip.city)
    }

    private suspend fun resolveCityName(
        latitude: Double,
        longitude: Double,
        fallbackCity: String?,
    ): String {
        val language = Locale.getDefault().language.ifBlank { "en" }
        val response = geocodingApi.reverseGeocode(
            latitude = latitude,
            longitude = longitude,
            language = language,
        )
        val result = response.results?.firstOrNull()
        val fromGeocode = result?.name?.takeIf { it.isNotBlank() }
            ?: result?.admin1?.takeIf { it.isNotBlank() }
        return fromGeocode ?: fallbackCity?.takeIf { it.isNotBlank() }
            ?: throw WeatherFetchException("Missing city name")
    }

    private suspend fun readCache(): CachedWeather? {
        val prefs = dataStore.data.first()
        val json = prefs[WeatherPreferencesKeys.CACHED_PAYLOAD] ?: return null
        val cachedAt = prefs[WeatherPreferencesKeys.CACHED_AT_MS] ?: return null
        return runCatching {
            val payload = gson.fromJson(json, WeatherCachePayload::class.java)
            CachedWeather(payload = payload, cachedAtMillis = cachedAt)
        }.getOrNull()
    }

    private suspend fun writeCache(info: WeatherInfo) {
        val now = System.currentTimeMillis()
        val payload = WeatherCachePayload(
            temperatureFahrenheit = info.temperatureFahrenheit,
            conditionLabel = info.conditionLabel,
            cityName = info.cityName,
            conditionEmoji = info.conditionEmoji,
        )
        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.CACHED_PAYLOAD] = gson.toJson(payload)
            prefs[WeatherPreferencesKeys.CACHED_AT_MS] = now
        }
    }

    private fun isCacheExpired(cachedAtMillis: Long): Boolean =
        System.currentTimeMillis() - cachedAtMillis > CACHE_TTL_MS

    private fun isNetworkAvailable(): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private data class ResolvedLocation(
        val latitude: Double,
        val longitude: Double,
        val city: String?,
    )

    private data class CachedWeather(
        val payload: WeatherCachePayload,
        val cachedAtMillis: Long,
    ) {
        fun toWeatherInfo(): WeatherInfo = WeatherInfo(
            temperatureFahrenheit = payload.temperatureFahrenheit,
            conditionLabel = payload.conditionLabel,
            cityName = payload.cityName,
            conditionEmoji = payload.conditionEmoji,
        )
    }

    private class WeatherFetchException(message: String) : Exception(message)
    private class NoNetworkException : Exception("No network")

    companion object {
        private const val CACHE_TTL_MS = 30L * 60L * 1_000L
    }
}
