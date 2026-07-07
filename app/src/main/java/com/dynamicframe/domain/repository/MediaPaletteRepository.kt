package com.dynamicframe.domain.repository

import com.dynamicframe.domain.model.MediaDynamicPalette

interface MediaPaletteRepository {
    fun getCached(imageUri: String): MediaDynamicPalette?

    suspend fun extractFromImageUri(imageUri: String): Result<MediaDynamicPalette>

    /** Decodifica y cachea la paleta si aún no existe (p. ej. slide siguiente). */
    suspend fun preload(imageUri: String)
}
