package com.dynamicframe.presentation.slideshow

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.ui.theme.PlaybackLetterboxBackground
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.focusable

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
    onVideoEnded: () -> Unit,
    backgroundType: com.dynamicframe.domain.model.PlaybackBackgroundType = com.dynamicframe.domain.model.PlaybackBackgroundType.BLACK,
    backgroundImageUri: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = context.imageLoader
    val decodeSize = rememberMaxDecodeSize()

    val effectiveTransition = if (
        currentItem.type == MediaType.VIDEO || nextItem?.type == MediaType.VIDEO
    ) {
        TransitionType.NONE
    } else {
        transitionType
    }

    LaunchedEffect(currentItem.id, nextItem?.id, currentIndex, playlistItems.size) {
        val uris = linkedSetOf<Uri>()
        if (currentItem.type == MediaType.IMAGE) uris.add(currentItem.uri)
        nextItem?.takeIf { it.type == MediaType.IMAGE }?.uri?.let { uris.add(it) }
        uris.forEach { uri ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .size(decodeSize.first, decodeSize.second)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    var underlayUri by remember { mutableStateOf<Uri?>(null) }

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
                kenBurnsEnabled = false,
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
                    onDisplayed = { underlayUri = item.uri }
                )
                MediaType.VIDEO -> VideoPlayer(
                    uri = item.uri.toString(),
                    isPlaying = isPlaying,
                    mediaVolume = mediaVolume,
                    muteAudio = muteVideoAudio,
                    onVideoEnded = onVideoEnded
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
    uri: Uri,
    transitionType: TransitionType,
    modifier: Modifier = Modifier,
    kenBurnsEnabled: Boolean = true,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
    onDisplayed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val size = rememberMaxDecodeSize()
    val targetWidth = decodeWidth ?: size.first
    val targetHeight = decodeHeight ?: size.second
    val enableKenBurns = false // Sin zoom: la imagen se muestra completa sin recortar

    val scale = 1f

    var loadFailed by remember(uri) { mutableStateOf(false) }

    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .size(targetWidth, targetHeight)
                .crossfade(450)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            onSuccess = {
                loadFailed = false
                onDisplayed?.invoke()
            },
            onError = { loadFailed = true }
        )
        if (loadFailed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar", color = Color.White.copy(alpha = 0.45f))
            }
        }
    }
}
