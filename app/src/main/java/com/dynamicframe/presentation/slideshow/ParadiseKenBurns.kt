package com.dynamicframe.presentation.slideshow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

/**
 * Ken Burns en la capa sharp Paradise (solo fotos). El blur de fondo queda estático.
 */
@Composable
fun Modifier.paradiseKenBurns(
    enabled: Boolean,
    photoKey: String,
): Modifier {
    if (!enabled) return this

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = configuration.screenWidthDp * density.density
    val screenHeightPx = configuration.screenHeightDp * density.density
    val direction = remember(photoKey) { Random.nextInt(4) }

    val infiniteTransition = rememberInfiniteTransition(label = "kenBurns_$photoKey")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kenBurnsScale",
    )

    val translateX = when (direction) {
        0 -> kenBurnsScale * 0.01f * screenWidthPx
        1 -> -kenBurnsScale * 0.01f * screenWidthPx
        else -> 0f
    }
    val translateY = when (direction) {
        2 -> kenBurnsScale * 0.01f * screenHeightPx
        3 -> -kenBurnsScale * 0.01f * screenHeightPx
        else -> 0f
    }

    return graphicsLayer {
        scaleX = kenBurnsScale
        scaleY = kenBurnsScale
        translationX = translateX
        translationY = translateY
    }
}
