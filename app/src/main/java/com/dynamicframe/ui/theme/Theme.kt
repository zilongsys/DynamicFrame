package com.dynamicframe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dynamicframe.presentation.device.typographyForDevice

val PaperBackground = Color(0xFFF3F0EA)
val PaperSurface = Color(0xFFFAF8F4)
val PaperInk = Color(0xFF1A1A1A)
val PaperMuted = Color(0xFF6B6560)
val PaperLine = Color(0xFFD8D2C8)
val PaperAccent = Color(0xFF2C2C2C)
val PaperSelected = Color(0xFFFFFFFF)

private val EditorialColorScheme = lightColorScheme(
    primary = PaperInk,
    onPrimary = PaperSurface,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = Color(0xFFE8E4DC),
    onSurfaceVariant = PaperMuted,
    outline = PaperLine
)

@Composable
fun DynamicFrameTheme(
    isTv: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EditorialColorScheme,
        typography = typographyForDevice(isTv),
        content = content
    )
}
