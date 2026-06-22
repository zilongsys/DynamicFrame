package com.dynamicframe.presentation.slideshow

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.presentation.common.ConfirmDeleteDialog
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
    // Cada interacción (navegación de foco, click, ajuste de volumen) incrementa
    // este contador para reiniciar el temporizador de auto-ocultado.
    var interactionTick by remember { mutableIntStateOf(0) }
    val (showActionHint, pulseActionHint) = rememberTransientVisibility(1_000L)

    fun touch() { interactionTick++ }

    fun revealControls() {
        showControls = true
        touch()
        pulseActionHint()
    }

    // Auto-ocultado robusto: en TV oculta tras 6 s de inactividad sin importar qué
    // control tiene el foco (antes solo ocultaba si el foco estaba en play/pausa).
    // En móvil, cualquier interacción reinicia el temporizador (antes desaparecía a
    // mitad de un gesto). Se reinicia siempre que cambie [interactionTick].
    LaunchedEffect(showControls, interactionTick, isTV) {
        if (!showControls) {
            pulseActionHint()
            return@LaunchedEffect
        }
        delay(if (isTV) 6_000 else 5_000)
        showControls = false
    }

    val showClock = config.playbackShowClock && showControls
    val contentZoom = config.playbackContentZoom.coerceIn(0.75f, 1f)

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(showClock) {
        if (!showClock) return@LaunchedEffect
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
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

    // Callbacks compartidos por los 3 temas. Cada acción reinicia el temporizador.
    val callbacks = PlaybackControlsCallbacks(
            onBack = onBack,
            onDelete = {
                touch()
                resumeAfterDeleteDismiss = slideshowState.isPlaying
                viewModel.pauseSlideshow()
                showDeleteConfirm = true
            },
            onOpenSettings = { touch(); onOpenSettings() },
            onPlayPause = {
                touch()
                if (slideshowState.isPlaying) viewModel.pauseSlideshow()
                else viewModel.startSlideshow()
            },
            onPrevious = { touch(); viewModel.previousSlide() },
            onNext = { touch(); viewModel.nextSlide() },
            onRestart = { touch(); viewModel.restartSlideshow() },
            onToggleMusic = { touch(); viewModel.toggleMusicPlayback() },
            onSkipTrack = { touch(); viewModel.skipNextTrack() },
            onMusicVolume = { touch(); viewModel.setMusicVolume(it) },
            onMediaVolume = { touch(); viewModel.setMediaVolume(it) },
            onSelectAlbum = { touch(); viewModel.selectAlbum(it) },
            setControlHint = { hint -> setControlHint(hint); touch() },
            onPauseFocusChanged = { focused -> if (focused) pulseActionHint() }
    )

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
                        text = slideshowState.error
                            ?: if (slideshowState.totalItems == 0) "No hay fotos ni vídeos. Añade carpetas en Ajustes."
                            else "Preparando fotos…",
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
            PlaybackControlsOverlay(
                slideshowState = slideshowState,
                config = config,
                musicState = musicState,
                pills = albumPills.map { it.id to it.label },
                selectedAlbumId = selectedAlbumId,
                isTV = isTV,
                controlHint = controlHint,
                pauseFocus = pauseFocus,
                bottomBarFocus = bottomBarFocus,
                cb = callbacks
            )
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
