package com.dynamicframe.domain.repository

interface ImageCacheRepository {
    fun evict(uri: String)
    fun preload(uri: String, width: Int, height: Int)

    /** Precarga y espera a que Coil tenga la imagen lista (p. ej. fondo dinámico). */
    suspend fun preloadAndAwait(uri: String, width: Int, height: Int)

    suspend fun preloadBlurAndAwait(
        uri: String,
        width: Int,
        height: Int,
        blurRadius: Float = 25f,
        blurSampling: Float = 5f,
    )
}
