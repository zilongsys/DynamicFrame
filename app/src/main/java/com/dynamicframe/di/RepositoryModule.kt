package com.dynamicframe.di

import com.dynamicframe.data.local.DataStoreSettingsRepository
import com.dynamicframe.data.local.LocalMediaRepository
import com.dynamicframe.data.local.LocalMusicRepository
import com.dynamicframe.domain.repository.MediaRepository
import com.dynamicframe.domain.repository.MusicRepository
import com.dynamicframe.domain.repository.SettingsRepository
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
}
