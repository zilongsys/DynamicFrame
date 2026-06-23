package com.dynamicframe.presentation.slideshow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.ui.components.AppAsyncImage
import com.dynamicframe.ui.theme.PlaybackLetterboxBackground
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideshowMediaViewport(
    currentItem: MediaItem,
    nextItem: MediaItem?,
    playlistItems: List<MediaItem>,
    currentIndex: Int,
    transitionType: TransitionType,
    transitionDurationMs: Int,
    isPlaying: Boolean,
    muteVideoAudio: Boolean,
    mediaVolume: Float,
    videoPlayer: SlideshowVideoPlayerRepository,
    playToken: Int = 0,
    videoBackdropPlayer: VideoBackdropPlayerRepository? = null,
    onVideoEnded: () -> Unit,
    onPlaybackError: () -> Unit = onVideoEnded,
    onPreloadImages: (List<String>, Int, Int) -> Unit = { _, _, _ -> },
    backgroundType: com.dynamicframe.domain.model.PlaybackBackgroundType = com.dynamicframe.domain.model.PlaybackBackgroundType.BLACK,
    backgroundImageUri: String = "",
    /** Paradise: el fondo lo aporta la capa blur; no pintar letterbox aquí. */
    skipLetterboxBackground: Boolean = false,
    /** Paradise: crossfade en [ParadiseSlideshowMediaStack]; sin AnimatedContent interno. */
    externalCrossfade: Boolean = false,
    modifier: Modifier = Modifier
) {
    val decodeSize = rememberMaxDecodeSize()
    val isVideo = currentItem.type == MediaType.VIDEO

    val effectiveTransition = if (externalCrossfade || isVideo || nextItem?.type == MediaType.VIDEO) {
        TransitionType.NONE
    } else {
        transitionType
    }

    LaunchedEffect(currentItem.id, nextItem?.id, currentIndex, playlistItems.size) {
        val uris = linkedSetOf<String>()
        if (!isVideo) uris.add(currentItem.uri)
        nextItem?.takeIf { it.type == MediaType.IMAGE }?.uri?.let { uris.add(it) }
        onPreloadImages(uris.toList(), decodeSize.first, decodeSize.second)
    }

    // Foto previa para fundidos suaves imagen→imagen. Se conserva al entrar en modo
    // vídeo (la foto puede ser útil para la transición vídeo→imagen de vuelta).
    var underlayUri by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(enabled = false)
    ) {
        if (!skipLetterboxBackground) {
            PlaybackLetterboxBackground(
                type = backgroundType,
                customImageUri = backgroundImageUri,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Underlay (foto previa) solo bajo imágenes para fundidos suaves.
        if (!externalCrossfade && !isVideo) {
            underlayUri?.let { uri ->
                SlideshowImageContent(
                    uri = uri,
                    transitionType = TransitionType.NONE,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (externalCrossfade && !isVideo) {
            SlideshowImageContent(
                uri = currentItem.uri,
                transitionType = TransitionType.NONE,
                decodeWidth = decodeSize.first,
                decodeHeight = decodeSize.second,
                modifier = Modifier.fillMaxSize(),
                onLoadError = onPlaybackError,
            )
        } else if (!isVideo) {
        // Imágenes con transición animada. targetState es siempre MediaItem para que
        // slideshowTransitionSpec (tipado sobre MediaItem) compile sin cambios.
        // Cuando el ítem es vídeo, el lambda no renderiza nada (el vídeo se dibuja
        // en la capa de abajo separada del AnimatedContent).
        AnimatedContent(
            targetState = currentItem,
            contentKey = { it.id },
            transitionSpec = {
                slideshowTransitionSpec(effectiveTransition, transitionDurationMs)
            },
            label = "slide_transition",
            modifier = Modifier.fillMaxSize()
        ) { item ->
            if (item.type == MediaType.IMAGE) {
                SlideshowImageContent(
                    uri = item.uri,
                    transitionType = effectiveTransition,
                    decodeWidth = decodeSize.first,
                    decodeHeight = decodeSize.second,
                    modifier = Modifier.fillMaxSize(),
                    onDisplayed = { underlayUri = item.uri },
                    onLoadError = onPlaybackError
                )
            }
        }
        }

        // Reproductor de vídeo FUERA de AnimatedContent: así no se recrea al cambiar
        // de un vídeo a otro (solo cambian uri y playToken). Gracias a
        // keepContentOnPlayerReset el último frame permanece visible mientras el
        // nuevo vídeo carga, eliminando el flash negro entre vídeos.
        //
        // En modo externalCrossfade (Paradise), el AnimatedContent externo compone
        // dos instancias simultáneamente durante la transición. Ambas usarían el mismo
        // player singleton causando una carrera. El guard isPlaying limita el player
        // a la instancia activa (isPlaying=true); la instancia saliente no lo monta
        // y por tanto su DisposableEffect no llama stopIfCurrent al desmontarse.
        if (isVideo && (!externalCrossfade || isPlaying)) {
            SlideshowVideoPlayer(
                videoPlayer = videoPlayer,
                uri = currentItem.uri,
                isPlaying = isPlaying,
                mediaVolume = mediaVolume,
                muteAudio = muteVideoAudio,
                onVideoEnded = onVideoEnded,
                onPlaybackError = onPlaybackError,
                playToken = playToken,
                backdropPlayer = videoBackdropPlayer,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun rememberMaxDecodeSize(): Pair<Int, Int> {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val width = (configuration.screenWidthDp * density).roundToInt().coerceIn(480, 1920)
    val height = (configuration.screenHeightDp * density).roundToInt().coerceIn(480, 1080)
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) { width to height }
}

@Composable
fun SlideshowImageContent(
    uri: String,
    transitionType: TransitionType,
    modifier: Modifier = Modifier,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
    onDisplayed: (() -> Unit)? = null,
    onLoadError: (() -> Unit)? = null
) {
    val size = rememberMaxDecodeSize()
    val targetWidth = decodeWidth ?: size.first
    val targetHeight = decodeHeight ?: size.second

    var loadFailed by remember(uri) { mutableStateOf(false) }

    Box(modifier) {
        AppAsyncImage(
            uri = uri,
            contentScale = ContentScale.Fit,
            decodeWidth = targetWidth,
            decodeHeight = targetHeight,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f
                    scaleY = 1f
                },
            onSuccess = {
                loadFailed = false
                onDisplayed?.invoke()
            },
            onError = {
                loadFailed = true
                onLoadError?.invoke()
            }
        )
        if (loadFailed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar", color = Color.White.copy(alpha = 0.45f))
            }
        }
    }
}
