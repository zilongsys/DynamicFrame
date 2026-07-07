package com.dynamicframe.di

import com.dynamicframe.data.remote.openmeteo.IpGeolocationApi
import com.dynamicframe.data.remote.openmeteo.OpenMeteoForecastApi
import com.dynamicframe.data.remote.openmeteo.OpenMeteoGeocodingApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOpenMeteoForecastApi(
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): OpenMeteoForecastApi =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenMeteoForecastApi::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoGeocodingApi(
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): OpenMeteoGeocodingApi =
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideIpGeolocationApi(
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): IpGeolocationApi =
        Retrofit.Builder()
            .baseUrl("https://ipwho.is/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(IpGeolocationApi::class.java)
}
