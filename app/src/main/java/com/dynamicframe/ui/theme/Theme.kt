package com.dynamicframe.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.dynamicframe.presentation.device.typographyForDevice

// Paleta MEMORIA — crema + morado (alias Nostalgia* para compatibilidad)
val NostalgiaBackground = MemoriaBg
val NostalgiaSurface = MemoriaSurface
val NostalgiaCard = MemoriaPurpleSoft
val NostalgiaInk = MemoriaInk
val NostalgiaMuted = MemoriaMuted
val NostalgiaLine = MemoriaLine
val NostalgiaAccent = MemoriaPurple
val NostalgiaAccentDeep = MemoriaPurpleDark
val NostalgiaSelected = MemoriaPurpleSoft
val NostalgiaFocus = MemoriaPurple
val NostalgiaGlow = MemoriaPurpleSoft

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFrameTheme(
    isTv: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NostalgiaColorScheme,
        typography = typographyForDevice(isTv)
    ) {
        // Evita crash PlatformRipple en TV box al componer Slider/ProgressIndicator.
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            content()
        }
    }
}
