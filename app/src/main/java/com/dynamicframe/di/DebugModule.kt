package com.dynamicframe.di

import com.dynamicframe.data.debug.AndroidAppDebugLogger
import com.dynamicframe.domain.repository.AppDebugLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugModule {

    @Binds
    @Singleton
    abstract fun bindAppDebugLogger(impl: AndroidAppDebugLogger): AppDebugLogger
}
