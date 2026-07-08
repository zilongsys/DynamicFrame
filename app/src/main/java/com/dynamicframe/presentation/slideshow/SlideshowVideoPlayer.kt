package com.dynamicframe.presentation.slideshow

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository

@Composable
fun SlideshowVideoPlayer(
    videoPlayer: SlideshowVideoPlayerRepository,
    uri: String,
    isPlaying: Boolean,
    mediaVolume: Float,
    muteAudio: Boolean,
    onVideoEnded: () -> Unit,
    onPlaybackError: () -> Unit = onVideoEnded,
    playToken: Int = 0,
    backdropPlayer: VideoBackdropPlayerRepository? = null,
    modifier: Modifier = Modifier
) {
    // Referencia a la PlayerView para forzar un re-layout cuando se conoce el
    // tamaño real del vídeo (corrige el vídeo "distorsionado" del primer frame).
    val playerViewHolder = remember { arrayOfNulls<PlayerView>(1) }

    // Oculta el reproductor hasta el primer frame real: evita el flash negro
    // cuando este composable se monta por primera vez (imagen→vídeo).
    // Entre vídeos (vídeo→vídeo) el composable NO se recrea, por lo que la
    // última imagen del vídeo anterior permanece visible gracias a
    // keepContentOnPlayerReset y este flag ya vale true.
    var firstFrameReady by remember { mutableStateOf(false) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrameReady) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "video-fade-in"
    )

    // Callbacks siempre actualizados: el listener se registra UNA vez (estable) y
    // así nunca pierde el evento STATE_ENDED por re-registros en cada recomposición.
    val currentOnEnded by rememberUpdatedState(onVideoEnded)
    val currentOnError by rememberUpdatedState(onPlaybackError)
    val currentUri by rememberUpdatedState(uri)
    val currentBackdrop by rememberUpdatedState(backdropPlayer)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentMediaVolume by rememberUpdatedState(mediaVolume)
    val currentMuteAudio by rememberUpdatedState(muteAudio)

    // Preparar al cambiar la URI o el playToken. isPlaying se lee con rememberUpdatedState
    // para no capturar false obsoleto (p. ej. tras beginSession startPaused) y pausar al final.
    LaunchedEffect(uri, playToken) {
        videoPlayer.prepare(uri, currentMediaVolume, currentMuteAudio, currentIsPlaying)
    }

    // Play/pausa sin reiniciar el vídeo.
    LaunchedEffect(isPlaying) {
        videoPlayer.setPlaying(isPlaying)
    }

    // Volumen/mute sin reiniciar el vídeo.
    LaunchedEffect(mediaVolume, muteAudio) {
        videoPlayer.setVolume(mediaVolume, muteAudio)
    }

    // Único listener estable: fin de vídeo, errores y tamaño/primer frame.
    DisposableEffect(videoPlayer) {
        val p = videoPlayer.player
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) currentOnEnded()
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnError()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                playerViewHolder[0]?.apply {
                    requestLayout()
                    invalidate()
                }
            }

            override fun onRenderedFirstFrame() {
                firstFrameReady = true
                playerViewHolder[0]?.apply {
                    requestLayout()
                    invalidate()
                }
            }
        }
        p.addListener(listener)
        onDispose {
            p.removeListener(listener)
            // Detener vídeo principal y backdrop blur en el mismo ciclo de vida.
            videoPlayer.stopIfCurrent(currentUri)
            currentBackdrop?.stopIfCurrent(currentUri)
        }
    }

    AndroidView(
        factory = { ctx ->
            // SurfaceView por defecto: recomendado para vídeo en Android TV y sin el
            // problema de pantalla negra al reanudar que tiene TextureView.
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                // Mantener el último frame al re-preparar (evita parpadeo a negro).
                setKeepContentOnPlayerReset(true)
                // Shutter transparente: el fondo del letterbox se ve mientras carga
                // el primer frame, evitando el flash negro al cambiar de vídeo.
                setShutterBackgroundColor(Color.Transparent.toArgb())
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                player = videoPlayer.player
                playerViewHolder[0] = this
            }
        },
        update = { playerView ->
            playerView.player = videoPlayer.player
            playerViewHolder[0] = playerView
        },
        onRelease = { playerView ->
            playerView.player = null
            playerViewHolder[0] = null
        },
        modifier = modifier.graphicsLayer { alpha = videoAlpha }
    )
}
