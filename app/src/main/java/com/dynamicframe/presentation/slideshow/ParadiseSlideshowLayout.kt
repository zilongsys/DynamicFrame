@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dynamicframe.presentation.slideshow

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.domain.model.VideoDynamicBackdropMode
import com.dynamicframe.domain.model.WeatherInfo
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.ui.components.AppAsyncImage
import com.dynamicframe.ui.components.AppBlurFillImage
import com.dynamicframe.ui.theme.AppVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val PARADISE_SHARP_CROSSFADE_MS = 1_200
private const val PARADISE_BLUR_CROSSFADE_MS = 800

/**
 * Capas Paradise en pantalla completa (de abajo hacia arriba):
 * 1 blur · 2 medio sharp · 3 vignettes · 4 info · (5 controles en [SlideshowScreen]).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ParadiseSlideshowMediaStack(
    currentItem: MediaItem?,
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    contentZoom: Float,
    videoPlayer: SlideshowVideoPlayerRepository,
    videoBackdropPlayer: VideoBackdropPlayerRepository,
    videoBlurThumbnailUri: String?,
    onVideoEnded: () -> Unit,
    onPlaybackError: () -> Unit,
    onPreloadImages: (List<String>, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlistItems = slideshowState.playlistItems
    val currentIndex = slideshowState.currentIndex
    val animatedVideoBackdrop = config.videoDynamicBackdropMode == VideoDynamicBackdropMode.ANIMATED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentItem != null && playlistItems.isNotEmpty()) {
            AnimatedContent(
                targetState = currentIndex,
                contentKey = { index -> playlistItems.getOrNull(index)?.id ?: index },
                transitionSpec = {
                    fadeIn(animationSpec = tween(PARADISE_BLUR_CROSSFADE_MS)) togetherWith
                        fadeOut(animationSpec = tween(PARADISE_BLUR_CROSSFADE_MS))
                },
                label = "paradise_blur_crossfade",
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                val item = playlistItems.getOrNull(index) ?: return@AnimatedContent
                val isActiveSlide = index == currentIndex
                ParadiseBlurBackdrop(
                    item = item,
                    isPlaying = slideshowState.isPlaying && isActiveSlide,
                    playToken = slideshowState.playToken,
                    videoBackdropPlayer = videoBackdropPlayer,
                    videoBlurThumbnailUri = videoBlurThumbnailUri,
                    animatedVideoBackdrop = animatedVideoBackdrop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedContent(
                targetState = currentIndex,
                contentKey = { index -> playlistItems.getOrNull(index)?.id ?: index },
                transitionSpec = {
                    fadeIn(animationSpec = tween(PARADISE_SHARP_CROSSFADE_MS)) togetherWith
                        fadeOut(animationSpec = tween(PARADISE_SHARP_CROSSFADE_MS))
                },
                label = "paradise_sharp_crossfade",
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                val item = playlistItems.getOrNull(index) ?: return@AnimatedContent
                val isActiveSlide = index == currentIndex
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(contentZoom)
                            .paradiseKenBurns(
                                enabled = item.type == MediaType.IMAGE,
                                photoKey = item.id,
                            ),
                    ) {
                        SlideshowMediaViewport(
                            currentItem = item,
                            nextItem = null,
                            playlistItems = playlistItems,
                            currentIndex = index,
                            transitionType = TransitionType.NONE,
                            transitionDurationMs = 0,
                            isPlaying = slideshowState.isPlaying && isActiveSlide,
                            muteVideoAudio = config.muteVideoAudio,
                            mediaVolume = config.mediaVolume,
                            videoPlayer = videoPlayer,
                            playToken = slideshowState.playToken,
                            videoBackdropPlayer = videoBackdropPlayer,
                            onVideoEnded = onVideoEnded,
                            onPlaybackError = onPlaybackError,
                            onPreloadImages = onPreloadImages,
                            videoDynamicBackdropMode = config.videoDynamicBackdropMode,
                            skipLetterboxBackground = true,
                            externalCrossfade = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            ParadiseVignetteGradients(modifier = Modifier.fillMaxSize())
        }
    }
}

/** Capa 1 — blurred background fill (rellena barras negras con el mismo medio). */
@Composable
fun ParadiseBlurBackdrop(
    item: MediaItem,
    isPlaying: Boolean,
    playToken: Int,
    videoBackdropPlayer: VideoBackdropPlayerRepository,
    videoBlurThumbnailUri: String?,
    animatedVideoBackdrop: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (item.type) {
            MediaType.IMAGE -> {
                // Base crop (por si el blur tarda o falla) + blur encima a opacidad plena.
                AppAsyncImage(
                    uri = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    decodeWidth = 960,
                    decodeHeight = 540,
                    crossfadeMillis = 0,
                    modifier = Modifier.fillMaxSize()
                )
                AppBlurFillImage(
                    uri = item.uri,
                    contentScale = ContentScale.Crop,
                    alpha = 1f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.VIDEO -> {
                if (animatedVideoBackdrop) {
                    ParadiseVideoBlurBackdrop(
                        uri = item.uri,
                        isPlaying = isPlaying,
                        playToken = playToken,
                        backdropPlayer = videoBackdropPlayer,
                        fallbackThumbnailUri = videoBlurThumbnailUri,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val thumbUri = videoBlurThumbnailUri ?: item.uri
                    AppAsyncImage(
                        uri = thumbUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        decodeWidth = 960,
                        decodeHeight = 540,
                        crossfadeMillis = 0,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AppBlurFillImage(
                        uri = thumbUri,
                        contentScale = ContentScale.Crop,
                        alpha = 1f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Capa 3 — viñetas (sin interacción; no interceptan D-pad ni toques). */
@Composable
fun ParadiseVignetteGradients(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .focusable(false)
            .pointerInteropFilter { false },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .paradiseVignetteLayer()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.38f)
                .paradiseVignetteLayer()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.30f)
                .paradiseVignetteLayer()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
    }
}

private fun Modifier.paradiseVignetteLayer(): Modifier =
    focusable(false).pointerInteropFilter { false }

/** Reloj Paradise — esquina inferior izquierda, sin fondo; offset anti burn-in OLED cada 60 s. */
@Composable
fun ParadiseClock(
    showDate: Boolean,
    modifier: Modifier = Modifier,
) {
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            timeText = timeFormat.format(now)
            dateText = dateFormat.format(now)
            delay(1_000L)
        }
    }

    var targetBurnX by remember { mutableIntStateOf(0) }
    var targetBurnY by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            targetBurnX = (-3..3).random()
            targetBurnY = (-3..3).random()
        }
    }

    val burnOffsetX by animateDpAsState(
        targetValue = targetBurnX.dp,
        animationSpec = tween(durationMillis = 800),
        label = "paradiseClockBurnX",
    )
    val burnOffsetY by animateDpAsState(
        targetValue = targetBurnY.dp,
        animationSpec = tween(durationMillis = 800),
        label = "paradiseClockBurnY",
    )

    Column(
        modifier = modifier.offset(x = burnOffsetX, y = burnOffsetY),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = timeText,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.97f),
            letterSpacing = (-0.5).sp,
            lineHeight = 64.sp,
        )
        if (showDate && dateText.isNotEmpty()) {
            Text(
                text = dateText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.70f),
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Widget clima Paradise — esquina superior derecha, sin fondo. */
@Composable
fun ParadiseWeatherWidget(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${weather.temperatureFahrenheit}°F · ${weather.conditionLabel}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = weather.cityName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.50f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = weather.conditionEmoji,
            fontSize = 22.sp,
        )
    }
}

/** Atribución de foto Paradise — esquina inferior derecha, misma altura que el reloj. */
@Composable
fun ParadisePhotoAttribution(
    currentItem: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val albumName = currentItem?.albumName?.takeIf { it.isNotBlank() }
    val visible = albumName != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (albumName != null && currentItem != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = albumName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Photo by ${paradisePhotoCredit(currentItem)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun paradisePhotoCredit(item: MediaItem): String = when (item.source) {
    MediaSource.GOOGLE_PHOTOS -> "Google Photos"
    MediaSource.ONEDRIVE -> "OneDrive"
    MediaSource.NETWORK_SHARE -> "Network share"
    MediaSource.LOCAL -> item.albumName
}

/** Pill de música Paradise — inferior centro, misma altura que reloj y atribución. */
@Composable
fun ParadiseMusicPill(
    musicState: MusicPlayerState,
    modifier: Modifier = Modifier,
) {
    val track = musicState.currentTrack
    val visible = musicState.isPlaying && track != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 400)),
        exit = fadeOut(animationSpec = tween(durationMillis = 400)),
        modifier = modifier,
    ) {
        if (track != null) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(100.dp),
                    )
                    .border(
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(100.dp),
                    )
                    .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF6B35), Color(0xFFF7C59F)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.97f),
                        modifier = Modifier.size(10.dp),
                    )
                }
                Text(
                    text = "${track.title} · ${track.artist}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Capa 4 — reloj, clima, música, atribución, puntos, hint. */
@Composable
fun ParadiseInfoOverlays(
    config: SlideshowConfig,
    slideshowState: SlideshowState,
    musicState: MusicPlayerState,
    weather: WeatherInfo?,
    showDpadHint: Boolean,
    isTV: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(false)
            .pointerInteropFilter { false },
    ) {
        weather?.let { info ->
            ParadiseWeatherWidget(
                weather = info,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 44.dp, top = 40.dp),
            )
        }

        if (config.playbackShowClock) {
            ParadiseClock(
                showDate = config.showDate,
                modifier = Modifier
                    .align(config.clockPosition.toAlignment())
                    .padding(config.clockPosition.toOverlayPadding(paradise = true)),
            )
        }

        if (!config.playbackImmersiveMode && config.playbackShowOverlay) {
            ParadiseMusicPill(
                musicState = musicState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 52.dp),
            )
        }

        ParadisePhotoAttribution(
            currentItem = slideshowState.currentItem,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 44.dp, bottom = 52.dp),
        )

        if (slideshowState.totalItems > 1) {
            ParadisePaginationDots(
                currentIndex = slideshowState.currentIndex,
                totalItems = slideshowState.totalItems,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }

        AnimatedVisibility(
            visible = isTV && showDpadHint,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 600)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 44.dp, bottom = 14.dp),
        ) {
            Text(
                text = "▼  Settings",
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.22f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
fun ParadisePaginationDots(
    currentIndex: Int,
    totalItems: Int,
    modifier: Modifier = Modifier,
    maxVisible: Int = 8,
) {
    val window = if (totalItems <= maxVisible) {
        0 until totalItems
    } else {
        val half = maxVisible / 2
        val start = (currentIndex - half).coerceIn(0, totalItems - maxVisible)
        start until (start + maxVisible)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        window.forEach { index ->
            ParadisePaginationDot(active = index == currentIndex)
        }
    }
}

@Composable
private fun ParadisePaginationDot(active: Boolean) {
    val width by animateDpAsState(
        targetValue = if (active) 16.dp else 3.dp,
        animationSpec = tween(durationMillis = 400),
        label = "paradisePaginationDotWidth",
    )
    Box(
        modifier = Modifier
            .height(3.dp)
            .width(width)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (active) Color.White.copy(alpha = 0.88f)
                else Color.White.copy(alpha = 0.28f),
            ),
    )
}

@Composable
fun ParadiseThemeBadge(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(androidx.compose.animation.core.tween(300)),
        exit = fadeOut(androidx.compose.animation.core.tween(400)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC081220))
                .border(1.dp, AuroraGlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tema: Paradise · ${AppVersion.shortLabel()}",
                color = AuroraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
