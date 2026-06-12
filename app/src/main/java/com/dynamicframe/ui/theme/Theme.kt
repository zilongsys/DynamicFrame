package com.dynamicframe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dynamicframe.presentation.device.typographyForDevice

// Paleta nostálgica — rosa suave, crema y ciruela
val NostalgiaBackground = Color(0xFFFFF5F7)
val NostalgiaSurface = Color(0xFFFFFBFC)
val NostalgiaCard = Color(0xFFFFE8EF)
val NostalgiaInk = Color(0xFF4A2C3D)
val NostalgiaMuted = Color(0xFF9A7082)
val NostalgiaLine = Color(0xFFF5C6D6)
val NostalgiaAccent = Color(0xFFD4738F)
val NostalgiaAccentDeep = Color(0xFFB85C78)
val NostalgiaSelected = Color(0xFFFFD9E6)
val NostalgiaFocus = Color(0xFFC75B7A)
val NostalgiaGlow = Color(0xFFFFB8D0)

// Alias para compatibilidad con pantallas existentes
val PaperBackground = NostalgiaBackground
val PaperSurface = NostalgiaSurface
val PaperInk = NostalgiaInk
val PaperMuted = NostalgiaMuted
val PaperLine = NostalgiaLine
val PaperAccent = NostalgiaAccentDeep
val PaperSelected = NostalgiaSelected

private val NostalgiaColorScheme = lightColorScheme(
    primary = NostalgiaAccentDeep,
    onPrimary = Color.White,
    secondary = NostalgiaAccent,
    onSecondary = Color.White,
    background = NostalgiaBackground,
    onBackground = NostalgiaInk,
    surface = NostalgiaSurface,
    onSurface = NostalgiaInk,
    surfaceVariant = NostalgiaCard,
    onSurfaceVariant = NostalgiaMuted,
    outline = NostalgiaLine,
    error = Color(0xFFC62828),
    onError = Color.White
)

@Composable
fun DynamicFrameTheme(
    isTv: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NostalgiaColorScheme,
        typography = typographyForDevice(isTv),
        content = content
    )
}
