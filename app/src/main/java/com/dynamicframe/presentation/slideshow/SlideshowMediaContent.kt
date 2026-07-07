package com.dynamicframe.presentation.slideshow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.PlaybackBackgroundType
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.domain.slideshow.DynamicBackdropPrefetcher
import com.dynamicframe.domain.slideshow.transitionMillis
import com.dynamicframe.ui.components.AppSharpImageWhenReady
import com.dynamicframe.ui.components.rememberAppImagePainter
import com.dynamicframe.ui.theme.PlaybackLetterboxBackground
import coil.compose.AsyncImagePainter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Diapositiva completa (fondo + foto) lista para retener o mostrar. */
private data class HeldSlide(
    val itemId: String,
    val uri: String,
    val palette: MediaDynamicPalette?,
)

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
    backgroundType: PlaybackBackgroundType = PlaybackBackgroundType.BLACK,
    backgroundImageUri: String = "",
    dynamicPalette: MediaDynamicPalette? = null,
    dynamicPalettes: Map<String, MediaDynamicPalette> = emptyMap(),
    videoBlurThumbnailUri: String? = null,
    skipLetterboxBackground: Boolean = false,
    externalCrossfade: Boolean = false,
    modifier: Modifier = Modifier
) {
    val decodeSize = rememberMaxDecodeSize()
    val isVideo = currentItem.type == MediaType.VIDEO
    val useDynamicBackground = backgroundType == PlaybackBackgroundType.DYNAMIC && !skipLetterboxBackground
    val dynamicBackgroundInTransition = useDynamicBackground && !isVideo && !externalCrossfade
    val dynamicDecodeW = DynamicBackdropPrefetcher.DECODE_WIDTH
    val dynamicDecodeH = DynamicBackdropPrefetcher.DECODE_HEIGHT

    val effectiveTransition = when {
        externalCrossfade || isVideo -> TransitionType.NONE
        else -> transitionType
    }

    LaunchedEffect(currentItem.id, nextItem?.id, currentIndex, playlistItems.size) {
        val uris = linkedSetOf<String>()
        if (!isVideo) uris.add(currentItem.uri)
        nextItem?.takeIf { it.type == MediaType.IMAGE }?.uri?.let { uris.add(it) }
        val w = if (dynamicBackgroundInTransition) dynamicDecodeW else decodeSize.first
        val h = if (dynamicBackgroundInTransition) dynamicDecodeH else decodeSize.second
        onPreloadImages(uris.toList(), w, h)
    }

    var heldSlide by remember { mutableStateOf<HeldSlide?>(null) }
    var compositeReady by remember { mutableStateOf<HeldSlide?>(null) }

    LaunchedEffect(currentItem.id) {
        compositeReady = null
    }

    // Retención: solo tras transición Y cuando foto+fondo están listos (evita fondo solo / blanco).
    LaunchedEffect(
        compositeReady?.itemId,
        currentItem.id,
        transitionDurationMs,
        effectiveTransition,
    ) {
        val ready = compositeReady ?: return@LaunchedEffect
        if (ready.itemId != currentItem.id) return@LaunchedEffect
        val delayMs = if (heldSlide == null) {
            0L
        } else {
            transitionMillis(effectiveTransition, transitionDurationMs).coerceAtLeast(1).toLong() + 32L
        }
        delay(delayMs)
        if (compositeReady?.itemId == currentItem.id) {
            heldSlide = ready
        }
    }

    fun onSlideCompositeReady(item: MediaItem, palette: MediaDynamicPalette?) {
        compositeReady = HeldSlide(
            itemId = item.id,
            uri = item.uri,
            palette = if (dynamicBackgroundInTransition) palette else null,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(enabled = false)
    ) {
        if (!skipLetterboxBackground) {
            when {
                useDynamicBackground && isVideo -> {
                    DynamicLetterboxBackground(
                        mediaUri = currentItem.uri,
                        mediaType = currentItem.type,
                        palette = dynamicPalette ?: dynamicPalettes[currentItem.id],
                        isPlaying = isPlaying,
                        playToken = playToken,
                        videoBackdropPlayer = videoBackdropPlayer,
                        videoBlurThumbnailUri = videoBlurThumbnailUri,
                        animateImageLoad = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                backgroundType != PlaybackBackgroundType.DYNAMIC -> {
                    PlaybackLetterboxBackground(
                        type = backgroundType,
                        customImageUri = backgroundImageUri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!externalCrossfade && !isVideo) {
            heldSlide?.let { held ->
                RetainedCompleteImageSlide(
                    target = held,
                    dynamicBackground = dynamicBackgroundInTransition,
                    decodeWidth = if (dynamicBackgroundInTransition) dynamicDecodeW else decodeSize.first,
                    decodeHeight = if (dynamicBackgroundInTransition) dynamicDecodeH else decodeSize.second,
                )
            }
        }

        if (externalCrossfade && !isVideo) {
            CompleteImageSlide(
                uri = currentItem.uri,
                palette = null,
                dynamicBackground = false,
                decodeWidth = decodeSize.first,
                decodeHeight = decodeSize.second,
                onError = onPlaybackError,
            )
        } else if (!isVideo) {
            AnimatedContent(
                targetState = currentItem,
                contentKey = { it.id },
                transitionSpec = {
                    slideshowTransitionSpec(effectiveTransition, transitionDurationMs)
                },
                label = "slide_transition",
                modifier = Modifier.fillMaxSize()
            ) { item ->
                val slidePalette = remember(item.id, dynamicPalettes[item.id]) {
                    dynamicPalettes[item.id]
                }
                if (dynamicBackgroundInTransition) {
                    CompleteImageSlide(
                        uri = item.uri,
                        palette = slidePalette,
                        dynamicBackground = true,
                        decodeWidth = dynamicDecodeW,
                        decodeHeight = dynamicDecodeH,
                        onError = onPlaybackError,
                        onSuccess = { onSlideCompositeReady(item, slidePalette) },
                    )
                } else if (item.type == MediaType.IMAGE) {
                    CompleteImageSlide(
                        uri = item.uri,
                        palette = null,
                        dynamicBackground = false,
                        decodeWidth = decodeSize.first,
                        decodeHeight = decodeSize.second,
                        onError = onPlaybackError,
                        onSuccess = { onSlideCompositeReady(item, null) },
                    )
                }
            }
        }

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

/** Capa de retención: mantiene la diapositiva anterior visible hasta que la nueva esté en caché. */
@Composable
private fun RetainedCompleteImageSlide(
    target: HeldSlide,
    dynamicBackground: Boolean,
    decodeWidth: Int,
    decodeHeight: Int,
) {
    var displayed by remember { mutableStateOf<HeldSlide?>(null) }

    val incomingPainter = rememberAppImagePainter(
        uri = target.uri,
        decodeWidth = decodeWidth,
        decodeHeight = decodeHeight,
    )
    LaunchedEffect(incomingPainter.state, target.itemId) {
        if (incomingPainter.state is AsyncImagePainter.State.Success) {
            displayed = target
        }
    }

    val slide = displayed ?: return
    val showPainter = rememberAppImagePainter(
        uri = slide.uri,
        decodeWidth = decodeWidth,
        decodeHeight = decodeHeight,
    )
    if (showPainter.state !is AsyncImagePainter.State.Success) return

    PaintedCompleteImageSlide(
        uri = slide.uri,
        palette = slide.palette,
        painter = showPainter,
        dynamicBackground = dynamicBackground,
    )
}

/** Fondo dinámico + foto nítida: siempre juntos, solo cuando Coil confirma la imagen. */
@Composable
private fun CompleteImageSlide(
    uri: String,
    palette: MediaDynamicPalette?,
    dynamicBackground: Boolean,
    decodeWidth: Int,
    decodeHeight: Int,
    onError: () -> Unit = {},
    onSuccess: () -> Unit = {},
) {
    AppSharpImageWhenReady(
        uri = uri,
        decodeWidth = decodeWidth,
        decodeHeight = decodeHeight,
        modifier = Modifier.fillMaxSize(),
        onError = onError,
        onSuccess = onSuccess,
    ) { painter ->
        PaintedCompleteImageSlide(
            uri = uri,
            palette = palette,
            painter = painter,
            dynamicBackground = dynamicBackground,
        )
    }
}

@Composable
private fun PaintedCompleteImageSlide(
    uri: String,
    palette: MediaDynamicPalette?,
    painter: AsyncImagePainter,
    dynamicBackground: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        if (dynamicBackground) {
            DynamicLetterboxBackground(
                mediaUri = uri,
                mediaType = MediaType.IMAGE,
                palette = palette,
                isPlaying = false,
                playToken = 0,
                videoBackdropPlayer = null,
                videoBlurThumbnailUri = null,
                animateImageLoad = false,
                showSharpCrop = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
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
    crossfadeMillis: Int = 0,
    onDisplayed: (() -> Unit)? = null,
    onLoadError: (() -> Unit)? = null
) {
    val size = rememberMaxDecodeSize()
    val targetWidth = decodeWidth ?: size.first
    val targetHeight = decodeHeight ?: size.second

    AppSharpImageWhenReady(
        uri = uri,
        decodeWidth = targetWidth,
        decodeHeight = targetHeight,
        modifier = modifier,
        onError = { onLoadError?.invoke() },
        onSuccess = { onDisplayed?.invoke() },
    ) { painter ->
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f
                    scaleY = 1f
                },
        )
    }
}
