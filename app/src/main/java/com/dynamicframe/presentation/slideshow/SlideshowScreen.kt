package com.dynamicframe.presentation.slideshow

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dynamicframe.domain.model.ClockPosition
import com.dynamicframe.presentation.common.ConfirmDeleteDialog
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.*
import com.dynamicframe.ui.theme.requestFocusWhenReady
import com.dynamicframe.ui.theme.tvFocusRequester
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    isTV: Boolean = false
) {
    val slideshowState by viewModel.slideshowState.collectAsStateWithLifecycle()
    val config by viewModel.slideshowConfig.collectAsStateWithLifecycle()
    val musicState by viewModel.musicState.collectAsStateWithLifecycle()
    val albumPills by viewModel.albumPills.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    ConfirmDeleteDialog(
        visible = showDeleteConfirm,
        title = "¿Borrar esta foto o vídeo?",
        message = "Se eliminará del dispositivo. Esta acción no se puede deshacer.",
        onConfirm = { viewModel.deleteCurrentSlide() },
        onDismiss = { showDeleteConfirm = false }
    )

    var showControls by remember { mutableStateOf(false) }
    var pauseHasFocus by remember { mutableStateOf(false) }
    val (showActionHint, pulseActionHint) = rememberTransientVisibility(1_000L)

    fun revealControls() {
        showControls = true
        pulseActionHint()
    }

    LaunchedEffect(showControls, pauseHasFocus, isTV) {
        if (!showControls) {
            pulseActionHint()
            return@LaunchedEffect
        }
        if (isTV && !pauseHasFocus) return@LaunchedEffect
        if (!isTV) return@LaunchedEffect
        delay(5_000)
        showControls = false
    }

    val showClock = config.playbackShowClock && showControls
    val showOverlay = showControls && config.playbackShowOverlay
    val contentZoom = config.playbackContentZoom.coerceIn(0.75f, 1f)

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(showClock) {
        if (!showClock) return@LaunchedEffect
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    val screenFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }
    val backgroundCapturesFocus = isTV && !showControls
    var backNavigationEnabled by remember { mutableStateOf(!isTV) }

    LaunchedEffect(Unit) {
        if (isTV) {
            delay(400)
            backNavigationEnabled = true
        }
    }

    LaunchedEffect(showControls, isTV) {
        if (!isTV) return@LaunchedEffect
        delay(150)
        if (showControls) pauseFocus.requestFocusWhenReady()
        else screenFocus.requestFocusWhenReady()
    }

    BackHandler(enabled = isTV && onBack != null && backNavigationEnabled) {
        if (showControls) {
            showControls = false
        } else {
            onBack?.invoke()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.Black)
            .then(
                when {
                    backgroundCapturesFocus -> Modifier
                        .tvFocusRequester(screenFocus)
                        .safeClickable(showFocusBorder = false, onClick = { revealControls() })
                    !isTV -> Modifier
                        .safeClickable(showFocusBorder = false, onClick = { revealControls() })
                    else -> Modifier
                }
            )
    ) {
        val currentItem = slideshowState.currentItem

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable(enabled = false)
        ) {
        if (currentItem != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize(contentZoom)) {
                    PictureFrame(
                        enabled = config.showPictureFrame,
                        scaleFactor = config.playbackPictureFrameScale,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SlideshowMediaViewport(
                            currentItem = currentItem,
                            nextItem = slideshowState.nextItem,
                            playlistItems = slideshowState.playlistItems,
                            currentIndex = slideshowState.currentIndex,
                            transitionType = config.transition,
                            transitionDurationMs = config.transitionDurationMs,
                            isPlaying = slideshowState.isPlaying,
                            muteVideoAudio = config.muteVideoAudio,
                            mediaVolume = config.mediaVolume,
                            onVideoEnded = { viewModel.onVideoCompleted() },
                            backgroundType = config.playbackBackgroundType,
                            backgroundImageUri = config.playbackBackgroundImageUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slideshowState.error ?: "Preparando fotos…",
                    color = GlassText.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
            }
        }
        }

        if (config.playbackShowSafeBorder) {
            PlaybackSafeBorder(modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = showClock,
            enter = fadeIn(androidx.compose.animation.core.tween(400)),
            exit = fadeOut(androidx.compose.animation.core.tween(400))
        ) {
            GlassClockOverlay(
                time = currentTime,
                date = if (config.showDate) currentDate else null,
                position = config.clockPosition,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(androidx.compose.animation.core.tween(280)),
            exit = fadeOut(androidx.compose.animation.core.tween(240))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isTV) Modifier.focusGroup() else Modifier)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (musicState.currentTrack != null && config.playbackShowOverlay) {
                        GlassMusicChip(
                            title = musicState.currentTrack?.title,
                            artist = musicState.currentTrack?.artist,
                            isPlaying = musicState.isPlaying
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val device = LocalDeviceProfile.current
                        if (onBack != null) {
                            GlassIconButton(
                                icon = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                label = if (device.isTv) "Inicio" else "Volver",
                                onClick = {
                                    onBack()
                                }
                            )
                        }
                        if (slideshowState.currentItem != null) {
                            GlassIconButton(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Borrar",
                                label = if (device.isTv) "Borrar" else null,
                                onClick = {
                                    showDeleteConfirm = true
                                }
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            label = if (device.isTv) "Configuración" else "Ajustes",
                            onClick = {
                                onOpenSettings()
                            },
                            prominent = true
                        )
                    }
                }

                val pauseModifier = if (isTV) Modifier.tvFocusRequester(pauseFocus) else Modifier
                CenterPlayPauseButton(
                    isPlaying = slideshowState.isPlaying,
                    onClick = {
                        if (slideshowState.isPlaying) viewModel.pauseSlideshow()
                        else viewModel.startSlideshow()
                    },
                    modifier = pauseModifier.align(Alignment.Center),
                    onFocusChanged = { focused ->
                        pauseHasFocus = focused
                        if (focused) pulseActionHint()
                    }
                )

                if (showOverlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isTV) Modifier.focusGroup() else Modifier)
                    ) {
                        if (config.playbackShowOverlay && albumPills.size > 1) {
                            Text(
                                text = "Álbumes",
                                color = GlassTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            GlassAlbumPillRow(
                                pills = albumPills.map { it.id to it.label },
                                selectedId = selectedAlbumId,
                                onSelect = {
                                    viewModel.selectAlbum(it)
                                }
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        if (slideshowState.totalItems > 0) {
                            val slideProgress =
                                (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                            LinearProgressIndicator(
                                progress = slideProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable(enabled = false),
                                color = Color.White.copy(alpha = 0.9f),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${slideshowState.currentIndex + 1} / ${slideshowState.totalItems}",
                                color = GlassTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlassCircleButton(
                                    icon = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    onClick = {
                                        viewModel.previousSlide()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    onClick = {
                                        viewModel.nextSlide()
                                    }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            ) {
                                if (isTV) {
                                    GlassCircleButton(
                                        icon = Icons.Default.VolumeDown,
                                        contentDescription = "Bajar volumen",
                                        onClick = {
                                            viewModel.setMusicVolume(
                                                (musicState.volume - 0.1f).coerceIn(0f, 1f)
                                            )
                                        }
                                    )
                                    Text(
                                        text = "${(musicState.volume * 100).toInt()}%",
                                        color = GlassTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    GlassCircleButton(
                                        icon = Icons.Default.VolumeUp,
                                        contentDescription = "Subir volumen",
                                        onClick = {
                                            viewModel.setMusicVolume(
                                                (musicState.volume + 0.1f).coerceIn(0f, 1f)
                                            )
                                        }
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.VolumeDown,
                                        null,
                                        tint = GlassTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Slider(
                                        value = musicState.volume,
                                        onValueChange = {
                                            viewModel.setMusicVolume(it)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                    )
                                }
                                GlassCircleButton(
                                    icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = "Música",
                                    onClick = {
                                        viewModel.toggleMusicPlayback()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Siguiente canción",
                                    onClick = {
                                        viewModel.skipNextTrack()
                                    }
                                )
                            }
                        }
                    }
                }
                }
                }
            }
        }

        if (!showControls && showActionHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = if (isTV) "OK: pausar · Atrás: salir" else "Toca: controles",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
        }
    }
    }
}

@Composable
private fun GlassClockOverlay(
    time: String,
    date: String?,
    position: ClockPosition,
    modifier: Modifier = Modifier
) {
    val alignment = when (position) {
        ClockPosition.TOP_LEFT -> Alignment.TopStart
        ClockPosition.TOP_RIGHT -> Alignment.TopEnd
        ClockPosition.BOTTOM_LEFT -> Alignment.BottomStart
        ClockPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        ClockPosition.CENTER -> Alignment.Center
    }

    Box(modifier = modifier, contentAlignment = alignment) {
        GlassSurface(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = if (position == ClockPosition.TOP_LEFT || position == ClockPosition.TOP_RIGHT) 88.dp else 20.dp,
                bottom = if (position == ClockPosition.BOTTOM_LEFT || position == ClockPosition.BOTTOM_RIGHT) 120.dp else 20.dp
            ),
            cornerRadius = 20.dp
        ) {
            Column {
                Text(
                    text = time,
                    color = GlassText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
                if (date != null) {
                    Text(text = date, color = GlassTextMuted, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun GlassMusicChip(
    title: String?,
    artist: String?,
    isPlaying: Boolean
) {
    if (title == null) return
    GlassSurface(cornerRadius = 16.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = null,
                tint = GlassText,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(title, color = GlassText, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                if (artist != null) {
                    Text(artist, color = GlassTextMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    uri: String,
    isPlaying: Boolean,
    mediaVolume: Float,
    muteAudio: Boolean,
    onVideoEnded: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = context.applicationContext
    val exoPlayer = remember {
        ExoPlayer.Builder(appContext).build()
    }
    val effectiveVolume = if (muteAudio) 0f else mediaVolume.coerceIn(0f, 1f)

    LaunchedEffect(uri, muteAudio, mediaVolume, isPlaying) {
        runCatching {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(ExoMediaItem.fromUri(uri))
            exoPlayer.volume = effectiveVolume
            exoPlayer.prepare()
            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) onVideoEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = Modifier.fillMaxSize()
    )
}
