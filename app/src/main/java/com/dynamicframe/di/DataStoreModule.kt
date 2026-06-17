package com.dynamicframe.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dynamicframe.data.local.debugPreferencesDataStore
import com.dynamicframe.data.local.slideshowSettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SlideshowSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DebugSettingsDataStore

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SlideshowSettingsDataStore
    fun provideSlideshowSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.slideshowSettingsDataStore

    @Provides
    @Singleton
    @DebugSettingsDataStore
    fun provideDebugSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.debugPreferencesDataStore
}
