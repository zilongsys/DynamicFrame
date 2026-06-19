package com.dynamicframe.presentation.slideshow

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.presentation.common.ConfirmDeleteDialog
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.presentation.permissions.MediaPermissionDeniedBanner
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
    isTV: Boolean = false,
    showPermissionDenied: Boolean = false
) {
    val slideshowState by viewModel.slideshowState.collectAsStateWithLifecycle()
    val config by viewModel.slideshowConfig.collectAsStateWithLifecycle()
    val musicState by viewModel.musicState.collectAsStateWithLifecycle()
    val albumPills by viewModel.albumPills.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var resumeAfterDeleteDismiss by remember { mutableStateOf(false) }
    val (controlHint, setControlHint) = rememberControlHintState(
        if (isTV) "Abajo/OK: controles · Atrás: salir" else "Toca la pantalla para ver controles"
    )

    DisposableEffect(Unit) {
        viewModel.startSlideshow(freshSession = true)
        onDispose {
            viewModel.pauseSlideshow()
        }
    }

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
        onConfirm = {
            resumeAfterDeleteDismiss = false
            showDeleteConfirm = false
            viewModel.deleteCurrentSlide()
        },
        onDismiss = {
            showDeleteConfirm = false
            if (resumeAfterDeleteDismiss) {
                viewModel.startSlideshow()
            }
            resumeAfterDeleteDismiss = false
        }
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
    val bottomBarFocus = remember { FocusRequester() }
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
                        .tvRevealOnDpad(enabled = true, onReveal = { revealControls() })
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
                            videoPlayer = viewModel.slideshowVideoPlayer,
                            playToken = slideshowState.playToken,
                            onVideoEnded = { viewModel.onVideoCompleted() },
                            onPlaybackError = { viewModel.onPlaybackError() },
                            onPreloadImages = viewModel::preloadImages,
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
                if (showPermissionDenied) {
                    MediaPermissionDeniedBanner(
                        message = "Concede acceso a fotos y vídeos en Ajustes del sistema para reproducir el slideshow.",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        text = slideshowState.error ?: "Preparando fotos…",
                        color = GlassText.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                }
            }
        }
        }

        if (config.playbackShowSafeBorder) {
            PlaybackSafeBorder(modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = showClock,
            enter = fadeIn(androidx.compose.animation.core.tween(400)),
            exit = fadeOut(androidx.compose.animation.core.tween(400)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            FrameModeClockTop(
                time = currentTime,
                date = if (config.showDate) currentDate else null
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(androidx.compose.animation.core.tween(280)),
            exit = fadeOut(androidx.compose.animation.core.tween(240))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = if (showClock) 88.dp else 16.dp,
                            bottom = 16.dp
                        ),
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
                                hintDescription = "Volver al panel principal",
                                onFocusHint = setControlHint,
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
                                hintDescription = "Borrar la foto o vídeo actual",
                                onFocusHint = setControlHint,
                                onClick = {
                                    resumeAfterDeleteDismiss = slideshowState.isPlaying
                                    viewModel.pauseSlideshow()
                                    showDeleteConfirm = true
                                }
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            label = if (device.isTv) "Configuración" else "Ajustes",
                            hintDescription = "Abrir ajustes de la app",
                            onFocusHint = setControlHint,
                            onClick = {
                                onOpenSettings()
                            },
                            prominent = true
                        )
                    }
                }

                val pauseModifier = if (isTV) {
                    Modifier
                        .tvFocusRequester(pauseFocus)
                        .focusProperties { down = bottomBarFocus }
                } else {
                    Modifier
                }
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
                    },
                    hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                    onFocusHint = setControlHint
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
                                    icon = Icons.Default.Replay,
                                    contentDescription = "Reiniciar reproducción",
                                    hintDescription = "Reiniciar desde el principio (aplica aleatorio si está activo)",
                                    onFocusHint = setControlHint,
                                    onClick = {
                                        viewModel.restartSlideshow()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    hintDescription = "Foto o vídeo anterior",
                                    onFocusHint = setControlHint,
                                    modifier = if (isTV) Modifier.focusRequester(bottomBarFocus) else Modifier,
                                    onClick = {
                                        viewModel.previousSlide()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    hintDescription = "Foto o vídeo siguiente",
                                    onFocusHint = setControlHint,
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
                                    TvVolumeStepper(
                                        label = "Volumen",
                                        icon = Icons.Default.VolumeUp,
                                        value = musicState.volume,
                                        onValueChange = viewModel::setMusicVolume,
                                        showLabel = false,
                                        horizontalKeysAdjustVolume = false,
                                        hintDescription = "Volumen música. ↑ ↓ ajustar · ← → cambiar botón",
                                        onFocusHint = setControlHint,
                                        modifier = Modifier.weight(1f)
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
                                    hintDescription = if (musicState.isPlaying) "Pausar música" else "Reanudar música",
                                    onFocusHint = setControlHint,
                                    onClick = {
                                        viewModel.toggleMusicPlayback()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Siguiente canción",
                                    hintDescription = "Saltar a la siguiente pista",
                                    onFocusHint = setControlHint,
                                    onClick = {
                                        viewModel.skipNextTrack()
                                    }
                                )
                            }
                        }
                        // Volumen del audio del vídeo (solo visible cuando reproduce un vídeo
                        // y el audio no está silenciado globalmente).
                        val currentItemForVol = slideshowState.currentItem
                        if (currentItemForVol?.type == MediaType.VIDEO && !config.muteVideoAudio) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = GlassTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (isTV) {
                                    TvVolumeStepper(
                                        label = "Vol. vídeo",
                                        icon = Icons.Default.Movie,
                                        value = config.mediaVolume,
                                        onValueChange = viewModel::setMediaVolume,
                                        showLabel = false,
                                        horizontalKeysAdjustVolume = false,
                                        hintDescription = "Volumen del audio del vídeo. ↑ ↓ ajustar · ← → cambiar botón",
                                        onFocusHint = setControlHint,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.VolumeDown,
                                        null,
                                        tint = GlassTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Slider(
                                        value = config.mediaVolume,
                                        onValueChange = viewModel::setMediaVolume,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                    )
                                }
                            }
                        }

                        if (isTV) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = controlHint,
                                color = GlassTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable(enabled = false),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2
                            )
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
                    text = if (isTV) "Abajo/OK: controles · Atrás: salir" else "Toca: controles",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
        }
    }
    }
}

@Composable
private fun FrameModeClockTop(
    time: String,
    date: String?
) {
    GlassSurface(cornerRadius = 16.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = time,
                color = GlassText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light
            )
            if (date != null) {
                Text(text = date, color = GlassTextMuted, fontSize = 13.sp, maxLines = 1)
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
