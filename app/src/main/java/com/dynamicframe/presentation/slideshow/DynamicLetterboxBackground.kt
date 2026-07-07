package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.ui.components.AppAsyncImage
import com.dynamicframe.ui.components.AppBlurFillImage
import com.dynamicframe.ui.components.FilmGrainOverlay

/**
 * Fondo letterbox dinámico: degradado de colores dominantes de la foto
 * + la misma imagen (o vídeo) en crop con blur y opacidad, como en Paradise.
 */
@Composable
fun DynamicLetterboxBackground(
    mediaUri: String,
    mediaType: MediaType,
    palette: MediaDynamicPalette?,
    isPlaying: Boolean,
    playToken: Int,
    videoBackdropPlayer: VideoBackdropPlayerRepository?,
    videoBlurThumbnailUri: String?,
    modifier: Modifier = Modifier,
    blurAlpha: Float = 1f,
    blurSampling: Float = 5f,
    grainAlpha: Float = 0.22f,
    /** false dentro de AnimatedContent: el fundido lo hace la transición del slide. */
    animateImageLoad: Boolean = true,
    /** false en slideshow dinámico: el blur rellena letterbox; la foto nítida va en la capa superior. */
    showSharpCrop: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicPaletteBrush(palette)),
        )
        when (mediaType) {
            MediaType.IMAGE -> {
                if (showSharpCrop) {
                    AppAsyncImage(
                        uri = mediaUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        decodeWidth = 960,
                        decodeHeight = 540,
                        crossfadeMillis = if (animateImageLoad) 300 else 0,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AppBlurFillImage(
                    uri = mediaUri,
                    contentScale = ContentScale.Crop,
                    blurSampling = blurSampling,
                    alpha = blurAlpha,
                    crossfade = animateImageLoad,
                    modifier = Modifier.fillMaxSize(),
                )
                FilmGrainOverlay(
                    alpha = grainAlpha,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            MediaType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = blurAlpha },
                ) {
                    if (videoBackdropPlayer != null) {
                        ParadiseVideoBlurBackdrop(
                            uri = mediaUri,
                            isPlaying = isPlaying,
                            playToken = playToken,
                            backdropPlayer = videoBackdropPlayer,
                            fallbackThumbnailUri = videoBlurThumbnailUri,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val thumbUri = videoBlurThumbnailUri ?: mediaUri
                        AppAsyncImage(
                            uri = thumbUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            decodeWidth = 960,
                            decodeHeight = 540,
                            crossfadeMillis = if (animateImageLoad) 300 else 0,
                            modifier = Modifier.fillMaxSize(),
                        )
                        AppBlurFillImage(
                            uri = thumbUri,
                            contentScale = ContentScale.Crop,
                            blurSampling = blurSampling,
                            alpha = 1f,
                            crossfade = animateImageLoad,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    FilmGrainOverlay(
                        alpha = grainAlpha,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private fun dynamicPaletteBrush(palette: MediaDynamicPalette?): Brush {
    if (palette == null) {
        return Brush.linearGradient(
            colors = listOf(Color(0xFF121218), Color(0xFF1E1E28)),
        )
    }
    return Brush.linearGradient(
        colors = listOf(
            Color(palette.tertiary),
            Color(palette.primary),
            Color(palette.secondary),
        ),
    )
}
