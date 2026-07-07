package com.dynamicframe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = WeatherPreferencesKeys.DATASTORE_FILE
)
