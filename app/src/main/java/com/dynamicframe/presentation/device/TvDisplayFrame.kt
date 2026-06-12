package com.dynamicframe.presentation.device

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Marco de pantalla TV: borde en el borde físico (overscan) y zoom vía densidad
 * (no rompe el foco D-pad como graphicsLayer).
 */
@Composable
fun TvDisplayFrame(
    isTv: Boolean,
    uiScale: Float,
    showScreenBorder: Boolean,
    content: @Composable () -> Unit
) {
    if (!isTv) {
        content()
        return
    }

    val scale = uiScale.coerceIn(0.75f, 1.25f)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * scale,
        fontScale = baseDensity.fontScale * scale
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }

        if (showScreenBorder) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 5.dp, color = Color(0xFFE53935), shape = RectangleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 1.dp, color = Color(0xFF1E88E5), shape = RectangleShape)
            )
        }
    }
}
