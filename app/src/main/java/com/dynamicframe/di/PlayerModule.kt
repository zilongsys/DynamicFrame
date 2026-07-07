package com.dynamicframe.di

import com.dynamicframe.data.player.ExoSlideshowVideoPlayerRepository
import com.dynamicframe.data.player.ExoVideoBackdropPlayerRepository
import com.dynamicframe.data.player.MusicPlayerController
import com.dynamicframe.domain.repository.MusicPlaybackRepository
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindMusicPlaybackRepository(impl: MusicPlayerController): MusicPlaybackRepository

    @Binds
    @Singleton
    abstract fun bindSlideshowVideoPlayerRepository(
        impl: ExoSlideshowVideoPlayerRepository
    ): SlideshowVideoPlayerRepository

    @Binds
    @Singleton
    abstract fun bindVideoBackdropPlayerRepository(
        impl: ExoVideoBackdropPlayerRepository
    ): VideoBackdropPlayerRepository
}
