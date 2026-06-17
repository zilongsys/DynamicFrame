package com.dynamicframe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Única definición del DataStore de ajustes del slideshow (singleton por proceso). */
internal val Context.slideshowSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SlideshowPreferencesKeys.DATASTORE_FILE
)
