package com.dynamicframe.domain.repository

/** Extrae un fotograma de vídeo para fondo blur (API menor a 31). */
interface VideoThumbnailRepository {
    suspend fun extractBlurFrameUri(mediaUri: String): Result<String?>
}
