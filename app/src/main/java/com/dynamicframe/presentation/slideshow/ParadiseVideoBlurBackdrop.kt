package com.dynamicframe.presentation.slideshow

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.ui.components.AppBlurFillImage

/**
 * Capa 1 Paradise para vídeo: segundo PlayerView (crop + blur en API 31+)
 * o thumbnail con [AppBlurFillImage] en API anteriores.
 */
@Composable
fun ParadiseVideoBlurBackdrop(
    uri: String,
    isPlaying: Boolean,
    playToken: Int,
    backdropPlayer: VideoBackdropPlayerRepository,
    fallbackThumbnailUri: String?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uri, playToken) {
        backdropPlayer.prepare(uri, isPlaying)
    }
    LaunchedEffect(isPlaying) {
        backdropPlayer.setPlaying(isPlaying)
    }

    DisposableEffect(uri) {
        onDispose {
            backdropPlayer.stopIfCurrent(uri)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(Color.Transparent.toArgb())
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = backdropPlayer.player
                    setRenderEffect(
                        RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                    )
                }
            },
            update = { playerView ->
                playerView.player = backdropPlayer.player
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = modifier
        )
    } else {
        val thumbUri = fallbackThumbnailUri ?: uri
        AppBlurFillImage(
            uri = thumbUri,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
