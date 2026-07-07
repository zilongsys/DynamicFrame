package com.dynamicframe.di

import com.dynamicframe.data.local.AndroidMediaPaletteRepository
import com.dynamicframe.data.local.CoilImageCacheRepository
import com.dynamicframe.data.local.DataStoreSettingsRepository
import com.dynamicframe.data.local.LocalMediaRepository
import com.dynamicframe.data.local.LocalMusicRepository
import com.dynamicframe.data.local.LocalStorageBrowserRepository
import com.dynamicframe.data.local.MediaMetadataVideoThumbnailRepository
import com.dynamicframe.data.local.OpenMeteoWeatherRepository
import com.dynamicframe.domain.repository.StorageBrowserRepository
import com.dynamicframe.domain.repository.ImageCacheRepository
import com.dynamicframe.domain.repository.MediaPaletteRepository
import com.dynamicframe.domain.repository.MediaRepository
import com.dynamicframe.domain.repository.MusicRepository
import com.dynamicframe.domain.repository.SettingsRepository
import com.dynamicframe.domain.repository.VideoThumbnailRepository
import com.dynamicframe.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: LocalMediaRepository): MediaRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: LocalMusicRepository): MusicRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindImageCacheRepository(impl: CoilImageCacheRepository): ImageCacheRepository

    @Binds
    @Singleton
    abstract fun bindStorageBrowserRepository(impl: LocalStorageBrowserRepository): StorageBrowserRepository

    @Binds
    @Singleton
    abstract fun bindVideoThumbnailRepository(
        impl: MediaMetadataVideoThumbnailRepository
    ): VideoThumbnailRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindMediaPaletteRepository(impl: AndroidMediaPaletteRepository): MediaPaletteRepository
}
