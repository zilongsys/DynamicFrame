package com.dynamicframe.presentation.slideshow

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dynamicframe.domain.model.ClockPosition
import com.dynamicframe.domain.model.MediaType
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

    val immersive = config.playbackImmersiveMode
    var showControls by remember(immersive, isTV) {
        mutableStateOf(!immersive && !isTV)
    }

    LaunchedEffect(immersive) {
        if (immersive) showControls = false
    }

    LaunchedEffect(showControls, immersive) {
        if (showControls && immersive) {
            delay(6_000)
            showControls = false
        }
    }

    fun toggleControls() {
        showControls = !showControls
    }

    val showClock = config.playbackShowClock && (!immersive || showControls)
    val showUi = (config.playbackShowOverlay && !immersive) || (immersive && showControls)

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
    LaunchedEffect(isTV) {
        if (isTV) {
            delay(350)
            screenFocus.requestFocusWhenReady()
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
            .tvFocusRequester(screenFocus)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    toggleControls()
                    true
                } else false
            }
            .safeClickable(showFocusBorder = false, onClick = { toggleControls() })
    ) {
        val currentItem = slideshowState.currentItem

        if (currentItem != null) {
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    slideshowTransitionSpec(config.transition, config.transitionDurationMs)
                },
                label = "slide_transition",
                modifier = Modifier.fillMaxSize()
            ) { item ->
                when (item.type) {
                    MediaType.IMAGE -> SlideshowImageContent(
                        uri = item.uri,
                        transitionType = config.transition,
                        modifier = Modifier.fillMaxSize()
                    )
                    MediaType.VIDEO -> VideoPlayer(
                        uri = item.uri.toString(),
                        isPlaying = slideshowState.isPlaying,
                        muteAudio = config.muteVideoAudio,
                        onVideoEnded = { viewModel.onVideoCompleted() }
                    )
                }
            }
        } else {
            AuroraBackground(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GlassText)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = slideshowState.error ?: "Cargando medios...",
                        color = GlassText,
                        fontSize = 18.sp
                    )
                }
            }
        }

        if (!immersive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f))
            )
        }

        AnimatedVisibility(
            visible = showClock,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            GlassClockOverlay(
                time = currentTime,
                date = if (config.showDate) currentDate else null,
                position = config.clockPosition,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(tween(350)) + slideInVertically { it / 8 },
            exit = fadeOut(tween(300)) + slideOutVertically { it / 8 }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
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
                                onClick = onBack
                            )
                        }
                        if (slideshowState.currentItem != null) {
                            GlassIconButton(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Borrar",
                                label = if (device.isTv) "Borrar" else null,
                                onClick = { showDeleteConfirm = true }
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            label = if (device.isTv) "Configuración" else "Ajustes",
                            onClick = onOpenSettings,
                            prominent = true
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    cornerRadius = 28.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                onSelect = { viewModel.selectAlbum(it) }
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        if (slideshowState.totalItems > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                                    onClick = { viewModel.previousSlide() }
                                )
                                GlassCircleButton(
                                    icon = if (slideshowState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (slideshowState.isPlaying) "Pausar" else "Reproducir",
                                    onClick = {
                                        if (slideshowState.isPlaying) viewModel.pauseSlideshow()
                                        else viewModel.startSlideshow()
                                    }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    onClick = { viewModel.nextSlide() }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeDown,
                                    null,
                                    tint = GlassTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Slider(
                                    value = musicState.volume,
                                    onValueChange = { viewModel.setMusicVolume(it) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.85f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    )
                                )
                                GlassCircleButton(
                                    icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = "Música",
                                    onClick = { viewModel.toggleMusicPlayback() }
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Siguiente canción",
                                    onClick = { viewModel.skipNextTrack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (immersive && !showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (isTV) "Pulsa OK para controles" else "Toca para controles",
                    color = Color.White.copy(alpha = 0.35f),
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
                if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicOff,
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
    muteAudio: Boolean,
    onVideoEnded: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(uri, muteAudio) {
        exoPlayer.setMediaItem(ExoMediaItem.fromUri(uri))
        exoPlayer.volume = if (muteAudio) 0f else 1f
        exoPlayer.prepare()
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
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
                player = exoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
