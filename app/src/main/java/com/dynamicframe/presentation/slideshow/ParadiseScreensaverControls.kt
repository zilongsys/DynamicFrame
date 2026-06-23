package com.dynamicframe.presentation.slideshow

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState

/** Controles flotantes Paradise — pill inferior, auto-ocultos (música: pause, skip, volumen). */
@Composable
fun ParadiseScreensaverControls(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    onPlayPause: () -> Unit,
    onSkipTrack: () -> Unit,
    onMusicVolume: (Float) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    /** En TV: FocusRequester del primer botón. El LaunchedEffect de SlideshowScreen llama
     *  requestFocusWhenReady() sobre él cuando el pill aparece, permitiendo la navegación D-pad. */
    firstButtonFocus: FocusRequester? = null,
) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .paradiseControlsPillSurface(shape)
                // focusGroup permite que el D-pad entre en la fila y navegue entre botones.
                .focusGroup()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = {
                    onInteraction()
                    onPlayPause()
                },
                modifier = Modifier
                    .size(40.dp)
                    .then(if (firstButtonFocus != null) Modifier.focusRequester(firstButtonFocus) else Modifier),
            ) {
                Icon(
                    imageVector = if (slideshowState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (slideshowState.isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = {
                    onInteraction()
                    onSkipTrack()
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Siguiente canción",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
            Slider(
                value = config.musicVolume,
                onValueChange = {
                    onInteraction()
                    onMusicVolume(it)
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White.copy(alpha = 0.92f),
                    activeTrackColor = Color.White.copy(alpha = 0.45f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                ),
            )
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun Modifier.paradiseControlsPillSurface(shape: RoundedCornerShape): Modifier {
    val surface = clip(shape).background(Color.Black.copy(alpha = 0.55f), shape)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        surface.graphicsLayer {
            renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
        }
    } else {
        surface
    }
}
