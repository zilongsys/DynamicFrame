package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.PlaybackTheme
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.FocusHintEffect
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.safeClickable
import com.dynamicframe.ui.theme.tvFocusRequester
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Acciones de la pantalla de reproducción, compartidas por los 3 temas de interfaz.
 * Se agrupan para evitar una explosión de parámetros en cada composable de tema.
 */
class PlaybackControlsCallbacks(
    val onBack: (() -> Unit)?,
    val onDelete: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onRestart: () -> Unit,
    val onToggleMusic: () -> Unit,
    val onSkipTrack: () -> Unit,
    val onMusicVolume: (Float) -> Unit,
    val onMediaVolume: (Float) -> Unit,
    val onSelectAlbum: (String?) -> Unit,
    val setControlHint: (String) -> Unit,
    val onPauseFocusChanged: (Boolean) -> Unit
)

/** Dispatcher: elige el overlay de controles según el tema configurado. */
@Composable
fun PlaybackControlsOverlay(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    pills: List<AlbumPillOption>,
    selectedAlbumId: String?,
    isTV: Boolean,
    controlHint: String,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    expanded: Boolean,
    cb: PlaybackControlsCallbacks
) {
    when (config.playbackTheme) {
        PlaybackTheme.AMBIENT ->
            AmbientControls(slideshowState, isTV, pauseFocus, bottomBarFocus, cb)
        PlaybackTheme.GALLERY ->
            GalleryControls(slideshowState, config, musicState, isTV, pauseFocus, cb)
        PlaybackTheme.PARADISE, PlaybackTheme.AURORA_GLASS ->
            AuroraGlassControls(
                slideshowState, config, musicState, pills, selectedAlbumId,
                isTV, controlHint, pauseFocus, bottomBarFocus, cb
            )
    }
}

// ── Tema C · Aurora Glass (HUD cyan glassmorphism — mockup) ────────────────────

@Composable
private fun AuroraGlassControls(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    pills: List<AlbumPillOption>,
    selectedAlbumId: String?,
    isTV: Boolean,
    controlHint: String,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Barra superior: chip de música (izq) + acciones circulares (der)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (musicState.currentTrack != null && config.playbackShowOverlay) {
                AuroraMusicChip(
                    title = musicState.currentTrack!!.title,
                    artist = musicState.currentTrack!!.artist,
                    isPlaying = musicState.isPlaying
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cb.onBack?.let {
                    AuroraHudIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        hintDescription = "Volver al panel principal",
                        onFocusHint = cb.setControlHint,
                        onClick = it
                    )
                }
                if (slideshowState.currentItem != null) {
                    AuroraHudIconButton(
                        icon = Icons.Default.DeleteOutline,
                        contentDescription = "Borrar",
                        hintDescription = "Borrar la foto o vídeo actual",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onDelete
                    )
                }
                AuroraHudIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    hintDescription = "Abrir ajustes de la app",
                    onFocusHint = cb.setControlHint,
                    onClick = cb.onOpenSettings
                )
            }
        }

        if (config.playbackShowOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Play/pausa centrado justo encima del HUD (mockup).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AuroraCenterPlayButton(
                        isPlaying = slideshowState.isPlaying,
                        onClick = cb.onPlayPause,
                        modifier = pauseDpadModifier(isTV, pauseFocus, bottomBarFocus),
                        onFocusChanged = cb.onPauseFocusChanged,
                        hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                        onFocusHint = cb.setControlHint
                    )
                }

                AuroraGlassHud(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                if (pills.size > 1) {
                    Text(
                        text = "Álbumes",
                        color = AuroraTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    AuroraAlbumPillRow(
                        pills = pills,
                        selectedId = selectedAlbumId,
                        onSelect = cb.onSelectAlbum
                    )
                }

                if (slideshowState.totalItems > 0) {
                    val slideProgress =
                        (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                    AuroraProgressRow(
                        progress = slideProgress,
                        label = "${slideshowState.currentIndex + 1} / ${slideshowState.totalItems}"
                    )
                }

                val showVideoVolume = !config.muteVideoAudio
                val transportSize = 42.dp
                val transportIcon = 22.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AuroraTransportButton(
                        icon = Icons.Default.Replay,
                        contentDescription = "Reiniciar",
                        hintDescription = "Reiniciar desde el principio",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onRestart,
                        buttonSize = transportSize,
                        iconSize = transportIcon
                    )
                    AuroraTransportButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        hintDescription = "Foto o vídeo anterior",
                        onFocusHint = cb.setControlHint,
                        modifier = if (isTV) Modifier.focusRequester(bottomBarFocus) else Modifier,
                        onClick = cb.onPrevious,
                        buttonSize = transportSize,
                        iconSize = transportIcon
                    )
                    AuroraTransportButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        hintDescription = "Foto o vídeo siguiente",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onNext,
                        buttonSize = transportSize,
                        iconSize = transportIcon
                    )
                    AuroraGlassVolumeSlider(
                        value = config.musicVolume,
                        onValueChange = cb.onMusicVolume,
                        icon = Icons.Default.MusicNote,
                        decreaseHintDescription = "Bajar volumen de la música",
                        increaseHintDescription = "Subir volumen de la música",
                        onFocusHint = cb.setControlHint,
                        compact = true,
                        inlineTrackWidth = 92.dp
                    )
                    if (showVideoVolume) {
                        AuroraGlassVolumeSlider(
                            value = config.mediaVolume,
                            onValueChange = cb.onMediaVolume,
                            icon = Icons.Default.Movie,
                            decreaseHintDescription = "Bajar volumen del vídeo",
                            increaseHintDescription = "Subir volumen del vídeo",
                            onFocusHint = cb.setControlHint,
                            compact = true,
                            inlineTrackWidth = 92.dp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AuroraTransportButton(
                        icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        contentDescription = "Música",
                        hintDescription = if (musicState.isPlaying) "Pausar música" else "Reanudar música",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onToggleMusic,
                        buttonSize = transportSize,
                        iconSize = transportIcon
                    )
                    AuroraTransportButton(
                        icon = Icons.Default.FastForward,
                        contentDescription = "Siguiente canción",
                        hintDescription = "Saltar a la siguiente pista",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onSkipTrack,
                        buttonSize = transportSize,
                        iconSize = transportIcon
                    )
                }

                if (isTV) {
                    Text(
                        text = controlHint,
                        color = AuroraTextMuted.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
                }
            }
        } else {
            // Sin HUD: play centrado en la parte inferior (solo pausa/reanudar).
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                AuroraCenterPlayButton(
                    isPlaying = slideshowState.isPlaying,
                    onClick = cb.onPlayPause,
                    modifier = pauseDpadModifier(isTV, pauseFocus, bottomBarFocus),
                    onFocusChanged = cb.onPauseFocusChanged,
                    hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                    onFocusHint = cb.setControlHint
                )
            }
        }
    }
}

// ── Tema A · Ambient (minimalismo: glifos planos, sin glass) ───────────────────

private val AmbientGlow = MemoriaPurple

@Composable
private fun AmbientControls(
    slideshowState: SlideshowState,
    isTV: Boolean,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Acciones planas y discretas arriba a la derecha (sin cajas/glass).
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            cb.onBack?.let {
                AmbientFlatIcon(Icons.Default.ArrowBack, "Volver", "Volver al panel principal", cb.setControlHint, it)
            }
            if (slideshowState.currentItem != null) {
                AmbientFlatIcon(Icons.Default.DeleteOutline, "Borrar", "Borrar la foto o vídeo actual", cb.setControlHint, cb.onDelete)
            }
            AmbientFlatIcon(Icons.Default.Settings, "Ajustes", "Abrir ajustes de la app", cb.setControlHint, cb.onOpenSettings)
        }

        // Scrim degradado inferior para legibilidad (sin paneles).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (slideshowState.totalItems > 0) {
                val slideProgress =
                    (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                LinearProgressIndicator(
                    progress = slideProgress,
                    modifier = Modifier
                        .fillMaxWidth(0.32f)
                        .padding(bottom = 22.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmbientFlatIcon(
                    Icons.Default.SkipPrevious, "Anterior", "Foto o vídeo anterior", cb.setControlHint, cb.onPrevious,
                    size = 56.dp, iconSize = 34.dp,
                    modifier = if (isTV) Modifier.focusRequester(bottomBarFocus) else Modifier
                )
                AmbientPlayButton(
                    isPlaying = slideshowState.isPlaying,
                    isTV = isTV,
                    pauseFocus = pauseFocus,
                    bottomBarFocus = bottomBarFocus,
                    onFocusChanged = cb.onPauseFocusChanged,
                    onFocusHint = cb.setControlHint,
                    onClick = cb.onPlayPause
                )
                AmbientFlatIcon(
                    Icons.Default.SkipNext, "Siguiente", "Foto o vídeo siguiente", cb.setControlHint, cb.onNext,
                    size = 56.dp, iconSize = 34.dp
                )
            }
        }
    }
}

@Composable
private fun AmbientFlatIcon(
    icon: ImageVector,
    contentDescription: String,
    hint: String,
    onFocusHint: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hint, onHint = onFocusHint)
    Box(
        modifier = modifier
            .size(size)
            .scale(if (focused && device.isTv) 1.2f else 1f)
            .clip(CircleShape)
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.16f)) else Modifier)
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interaction
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = if (focused) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun AmbientPlayButton(
    isPlaying: Boolean,
    isTV: Boolean,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onFocusHint: (String) -> Unit,
    onClick: () -> Unit
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(
        focused = focused,
        description = if (isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
        onHint = onFocusHint
    )
    Box(
        modifier = pauseDpadModifier(isTV, pauseFocus, bottomBarFocus)
            .size(108.dp)
            .scale(if (focused && device.isTv) 1.06f else 1f)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AmbientGlow.copy(alpha = if (focused) 0.95f else 0.5f),
                        Color.Transparent
                    )
                )
            )
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interaction
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

// ── Tema B · Gallery (museo: riel bronce/oro + placa de pergamino) ─────────────

private val GalleryBronze = Color(0xCC5C461F)
private val GalleryGoldBorder = Color(0xFFD9B25A)
private val GalleryGoldBright = Color(0xFFE8C877)
private val GalleryInk = Color(0xFF2A2010)
private val GalleryParchment = Color(0xFFEFE6CF)

@Composable
private fun GalleryControls(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    isTV: Boolean,
    pauseFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    val item = slideshowState.currentItem
    Box(modifier = Modifier.fillMaxSize()) {
        // Placa de datos tipo museo (pergamino), centrada abajo.
        if (item != null && config.playbackShowOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .widthIn(min = 240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GalleryParchment)
                    .border(1.dp, GalleryGoldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 30.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                formatItemDate(item.dateAdded)?.let { date ->
                    Text(
                        text = date,
                        color = GalleryInk,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                val subtitle = item.albumName.ifBlank { item.name }.ifBlank { "Recuerdos" }
                Text(
                    text = subtitle,
                    color = GalleryInk.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                if (slideshowState.totalItems > 0) {
                    Text(
                        text = "${slideshowState.currentIndex + 1} / ${slideshowState.totalItems}",
                        color = GalleryInk.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Riel vertical de controles bronce/oro acoplado a la derecha.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GalleryRailButton(
                icon = if (slideshowState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (slideshowState.isPlaying) "Pausar" else "Reproducir",
                hint = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                onFocusHint = cb.setControlHint,
                onClick = cb.onPlayPause,
                size = 72.dp,
                onFocusChanged = cb.onPauseFocusChanged,
                modifier = if (isTV) Modifier.tvFocusRequester(pauseFocus) else Modifier
            )
            GalleryRailButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                hint = "Foto o vídeo anterior",
                onFocusHint = cb.setControlHint,
                onClick = cb.onPrevious
            )
            GalleryRailButton(
                icon = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                hint = "Foto o vídeo siguiente",
                onFocusHint = cb.setControlHint,
                onClick = cb.onNext
            )
            GalleryRailButton(
                icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = "Música",
                hint = if (musicState.isPlaying) "Pausar música" else "Reanudar música",
                onFocusHint = cb.setControlHint,
                onClick = cb.onToggleMusic
            )
            GalleryRailButton(
                icon = Icons.Default.Settings,
                contentDescription = "Ajustes",
                hint = "Abrir ajustes de la app",
                onFocusHint = cb.setControlHint,
                onClick = cb.onOpenSettings
            )
            cb.onBack?.let {
                GalleryRailButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    hint = "Volver al panel principal",
                    onFocusHint = cb.setControlHint,
                    onClick = it
                )
            }
        }
    }
}

@Composable
private fun GalleryRailButton(
    icon: ImageVector,
    contentDescription: String,
    hint: String,
    onFocusHint: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hint, onHint = onFocusHint)
    Box(
        modifier = modifier
            .size(size)
            .scale(if (focused && device.isTv) 1.1f else 1f)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .clip(CircleShape)
            .background(if (focused) GalleryGoldBright else GalleryBronze)
            .border(1.5.dp, GalleryGoldBorder, CircleShape)
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interaction
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = if (focused) GalleryInk else GalleryGoldBright,
            modifier = Modifier.size((size.value * 0.42f).dp)
        )
    }
}

// ── Helpers compartidos ───────────────────────────────────────────────────────

/** Modificador de foco D-pad para el botón central: enfocable + bajar al transporte. */
private fun pauseDpadModifier(
    isTV: Boolean,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester
): Modifier = if (isTV) {
    Modifier
        .tvFocusRequester(pauseFocus)
        .focusProperties { down = bottomBarFocus }
} else {
    Modifier
}

/** Fecha legible a partir de `dateAdded` (segundos epoch de MediaStore). Null si no hay dato. */
private fun formatItemDate(dateAdded: Long): String? {
    if (dateAdded <= 0L) return null
    return runCatching {
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.getDefault()).format(Date(dateAdded * 1000))
    }.getOrNull()
}
