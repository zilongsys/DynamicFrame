package com.dynamicframe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val DEBUG_DATASTORE_FILE = "app_debug"

/** Única definición del DataStore del modo depuración. */
internal val Context.debugPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DEBUG_DATASTORE_FILE
)
