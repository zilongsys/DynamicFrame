package com.dynamicframe.presentation.slideshow

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository

@Composable
fun SlideshowVideoPlayer(
    videoPlayer: SlideshowVideoPlayerRepository,
    uri: String,
    isPlaying: Boolean,
    mediaVolume: Float,
    muteAudio: Boolean,
    onVideoEnded: () -> Unit,
    onPlaybackError: () -> Unit = onVideoEnded,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uri, muteAudio, mediaVolume, isPlaying) {
        videoPlayer.prepare(uri, mediaVolume, muteAudio, isPlaying)
    }

    DisposableEffect(videoPlayer, onVideoEnded, onPlaybackError) {
        videoPlayer.setListeners(onEnded = onVideoEnded, onError = onPlaybackError)
        onDispose {
            videoPlayer.clearListeners()
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
            playerView.player = videoPlayer.player
        },
        modifier = modifier
    )
}
