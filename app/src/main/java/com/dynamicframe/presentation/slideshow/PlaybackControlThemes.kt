package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.GlassAlbumPillRow
import com.dynamicframe.ui.theme.GlassCircleButton
import com.dynamicframe.ui.theme.GlassIconButton
import com.dynamicframe.ui.theme.GlassSurface
import com.dynamicframe.ui.theme.GlassText
import com.dynamicframe.ui.theme.GlassTextMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.TvVolumeStepper
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
    pills: List<Pair<String?, String>>,
    selectedAlbumId: String?,
    isTV: Boolean,
    controlHint: String,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    when (config.playbackTheme) {
        com.dynamicframe.domain.model.PlaybackTheme.AMBIENT ->
            AmbientControls(slideshowState, config, isTV, pauseFocus, bottomBarFocus, cb)
        com.dynamicframe.domain.model.PlaybackTheme.GALLERY ->
            GalleryControls(slideshowState, config, musicState, isTV, pauseFocus, cb)
        else ->
            AuroraGlassControls(
                slideshowState, config, musicState, pills, selectedAlbumId,
                isTV, controlHint, pauseFocus, bottomBarFocus, cb
            )
    }
}

// ── Tema C · Aurora Glass (por defecto) ───────────────────────────────────────

@Composable
private fun AuroraGlassControls(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    pills: List<Pair<String?, String>>,
    selectedAlbumId: String?,
    isTV: Boolean,
    controlHint: String,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    val device = LocalDeviceProfile.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Barra superior: música (izq) + acciones (der)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (musicState.currentTrack != null && config.playbackShowOverlay) {
                PlaybackMusicChip(
                    title = musicState.currentTrack?.title,
                    artist = musicState.currentTrack?.artist,
                    isPlaying = musicState.isPlaying
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cb.onBack?.let {
                    GlassIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        label = if (device.isTv) "Inicio" else "Volver",
                        hintDescription = "Volver al panel principal",
                        onFocusHint = cb.setControlHint,
                        onClick = it
                    )
                }
                if (slideshowState.currentItem != null) {
                    GlassIconButton(
                        icon = Icons.Default.DeleteOutline,
                        contentDescription = "Borrar",
                        label = if (device.isTv) "Borrar" else null,
                        hintDescription = "Borrar la foto o vídeo actual",
                        onFocusHint = cb.setControlHint,
                        onClick = cb.onDelete
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    label = if (device.isTv) "Configuración" else "Ajustes",
                    hintDescription = "Abrir ajustes de la app",
                    onFocusHint = cb.setControlHint,
                    onClick = cb.onOpenSettings,
                    prominent = true
                )
            }
        }

        CenterPlayPauseButton(
            isPlaying = slideshowState.isPlaying,
            onClick = cb.onPlayPause,
            modifier = pauseDpadModifier(isTV, pauseFocus, bottomBarFocus).align(Alignment.Center),
            onFocusChanged = cb.onPauseFocusChanged,
            hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
            onFocusHint = cb.setControlHint
        )

        if (config.playbackShowOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    cornerRadius = 28.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (pills.size > 1) {
                            Text(
                                text = "Álbumes",
                                color = GlassTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            GlassAlbumPillRow(
                                pills = pills,
                                selectedId = selectedAlbumId,
                                onSelect = cb.onSelectAlbum
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        if (slideshowState.totalItems > 0) {
                            val slideProgress =
                                (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                            LinearProgressIndicator(
                                progress = slideProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White.copy(alpha = 0.9f),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${slideshowState.currentIndex + 1} / ${slideshowState.totalItems}",
                                color = GlassTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlassCircleButton(
                                    icon = Icons.Default.Replay,
                                    contentDescription = "Reiniciar reproducción",
                                    hintDescription = "Reiniciar desde el principio (aplica aleatorio si está activo)",
                                    onFocusHint = cb.setControlHint,
                                    onClick = cb.onRestart
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    hintDescription = "Foto o vídeo anterior",
                                    onFocusHint = cb.setControlHint,
                                    modifier = if (isTV) Modifier.focusRequester(bottomBarFocus) else Modifier,
                                    onClick = cb.onPrevious
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    hintDescription = "Foto o vídeo siguiente",
                                    onFocusHint = cb.setControlHint,
                                    onClick = cb.onNext
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            ) {
                                VolumeControl(
                                    isTV = isTV,
                                    icon = Icons.Default.VolumeUp,
                                    label = "Volumen",
                                    value = config.musicVolume,
                                    onValueChange = cb.onMusicVolume,
                                    hint = "Volumen música. ↑ ↓ ajustar · ← → cambiar botón",
                                    setControlHint = cb.setControlHint,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassCircleButton(
                                    icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = "Música",
                                    hintDescription = if (musicState.isPlaying) "Pausar música" else "Reanudar música",
                                    onFocusHint = cb.setControlHint,
                                    onClick = cb.onToggleMusic
                                )
                                GlassCircleButton(
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Siguiente canción",
                                    hintDescription = "Saltar a la siguiente pista",
                                    onFocusHint = cb.setControlHint,
                                    onClick = cb.onSkipTrack
                                )
                            }
                        }

                        val currentForVol = slideshowState.currentItem
                        if (currentForVol?.type == MediaType.VIDEO && !config.muteVideoAudio) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = GlassTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                VolumeControl(
                                    isTV = isTV,
                                    icon = Icons.Default.Movie,
                                    label = "Vol. vídeo",
                                    value = config.mediaVolume,
                                    onValueChange = cb.onMediaVolume,
                                    hint = "Volumen del audio del vídeo. ↑ ↓ ajustar · ← → cambiar botón",
                                    setControlHint = cb.setControlHint,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (isTV) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = controlHint,
                                color = GlassTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tema A · Ambient (minimalismo cinematográfico) ────────────────────────────

@Composable
private fun AmbientControls(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    isTV: Boolean,
    pauseFocus: FocusRequester,
    bottomBarFocus: FocusRequester,
    cb: PlaybackControlsCallbacks
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Acciones discretas arriba a la derecha.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cb.onBack?.let {
                GlassCircleButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    hintDescription = "Volver al panel principal",
                    onFocusHint = cb.setControlHint,
                    onClick = it
                )
            }
            if (slideshowState.currentItem != null) {
                GlassCircleButton(
                    icon = Icons.Default.DeleteOutline,
                    contentDescription = "Borrar",
                    hintDescription = "Borrar la foto o vídeo actual",
                    onFocusHint = cb.setControlHint,
                    onClick = cb.onDelete
                )
            }
            GlassCircleButton(
                icon = Icons.Default.Settings,
                contentDescription = "Ajustes",
                hintDescription = "Abrir ajustes de la app",
                onFocusHint = cb.setControlHint,
                onClick = cb.onOpenSettings
            )
        }

        // Barra inferior mínima sobre un scrim degradado para legibilidad.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (slideshowState.totalItems > 0) {
                val slideProgress =
                    (slideshowState.currentIndex + 1).toFloat() / slideshowState.totalItems
                LinearProgressIndicator(
                    progress = slideProgress,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(bottom = 20.dp),
                    color = MemoriaPurple,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaCircleButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    onClick = cb.onPrevious,
                    size = 64.dp,
                    hintDescription = "Foto o vídeo anterior",
                    onFocusHint = cb.setControlHint,
                    modifier = if (isTV) Modifier.focusRequester(bottomBarFocus) else Modifier
                )
                CenterPlayPauseButton(
                    isPlaying = slideshowState.isPlaying,
                    onClick = cb.onPlayPause,
                    buttonSize = 100.dp,
                    modifier = pauseDpadModifier(isTV, pauseFocus, bottomBarFocus),
                    onFocusChanged = cb.onPauseFocusChanged,
                    hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                    onFocusHint = cb.setControlHint
                )
                MediaCircleButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    onClick = cb.onNext,
                    size = 64.dp,
                    hintDescription = "Foto o vídeo siguiente",
                    onFocusHint = cb.setControlHint
                )
            }
        }
    }
}

// ── Tema B · Gallery (marco editorial / museo) ────────────────────────────────

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
        // Placa de datos tipo museo, centrada abajo.
        if (item != null && config.playbackShowOverlay) {
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .widthIn(min = 260.dp),
                cornerRadius = 14.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    formatItemDate(item.dateAdded)?.let { date ->
                        Text(
                            text = date,
                            color = GlassText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val subtitle = item.albumName.ifBlank { item.name }.ifBlank { "Recuerdos" }
                    Text(
                        text = subtitle,
                        color = GlassTextMuted,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    if (slideshowState.totalItems > 0) {
                        Text(
                            text = "${slideshowState.currentIndex + 1} / ${slideshowState.totalItems}",
                            color = GlassTextMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Riel vertical de controles acoplado a la derecha.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CenterPlayPauseButton(
                isPlaying = slideshowState.isPlaying,
                onClick = cb.onPlayPause,
                buttonSize = 72.dp,
                modifier = if (isTV) Modifier.tvFocusRequester(pauseFocus) else Modifier,
                onFocusChanged = cb.onPauseFocusChanged,
                hintDescription = if (slideshowState.isPlaying) "Pausar fotos y música" else "Reanudar reproducción",
                onFocusHint = cb.setControlHint
            )
            MediaCircleButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                onClick = cb.onPrevious,
                size = 56.dp,
                hintDescription = "Foto o vídeo anterior",
                onFocusHint = cb.setControlHint
            )
            MediaCircleButton(
                icon = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                onClick = cb.onNext,
                size = 56.dp,
                hintDescription = "Foto o vídeo siguiente",
                onFocusHint = cb.setControlHint
            )
            MediaCircleButton(
                icon = if (musicState.isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = "Música",
                onClick = cb.onToggleMusic,
                size = 56.dp,
                hintDescription = if (musicState.isPlaying) "Pausar música" else "Reanudar música",
                onFocusHint = cb.setControlHint
            )
            MediaCircleButton(
                icon = Icons.Default.Settings,
                contentDescription = "Ajustes",
                onClick = cb.onOpenSettings,
                size = 56.dp,
                hintDescription = "Abrir ajustes de la app",
                onFocusHint = cb.setControlHint
            )
            cb.onBack?.let {
                MediaCircleButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    onClick = it,
                    size = 56.dp,
                    hintDescription = "Volver al panel principal",
                    onFocusHint = cb.setControlHint
                )
            }
        }
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

/** Control de volumen unificado: stepper en TV, slider en móvil. */
@Composable
private fun VolumeControl(
    isTV: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    hint: String,
    setControlHint: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isTV) {
        TvVolumeStepper(
            label = label,
            icon = icon,
            value = value,
            onValueChange = onValueChange,
            showLabel = false,
            horizontalKeysAdjustVolume = false,
            hintDescription = hint,
            onFocusHint = setControlHint,
            modifier = modifier
        )
    } else {
        Icon(
            Icons.Default.VolumeDown,
            null,
            tint = GlassTextMuted,
            modifier = Modifier.size(18.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun PlaybackMusicChip(
    title: String?,
    artist: String?,
    isPlaying: Boolean
) {
    if (title == null) return
    GlassSurface(cornerRadius = 16.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = null,
                tint = GlassText,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(title, color = GlassText, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                if (artist != null) {
                    Text(artist, color = GlassTextMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Fecha legible a partir de `dateAdded` (segundos epoch de MediaStore). Null si no hay dato. */
private fun formatItemDate(dateAdded: Long): String? {
    if (dateAdded <= 0L) return null
    return runCatching {
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.getDefault()).format(Date(dateAdded * 1000))
    }.getOrNull()
}
