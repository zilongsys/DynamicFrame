package com.dynamicframe.data.local

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WeatherPreferencesKeys {
    const val DATASTORE_FILE = "weather_cache"
    val CACHED_PAYLOAD = stringPreferencesKey("weather_cached_payload")
    val CACHED_AT_MS = longPreferencesKey("weather_cached_at_ms")
}
