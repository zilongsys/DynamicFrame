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
    onVideoEnded: () -> Unit,
    onPlaybackError: () -> Unit = onVideoEnded,
    onPreloadImages: (List<String>, Int, Int) -> Unit = { _, _, _ -> },
    backgroundType: com.dynamicframe.domain.model.PlaybackBackgroundType = com.dynamicframe.domain.model.PlaybackBackgroundType.BLACK,
    backgroundImageUri: String = "",
    modifier: Modifier = Modifier
) {
    val decodeSize = rememberMaxDecodeSize()

    val effectiveTransition = if (
        currentItem.type == MediaType.VIDEO || nextItem?.type == MediaType.VIDEO
    ) {
        TransitionType.NONE
    } else {
        transitionType
    }

    LaunchedEffect(currentItem.id, nextItem?.id, currentIndex, playlistItems.size) {
        val uris = linkedSetOf<String>()
        if (currentItem.type == MediaType.IMAGE) uris.add(currentItem.uri)
        nextItem?.takeIf { it.type == MediaType.IMAGE }?.uri?.let { uris.add(it) }
        onPreloadImages(uris.toList(), decodeSize.first, decodeSize.second)
    }

    var underlayUri by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(enabled = false)
    ) {
        PlaybackLetterboxBackground(
            type = backgroundType,
            customImageUri = backgroundImageUri,
            modifier = Modifier.fillMaxSize()
        )
        underlayUri?.let { uri ->
            SlideshowImageContent(
                uri = uri,
                transitionType = TransitionType.NONE,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedContent(
            targetState = currentItem,
            contentKey = { it.id },
            transitionSpec = {
                slideshowTransitionSpec(effectiveTransition, transitionDurationMs)
            },
            label = "slide_transition",
            modifier = Modifier.fillMaxSize()
        ) { item ->
            when (item.type) {
                MediaType.IMAGE -> SlideshowImageContent(
                    uri = item.uri,
                    transitionType = effectiveTransition,
                    decodeWidth = decodeSize.first,
                    decodeHeight = decodeSize.second,
                    modifier = Modifier.fillMaxSize(),
                    onDisplayed = { underlayUri = item.uri },
                    onLoadError = onPlaybackError
                )
                MediaType.VIDEO -> SlideshowVideoPlayer(
                    videoPlayer = videoPlayer,
                    uri = item.uri,
                    isPlaying = isPlaying,
                    mediaVolume = mediaVolume,
                    muteAudio = muteVideoAudio,
                    onVideoEnded = onVideoEnded,
                    onPlaybackError = onPlaybackError,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
