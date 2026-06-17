package com.dynamicframe.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.dynamicframe.domain.model.PlaybackBackgroundType
import com.dynamicframe.ui.components.AppAsyncImage

object PlaybackBackgrounds {
    val demoLavender = Brush.linearGradient(
        colors = listOf(Color(0xFF5E3BA8), Color(0xFF9B7FD4), Color(0xFFF0EBF8))
    )
    val demoSunset = Brush.linearGradient(
        colors = listOf(Color(0xFF3D2066), Color(0xFF7B4FD4), Color(0xFFE8A87C))
    )
    val demoMidnight = Brush.linearGradient(
        colors = listOf(Color(0xFF0A0A10), Color(0xFF1A1040), Color(0xFF3D2A6E))
    )

    fun brushFor(type: PlaybackBackgroundType): Brush? = when (type) {
        PlaybackBackgroundType.DEMO_LAVENDER -> demoLavender
        PlaybackBackgroundType.DEMO_SUNSET -> demoSunset
        PlaybackBackgroundType.DEMO_MIDNIGHT -> demoMidnight
        else -> null
    }
}

@Composable
fun PlaybackLetterboxBackground(
    type: PlaybackBackgroundType,
    customImageUri: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (type) {
            PlaybackBackgroundType.BLACK -> {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            PlaybackBackgroundType.CUSTOM_IMAGE -> {
                if (customImageUri.isNotBlank()) {
                    AppAsyncImage(
                        uri = customImageUri,
                        crossfadeMillis = 300,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }
            }
            else -> {
                PlaybackBackgrounds.brushFor(type)?.let { brush ->
                    Box(Modifier.fillMaxSize().background(brush))
                } ?: Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}

fun PlaybackBackgroundType.displayName(): String = when (this) {
    PlaybackBackgroundType.BLACK -> "Negro"
    PlaybackBackgroundType.DEMO_LAVENDER -> "Lavanda MEMORIA"
    PlaybackBackgroundType.DEMO_SUNSET -> "Atardecer"
    PlaybackBackgroundType.DEMO_MIDNIGHT -> "Medianoche"
    PlaybackBackgroundType.CUSTOM_IMAGE -> "Mi imagen"
}
