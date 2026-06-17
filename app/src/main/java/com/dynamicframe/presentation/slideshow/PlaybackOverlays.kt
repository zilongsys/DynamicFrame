package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.FocusHintEffect
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.NostalgiaAccent
import com.dynamicframe.ui.theme.NostalgiaFocus
import com.dynamicframe.ui.theme.safeClickable

/** Borde rosa de zona segura en pantalla completa (guía overscan). */
@Composable
fun PlaybackSafeBorder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 4.dp, color = NostalgiaFocus, shape = RectangleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = NostalgiaAccent.copy(alpha = 0.55f), shape = RectangleShape)
        )
    }
}

@Composable
fun CenterPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 88.dp,
    onFocusChanged: (Boolean) -> Unit = {},
    hintDescription: String = if (isPlaying) "Pausar reproducción" else "Reanudar reproducción",
    onFocusHint: ((String) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") filled: Boolean = false
) {
    MediaCircleButton(
        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
        onClick = onClick,
        modifier = modifier,
        size = buttonSize,
        onFocusChanged = onFocusChanged,
        hintDescription = hintDescription,
        onFocusHint = onFocusHint
    )
}

@Composable
fun MediaCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onFocusChanged: (Boolean) -> Unit = {},
    hintDescription: String = contentDescription,
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})
    val iconSize = (size.value * 0.46f).dp

    val bg = when {
        focused -> MemoriaPurple
        else -> Color.White.copy(alpha = 0.14f)
    }
    val iconTint = if (focused) Color.White else Color.White.copy(alpha = 0.92f)

    Box(
        modifier = modifier
            .size(size)
            .scale(if (focused && device.isTv) 1.08f else 1f)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .clip(CircleShape)
            .background(bg)
            .then(
                if (!focused) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                } else Modifier
            )
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interactionSource
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}
