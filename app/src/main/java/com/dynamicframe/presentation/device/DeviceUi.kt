package com.dynamicframe.presentation.device

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.BuildConfig
import com.dynamicframe.ui.theme.PaperInk
import com.dynamicframe.ui.theme.PaperMuted

data class DeviceUiProfile(
    val isTv: Boolean,
    val showNavLabels: Boolean,
    val useSidebarNav: Boolean,
    val navIconSize: Dp,
    val navLabelSize: androidx.compose.ui.unit.TextUnit,
    val actionButtonSize: Dp,
    val thumbnailWidth: Dp,
    val thumbnailHeight: Dp,
    val clockLargeSp: androidx.compose.ui.unit.TextUnit,
    val clockCompactSp: androidx.compose.ui.unit.TextUnit,
    val contentPaddingH: Dp,
    val contentPaddingV: Dp,
    val sidebarWidth: Dp
)

fun DeviceUiProfile.scaled(scale: Float): DeviceUiProfile {
    if (scale == 1f) return this
    fun Dp.s() = (value * scale).dp
    fun androidx.compose.ui.unit.TextUnit.s() = (value * scale).sp
    return copy(
        navIconSize = navIconSize.s(),
        navLabelSize = navLabelSize.s(),
        actionButtonSize = actionButtonSize.s(),
        thumbnailWidth = thumbnailWidth.s(),
        thumbnailHeight = thumbnailHeight.s(),
        clockLargeSp = clockLargeSp.s(),
        clockCompactSp = clockCompactSp.s(),
        contentPaddingH = contentPaddingH.s(),
        contentPaddingV = contentPaddingV.s(),
        sidebarWidth = sidebarWidth.s()
    )
}

fun deviceUiProfile(isTv: Boolean): DeviceUiProfile = if (isTv) {
    DeviceUiProfile(
        isTv = true,
        showNavLabels = true,
        useSidebarNav = true,
        navIconSize = 26.dp,
        navLabelSize = 17.sp,
        actionButtonSize = 52.dp,
        thumbnailWidth = 160.dp,
        thumbnailHeight = 112.dp,
        clockLargeSp = 80.sp,
        clockCompactSp = 56.sp,
        contentPaddingH = 32.dp,
        contentPaddingV = 28.dp,
        sidebarWidth = 248.dp
    )
} else {
    DeviceUiProfile(
        isTv = false,
        showNavLabels = true,
        useSidebarNav = false,
        navIconSize = 24.dp,
        navLabelSize = 11.sp,
        actionButtonSize = 44.dp,
        thumbnailWidth = 108.dp,
        thumbnailHeight = 76.dp,
        clockLargeSp = 52.sp,
        clockCompactSp = 36.sp,
        contentPaddingH = 16.dp,
        contentPaddingV = 12.dp,
        sidebarWidth = 0.dp
    )
}

val LocalDeviceProfile = staticCompositionLocalOf { deviceUiProfile(isTv = false) }

@Composable
fun rememberDeviceProfile(fallbackIsTv: Boolean): DeviceUiProfile {
    val configuration = LocalConfiguration.current
    val runtimeTv = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    return deviceUiProfile(runtimeTv || fallbackIsTv || BuildConfig.IS_TV)
}

@Composable
fun ProvideDeviceProfile(
    isTv: Boolean,
    content: @Composable () -> Unit
) {
    val profile = rememberDeviceProfile(isTv)
    CompositionLocalProvider(LocalDeviceProfile provides profile, content = content)
}

fun typographyForDevice(isTv: Boolean): Typography {
    val clockLarge = if (isTv) 80.sp else 52.sp
    val title = if (isTv) 18.sp else 16.sp
    val body = if (isTv) 15.sp else 14.sp
    val label = if (isTv) 12.sp else 11.sp
    return Typography(
        headlineLarge = TextStyle(
            fontSize = clockLarge,
            fontWeight = FontWeight.Light,
            color = PaperInk,
            letterSpacing = (-2).sp
        ),
        titleMedium = TextStyle(
            fontSize = title,
            fontWeight = FontWeight.Medium,
            color = PaperInk
        ),
        bodyMedium = TextStyle(
            fontSize = body,
            color = PaperMuted
        ),
        labelSmall = TextStyle(
            fontSize = label,
            fontWeight = FontWeight.Medium,
            color = PaperMuted,
            letterSpacing = if (isTv) 1.2.sp else 0.5.sp
        )
    )
}

enum class HomeSection {
    SLIDESHOW, ALBUMS, MUSIC, SETTINGS;

    val icon: ImageVector
        get() = when (this) {
            SLIDESHOW -> Icons.Default.Photo
            ALBUMS -> Icons.Default.PhotoLibrary
            MUSIC -> Icons.Default.MusicNote
            SETTINGS -> Icons.Default.Settings
        }
}

/** Etiquetas cortas para móvil, completas para TV. */
fun HomeSection.navLabel(isTv: Boolean): String = when (this) {
    HomeSection.SLIDESHOW -> if (isTv) "Slideshow" else "Inicio"
    HomeSection.ALBUMS -> "Álbumes"
    HomeSection.MUSIC -> "Música"
    HomeSection.SETTINGS -> "Ajustes"
}
