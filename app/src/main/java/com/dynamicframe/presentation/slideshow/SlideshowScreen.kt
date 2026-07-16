package com.dynamicframe.presentation.slideshow

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.dynamicframe.domain.model.PlaybackBackgroundType
import com.dynamicframe.domain.model.PlaybackTheme
import com.dynamicframe.domain.model.isParadiseActive
import com.dynamicframe.presentation.common.ConfirmDeleteDialog
import com.dynamicframe.presentation.common.DeleteMediaFailureDialog
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
    val videoBlurThumbnailUri by viewModel.videoBlurThumbnailUri.collectAsStateWithLifecycle()
    val dynamicLetterboxPalette by viewModel.dynamicLetterboxPalette.collectAsStateWithLifecycle()
    val dynamicPalettes by viewModel.dynamicPalettes.collectAsStateWithLifecycle()
    val presentationPhase by viewModel.presentationPhase.collectAsStateWithLifecycle()
    val isPresenting = presentationPhase == SlideshowPresentationPhase.Presenting
    val isPreparing = presentationPhase == SlideshowPresentationPhase.Preparing
    val weatherInfo by viewModel.weatherInfo.collectAsStateWithLifecycle()
    val paradiseControlsVisible by viewModel.controlsVisible.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<com.dynamicframe.domain.model.MediaItem?>(null) }
    val deleteFailure by viewModel.deleteFailure.collectAsStateWithLifecycle()
    val deleteConsentIntentSender by viewModel.deleteConsentIntentSender.collectAsStateWithLifecycle()

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(deleteConsentIntentSender) {
        val sender = deleteConsentIntentSender ?: return@LaunchedEffect
        deleteConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        viewModel.clearDeleteConsentIntent()
    }
    val (controlHint, setControlHint) = rememberControlHintState(
        if (isTV) "Abajo/OK: controles · Atrás: salir" else "Toca la pantalla para ver controles"
    )

    DisposableEffect(Unit) {
        viewModel.onSlideshowScreenVisible()
        onDispose {
            viewModel.stopSlideshow()
        }
    }

    // Pantalla completa tipo marco: ocultar barras del sistema; restaurar al salir.
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    DeleteMediaFailureDialog(
        failure = deleteFailure,
        onDismiss = { viewModel.clearDeleteFailure() },
        onOpenContentSettings = onOpenSettings,
    )

    ConfirmDeleteDialog(
        visible = showDeleteConfirm,
        title = "¿Borrar esta foto o vídeo?",
        message = "Se eliminará del dispositivo. Esta acción no se puede deshacer.",
        thumbnailUri = pendingDeleteItem?.let { item ->
            item.thumbnailUri ?: item.uri
        },
        itemName = pendingDeleteItem?.name.orEmpty(),
        mediaType = pendingDeleteItem?.type,
        onConfirm = {
            showDeleteConfirm = false
            pendingDeleteItem?.let { viewModel.confirmDelete(it) }
            pendingDeleteItem = null
        },
        onDismiss = {
            showDeleteConfirm = false
            viewModel.cancelDelete()
            pendingDeleteItem = null
        },
    )

    var showControls by remember { mutableStateOf(false) }
    val isParadise = config.isParadiseActive()
    val controlsShown = if (isParadise) paradiseControlsVisible else showControls
    // Cada interacción (navegación de foco, click, ajuste de volumen) incrementa
    // este contador para reiniciar el temporizador de auto-ocultado.
    var interactionTick by remember { mutableIntStateOf(0) }
    val (showActionHint, pulseActionHint) = rememberTransientVisibility(1_000L)
    var showParadiseDpadHint by remember { mutableStateOf(false) }
    var paradiseDpadHintTimerTick by remember { mutableIntStateOf(0) }

    fun pulseParadiseDpadHint() {
        if (!isTV) return
        showParadiseDpadHint = true
        paradiseDpadHintTimerTick++
    }

    fun touch() {
        interactionTick++
        if (isParadise) viewModel.onControlsInteraction()
    }

    fun revealControls() {
        if (isParadise) {
            viewModel.onRemoteOkPressed()
            return
        }
        showControls = true
        touch()
        pulseActionHint()
    }

    // Auto-ocultado (temas no-Paradise): TV 6 s / móvil 5 s.
    LaunchedEffect(showControls, interactionTick, isTV, isParadise) {
        if (isParadise || !showControls) {
            if (!isParadise && !showControls) pulseActionHint()
            return@LaunchedEffect
        }
        delay(if (isTV) 6_000 else 5_000)
        showControls = false
    }

    val isParadiseEarly = isParadise
    LaunchedEffect(isParadiseEarly, isTV) {
        if (isParadiseEarly && isTV) {
            showParadiseDpadHint = true
            paradiseDpadHintTimerTick++
        } else {
            showParadiseDpadHint = false
        }
    }

    LaunchedEffect(paradiseDpadHintTimerTick) {
        if (!showParadiseDpadHint) return@LaunchedEffect
        delay(10_000L)
        showParadiseDpadHint = false
    }

    LaunchedEffect(controlsShown, isParadiseEarly, isTV) {
        if (isParadiseEarly && isTV && !controlsShown) {
            pulseParadiseDpadHint()
        }
    }

    val showClock = config.playbackShowClock && (
        if (config.playbackImmersiveMode) controlsShown
        else (isParadise || controlsShown)
    )
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

    // Confirmación breve del tema activo al entrar (también sirve de diagnóstico:
    // si no aparece, el APK en ejecución no incluye los cambios de temas).
    var showThemeBadge by remember { mutableStateOf(true) }
    LaunchedEffect(config.appTheme, config.playbackTheme) {
        showThemeBadge = true
        delay(2500)
        showThemeBadge = false
    }

    val screenFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }
    val bottomBarFocus = remember { FocusRequester() }
    // Paradise tiene su propio pill de controles, separado del overlay de Aurora/Ambiente/Galería.
    // Necesita un FocusRequester independiente para que el D-pad entre en los IconButtons
    // cuando los controles aparecen tras pulsar OK.
    val paradiseFocus = remember { FocusRequester() }
    val themeAlwaysVisible =
        !isParadise &&
            !config.playbackImmersiveMode &&
            config.playbackTheme != PlaybackTheme.AURORA_GLASS
    val backgroundCapturesFocus = isTV && !controlsShown && !themeAlwaysVisible
    var backNavigationEnabled by remember { mutableStateOf(!isTV) }

    LaunchedEffect(Unit) {
        if (isTV) {
            delay(400)
            backNavigationEnabled = true
        }
    }

    LaunchedEffect(controlsShown, isTV, themeAlwaysVisible, isParadise) {
        if (!isTV) return@LaunchedEffect
        delay(150)
        when {
            // Paradise usa su propio pill: el foco va a paradiseFocus (primer botón del pill).
            isParadise && controlsShown -> paradiseFocus.requestFocusWhenReady()
            // Temas clásicos (Aurora, Ambiente, Galería): foco al botón de pausa central.
            controlsShown || themeAlwaysVisible -> pauseFocus.requestFocusWhenReady()
            // Sin controles visibles: foco al fondo (captura teclas del mando).
            else -> screenFocus.requestFocusWhenReady()
        }
    }

    BackHandler(enabled = isTV && onBack != null && backNavigationEnabled) {
        if (controlsShown) {
            if (isParadise) viewModel.hideControls() else showControls = false
        } else {
            onBack?.invoke()
        }
    }

    // Callbacks compartidos por los 3 temas. Cada acción reinicia el temporizador.
    val callbacks = PlaybackControlsCallbacks(
            onBack = onBack,
            onDelete = {
                touch()
                val item = slideshowState.currentItem ?: return@PlaybackControlsCallbacks
                pendingDeleteItem = item
                if (viewModel.prepareDelete(item)) {
                    showDeleteConfirm = true
                }
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
                if (isParadise) {
                    Modifier.onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionCenter,
                            Key.Enter,
                            Key.NumPadEnter,
                            Key.ButtonA -> {
                                viewModel.onRemoteOkPressed()
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                when {
                    backgroundCapturesFocus && isParadise -> Modifier
                        .tvFocusRequester(screenFocus)
                        .paradiseScreensaverKeys(
                            onOk = { viewModel.onRemoteOkPressed() },
                            onOtherRemoteKey = { pulseParadiseDpadHint() },
                        )
                        .safeClickable(showFocusBorder = false, onClick = { viewModel.onRemoteOkPressed() })
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

        if (isPreparing) {
            DynamicLetterboxLoadingOverlay(modifier = Modifier.fillMaxSize())
        } else if (isParadise) {
            if (currentItem != null && isPresenting) {
                ParadiseSlideshowMediaStack(
                    currentItem = currentItem,
                    slideshowState = slideshowState,
                    config = config,
                    contentZoom = contentZoom,
                    videoPlayer = viewModel.slideshowVideoPlayer,
                    videoBackdropPlayer = viewModel.videoBackdropPlayer,
                    videoBlurThumbnailUri = videoBlurThumbnailUri,
                    onVideoEnded = { viewModel.onVideoCompleted() },
                    onPlaybackError = { viewModel.onPlaybackError() },
                    onPreloadImages = viewModel::preloadImages
                )
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

            ParadiseInfoOverlays(
                config = config,
                slideshowState = slideshowState,
                musicState = musicState,
                weather = weatherInfo,
                showDpadHint = isPresenting &&
                    !controlsShown &&
                    showParadiseDpadHint &&
                    !config.playbackImmersiveMode,
                isTV = isTV
            )

            ParadiseThemeBadge(
                visible = showThemeBadge && isPresenting,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable(enabled = false)
        ) {
        if (currentItem != null && isPresenting) {
            val mediaViewport: @Composable () -> Unit = {
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
                    dynamicPalette = dynamicLetterboxPalette,
                    dynamicPalettes = dynamicPalettes,
                    videoBackdropPlayer = if (config.playbackBackgroundType == PlaybackBackgroundType.DYNAMIC) {
                        viewModel.videoBackdropPlayer
                    } else {
                        null
                    },
                    videoBlurThumbnailUri = videoBlurThumbnailUri,
                    videoDynamicBackdropMode = config.videoDynamicBackdropMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize(contentZoom)) {
                    when (config.playbackTheme) {
                        // Galería: paspartú editorial siempre visible (tema "museo").
                        PlaybackTheme.GALLERY -> GalleryMatFrame(
                            scaleFactor = config.playbackPictureFrameScale,
                            modifier = Modifier.fillMaxSize()
                        ) { mediaViewport() }
                        // Ambiente: a sangre, sin marco (minimalismo cinematográfico).
                        PlaybackTheme.AMBIENT -> Box(Modifier.fillMaxSize()) { mediaViewport() }
                        // Aurora Glass: respeta el marco dorado opcional del usuario.
                        else -> PictureFrame(
                            enabled = config.showPictureFrame,
                            scaleFactor = config.playbackPictureFrameScale,
                            modifier = Modifier.fillMaxSize()
                        ) { mediaViewport() }
                    }
                }
            }
        } else if (!isPreparing) {
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
        }

        if (!isParadise && config.playbackShowSafeBorder) {
            PlaybackSafeBorder(modifier = Modifier.fillMaxSize())
        }

        if (!isParadise) {
        AnimatedVisibility(
            visible = showThemeBadge && !config.playbackImmersiveMode,
            enter = fadeIn(androidx.compose.animation.core.tween(300)),
            exit = fadeOut(androidx.compose.animation.core.tween(400)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            val themeName = when (config.playbackTheme) {
                PlaybackTheme.AMBIENT -> "Ambiente"
                PlaybackTheme.GALLERY -> "Galería"
                else -> "Aurora Glass"
            }
            when (config.playbackTheme) {
                PlaybackTheme.AURORA_GLASS -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC081220))
                            .border(1.dp, AuroraGlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tema: $themeName · ${AppVersion.shortLabel()}",
                            color = AuroraText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> GlassSurface(cornerRadius = 12.dp) {
                    Text(
                        text = "Tema: $themeName · ${AppVersion.shortLabel()}",
                        color = GlassText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showClock,
            enter = fadeIn(androidx.compose.animation.core.tween(400)),
            exit = fadeOut(androidx.compose.animation.core.tween(400)),
            modifier = Modifier
                .align(config.clockPosition.toAlignment())
                .padding(config.clockPosition.toOverlayPadding())
        ) {
            when (config.playbackTheme) {
                PlaybackTheme.AURORA_GLASS -> AuroraClockTop(
                    time = currentTime,
                    date = if (config.showDate) currentDate else null
                )
                else -> FrameModeClockTop(
                    time = currentTime,
                    date = if (config.showDate) currentDate else null
                )
            }
        }
        }

        if (isParadise && config.playbackShowSafeBorder) {
            PlaybackSafeBorder(modifier = Modifier.fillMaxSize())
        }

        // Paradise: controles pill inferiores solo tras OK/tocar (4 s auto-hide en ViewModel).
        if (isParadise) {
            AnimatedVisibility(
                visible = paradiseControlsVisible,
                enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(animationSpec = tween(300)) { it / 2 },
                exit = fadeOut(animationSpec = tween(400)) +
                    slideOutVertically(animationSpec = tween(400)) { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ParadiseScreensaverControls(
                    slideshowState = slideshowState,
                    config = config,
                    onPlayPause = {
                        touch()
                        if (slideshowState.isPlaying) viewModel.pauseSlideshow()
                        else viewModel.startSlideshow()
                    },
                    onSkipTrack = {
                        touch()
                        viewModel.skipNextTrack()
                    },
                    onMusicVolume = {
                        touch()
                        viewModel.setMusicVolume(it)
                    },
                    onInteraction = { viewModel.onControlsInteraction() },
                    firstButtonFocus = if (isTV) paradiseFocus else null,
                )
            }
        } else if (isPresenting && (themeAlwaysVisible || showControls)) {
            val overlayConfig = config
            PlaybackControlsOverlay(
                slideshowState = slideshowState,
                config = overlayConfig,
                musicState = musicState,
                pills = albumPills,
                selectedAlbumId = selectedAlbumId,
                isTV = isTV,
                controlHint = controlHint,
                pauseFocus = pauseFocus,
                bottomBarFocus = bottomBarFocus,
                expanded = showControls || themeAlwaysVisible,
                cb = callbacks
            )
        }

        if (!isParadise && !showControls && !themeAlwaysVisible && showActionHint) {
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
