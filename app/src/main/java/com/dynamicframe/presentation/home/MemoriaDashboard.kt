package com.dynamicframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.presentation.common.TvPickerChip
import com.dynamicframe.presentation.common.TvPickerChipStyle
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.presentation.permissions.MediaPermissionDeniedBanner
import com.dynamicframe.presentation.settings.dashboardLabel
import com.dynamicframe.ui.theme.*

@Composable
fun MemoriaDashboard(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    albumLabel: String,
    showPermissionDenied: Boolean = false,
    onGrantAccess: (() -> Unit)? = null,
    onOpenFullscreen: () -> Unit,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onPhotoShuffleChange: (Boolean) -> Unit,
    onVideoShuffleChange: (Boolean) -> Unit,
    onMusicShuffleChange: (Boolean) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onShowDateChange: (Boolean) -> Unit
) {
    val items = slideshowState.playlistItems
    val photoCount = items.count { it.type == MediaType.IMAGE }
    val videoCount = items.count { it.type == MediaType.VIDEO }
    val totalDurationSec = estimateDurationSec(items, config.intervalSeconds, photoCount, videoCount)
    val trackCount = musicState.playlist.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MemoriaTopBar(
            albumLabel = albumLabel,
            onFrameMode = onOpenFullscreen
        )

        if (showPermissionDenied) {
            MediaPermissionDeniedBanner(
                message = "Concede acceso a fotos y vídeos en Ajustes del sistema o elige carpetas en Configuración.",
                onGrantAccess = onGrantAccess
            )
        }

        Spacer(Modifier.weight(1f))

        MemoriaControlsPanel(
            config = config,
            onIntervalChange = onIntervalChange,
            onTransitionChange = onTransitionChange,
            onPhotoShuffleChange = onPhotoShuffleChange,
            onVideoShuffleChange = onVideoShuffleChange,
            onMusicShuffleChange = onMusicShuffleChange,
            onLoopChange = onLoopChange,
            onVolumeChange = onVolumeChange,
            onCaptionChange = onShowDateChange
        )

        MemoriaStatsFooter(
            photoCount = photoCount,
            videoCount = videoCount,
            durationSec = totalDurationSec,
            trackCount = trackCount
        )
    }
}

@Composable
private fun MemoriaTopBar(
    albumLabel: String,
    onFrameMode: () -> Unit
) {
    val frameFocus = remember { MutableInteractionSource() }
    val frameFocused by frameFocus.collectIsFocusedAsState()
    val (showHint, pulseHint) = rememberTransientVisibility(2_500L)

    LaunchedEffect(frameFocused) {
        if (frameFocused) pulseHint()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .focusProperties { canFocus = false },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PhotoAlbum, null, tint = MemoriaMuted, modifier = Modifier.size(20.dp))
                Text(
                    albumLabel,
                    color = MemoriaInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MemoriaPlaybackCircleButton(
                icon = Icons.Default.TheaterComedy,
                contentDescription = "Modo cuadro: pantalla completa con reproducción",
                onClick = onFrameMode,
                size = 58.dp,
                interactionSource = frameFocus
            )
        }
        if (showHint && frameFocused) {
            Text(
                "Modo cuadro: abre pantalla completa y reproduce fotos, vídeos y música",
                color = MemoriaMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun MemoriaControlsPanel(
    config: SlideshowConfig,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onPhotoShuffleChange: (Boolean) -> Unit,
    onVideoShuffleChange: (Boolean) -> Unit,
    onMusicShuffleChange: (Boolean) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onCaptionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    val durationOptions = listOf(5, 8, 10, 15, 30, 60, 120)
    val durationLabels = durationOptions.map { "${it}s" }
    val currentDurationLabel = "${config.intervalSeconds}s"
    val transitionLabels = TransitionType.entries.map { it.dashboardLabel() }
    val currentTransitionLabel = config.transition.dashboardLabel()
    val (hintText, setHint) = rememberControlHintState(
        "Selecciona un control para ver su función"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MemoriaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MemoriaLine)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (device.isTv) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvPickerChip(
                        title = "Duración por foto",
                        description = "Segundos que se muestra cada foto. OK: siguiente · Mantener OK: lista",
                        icon = Icons.Default.Timer,
                        displayValue = currentDurationLabel,
                        currentValue = currentDurationLabel,
                        options = durationLabels,
                        onSelect = { onIntervalChange(durationOptions[it.coerceIn(durationOptions.indices)]) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f),
                        style = TvPickerChipStyle.Compact
                    )
                    TvPickerChip(
                        title = "Transición",
                        description = "Efecto al cambiar de imagen. OK: siguiente · Mantener OK: lista",
                        icon = Icons.Default.AutoAwesome,
                        displayValue = currentTransitionLabel,
                        currentValue = currentTransitionLabel,
                        options = transitionLabels,
                        onSelect = { onTransitionChange(TransitionType.entries[it]) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f),
                        style = TvPickerChipStyle.Compact
                    )
                    MemoriaToggleChip(
                        description = "Mostrar u ocultar leyenda con fecha en las fotos",
                        label = "Leyenda",
                        icon = Icons.Default.Subtitles,
                        checked = config.showDate,
                        onToggle = { onCaptionChange(!config.showDate) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaToggleChip(
                        description = "Repetir el álbum al llegar al final",
                        label = "Bucle",
                        icon = Icons.Default.Repeat,
                        checked = config.loop,
                        onToggle = { onLoopChange(!config.loop) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemoriaShuffleToggleChip(
                        description = "Orden aleatorio de las fotos del álbum",
                        contentIcon = Icons.Default.Photo,
                        checked = config.photoShuffle,
                        onToggle = { onPhotoShuffleChange(!config.photoShuffle) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaShuffleToggleChip(
                        description = "Orden aleatorio de los vídeos del álbum",
                        contentIcon = Icons.Default.Videocam,
                        checked = config.videoShuffle,
                        onToggle = { onVideoShuffleChange(!config.videoShuffle) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaShuffleToggleChip(
                        description = "Orden aleatorio de la música de fondo",
                        contentIcon = Icons.Default.MusicNote,
                        checked = config.musicShuffle,
                        onToggle = { onMusicShuffleChange(!config.musicShuffle) },
                        onFocusHint = setHint,
                        modifier = Modifier.weight(1f)
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TvVolumeStepper(
                        label = "Volumen música",
                        icon = Icons.Default.VolumeUp,
                        value = config.musicVolume,
                        onValueChange = onVolumeChange,
                        hintDescription = "Volumen de la música. ← → ajustar · OK: subir",
                        onFocusHint = setHint,
                        showLabel = false,
                        showInlineIcon = true,
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 340.dp)
                            .fillMaxWidth(0.6f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        MemoriaMobileDropdown(
                            title = "Duración",
                            value = "${config.intervalSeconds} seg",
                            options = durationOptions.map { "$it seg" },
                            onSelect = { onIntervalChange(durationOptions[it]) }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        MemoriaMobileDropdown(
                            title = "Transición",
                            value = config.transition.dashboardLabel(),
                            options = transitionLabels,
                            onSelect = { onTransitionChange(TransitionType.entries[it]) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoriaToggleChip(
                        description = "Mostrar leyenda",
                        label = "Leyenda",
                        icon = Icons.Default.Subtitles,
                        checked = config.showDate,
                        onToggle = { onCaptionChange(!config.showDate) },
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaToggleChip(
                        description = "Bucle continuo",
                        label = "Bucle",
                        icon = Icons.Default.Repeat,
                        checked = config.loop,
                        onToggle = { onLoopChange(!config.loop) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoriaShuffleToggleChip(
                        description = "Fotos aleatorias",
                        contentIcon = Icons.Default.Photo,
                        checked = config.photoShuffle,
                        onToggle = { onPhotoShuffleChange(!config.photoShuffle) },
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaShuffleToggleChip(
                        description = "Videos aleatorios",
                        contentIcon = Icons.Default.Videocam,
                        checked = config.videoShuffle,
                        onToggle = { onVideoShuffleChange(!config.videoShuffle) },
                        modifier = Modifier.weight(1f)
                    )
                    MemoriaShuffleToggleChip(
                        description = "Música aleatoria",
                        contentIcon = Icons.Default.MusicNote,
                        checked = config.musicShuffle,
                        onToggle = { onMusicShuffleChange(!config.musicShuffle) },
                        modifier = Modifier.weight(1f)
                    )
                }
                TvVolumeStepper(
                    label = "Volumen música",
                    icon = Icons.Default.VolumeUp,
                    value = config.musicVolume,
                    onValueChange = onVolumeChange,
                    showInlineIcon = true
                )
            }
            MemoriaControlsHintBar(text = hintText)
        }
    }
}

@Composable
private fun MemoriaShuffleToggleChip(
    description: String,
    contentIcon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusHint: ((String) -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    FocusHintEffect(focused = focused, description = description, onHint = onFocusHint ?: {})

    val borderColor = when {
        focused -> MemoriaPurple
        checked -> MemoriaPurple
        else -> MemoriaLine.copy(alpha = 0.45f)
    }
    val bg = when {
        focused -> MemoriaPurpleSoft
        checked -> MemoriaPurpleSoft
        else -> MemoriaSurface
    }
    val tint = if (checked || focused) MemoriaPurple else MemoriaMuted

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(bg, shape)
            .border(2.dp, borderColor, shape)
            .safeClickable(
                interactionSource = interactionSource,
                showFocusBorder = false,
                focusShape = shape,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                contentIcon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                ShuffleIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MemoriaToggleChip(
    description: String,
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusHint: ((String) -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    FocusHintEffect(focused = focused, description = description, onHint = onFocusHint ?: {})

    val borderColor = when {
        focused -> MemoriaPurple
        checked -> MemoriaPurple
        else -> MemoriaLine.copy(alpha = 0.45f)
    }
    val bg = when {
        focused -> MemoriaPurpleSoft
        checked -> MemoriaPurpleSoft
        else -> MemoriaSurface
    }
    val tint = if (checked || focused) MemoriaPurple else MemoriaMuted

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(bg, shape)
            .border(2.dp, borderColor, shape)
            .safeClickable(
                interactionSource = interactionSource,
                showFocusBorder = false,
                focusShape = shape,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = tint,
                fontSize = 10.sp,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MemoriaMobileDropdown(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MemoriaSurface)
                .border(1.dp, MemoriaLine, RoundedCornerShape(8.dp))
                .safeClickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = MemoriaMuted, fontSize = 11.sp)
                Text(value, color = MemoriaInk, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = MemoriaMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MemoriaStatsFooter(
    photoCount: Int,
    videoCount: Int,
    durationSec: Int,
    trackCount: Int
) {
    val min = durationSec / 60
    val sec = durationSec % 60
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MemoriaInfoBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(Icons.Default.Photo, photoCount.toString(), "Fotos")
            StatCell(Icons.Default.Videocam, videoCount.toString(), "Videos")
            StatCell(Icons.Default.Timer, "%d:%02d".format(min, sec), "Duración")
            StatCell(Icons.Default.MusicNote, trackCount.toString(), "Pistas")
        }
        AppVersionLabel(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
            fontSize = 10.sp,
            showBuildCode = true
        )
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.focusProperties { canFocus = false }
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MemoriaMuted,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(value, color = MemoriaInfoInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun estimateDurationSec(
    items: List<com.dynamicframe.domain.model.MediaItem>,
    intervalSec: Int,
    photoCount: Int,
    videoCount: Int
): Int {
    val photoDur = photoCount * intervalSec
    val videoDur = items.filter { it.type == MediaType.VIDEO }.sumOf {
        (it.duration / 1000).toInt().coerceAtLeast(30)
    }
    return (photoDur + videoDur).coerceAtLeast(if (items.isEmpty()) 0 else intervalSec)
}
