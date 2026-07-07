package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.repository.MediaPaletteRepository
import javax.inject.Inject

class GetMediaDynamicPaletteUseCase @Inject constructor(
    private val mediaPaletteRepository: MediaPaletteRepository,
) {
    fun peekCached(imageUri: String): MediaDynamicPalette? =
        mediaPaletteRepository.getCached(imageUri)

    suspend fun preload(imageUri: String) {
        mediaPaletteRepository.preload(imageUri)
    }

    suspend operator fun invoke(imageUri: String): Result<MediaDynamicPalette> =
        mediaPaletteRepository.extractFromImageUri(imageUri)
}
