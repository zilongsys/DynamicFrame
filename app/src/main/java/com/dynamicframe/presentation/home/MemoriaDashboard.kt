package com.dynamicframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.presentation.settings.SettingsDropdownItem
import com.dynamicframe.presentation.settings.displayName
import com.dynamicframe.presentation.slideshow.CenterPlayPauseButton
import com.dynamicframe.presentation.slideshow.MediaCircleButton
import com.dynamicframe.ui.theme.*

@Composable
fun MemoriaDashboard(
    slideshowState: SlideshowState,
    config: SlideshowConfig,
    musicState: MusicPlayerState,
    albumLabel: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onUpdateConfig: (SlideshowConfig) -> Unit
) {
    val items = slideshowState.playlistItems
    val photoCount = items.count { it.type == MediaType.IMAGE }
    val videoCount = items.count { it.type == MediaType.VIDEO }
    val totalDurationSec = estimateDurationSec(items, config.intervalSeconds, photoCount, videoCount)
    val trackCount = musicState.playlist.size

    val device = LocalDeviceProfile.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (device.isTv) Modifier.focusGroup() else Modifier)
    ) {
        MemoriaTopBar(
            albumLabel = albumLabel,
            onFrameMode = onOpenFullscreen,
            onPlay = onPlayPause,
            isPlaying = slideshowState.isPlaying
        )
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight)
            MemoriaSquarePreview(
                slideshowState = slideshowState,
                musicState = musicState,
                config = config,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrev = onPrev,
                modifier = Modifier.size(side)
            )
        }

        Spacer(Modifier.height(12.dp))
        MemoriaControlsPanel(
            config = config,
            onIntervalChange = onIntervalChange,
            onTransitionChange = onTransitionChange,
            onShuffleChange = onShuffleChange,
            onLoopChange = onLoopChange,
            onVolumeChange = onVolumeChange,
            onCaptionChange = { onUpdateConfig(config.copy(showDate = it)) }
        )
        Spacer(Modifier.height(8.dp))
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
    onFrameMode: () -> Unit,
    onPlay: () -> Unit,
    isPlaying: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
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
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TopBarCircleAction(
                icon = Icons.Default.CropSquare,
                label = "Modo cuadro",
                onClick = onFrameMode
            )
            TopBarCircleAction(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (isPlaying) "Pausar" else "Play",
                onClick = onPlay
            )
        }
    }
}

@Composable
private fun TopBarCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MemoriaPlaybackCircleButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            size = 58.dp
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = MemoriaMuted, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun MemoriaSquarePreview(
    slideshowState: SlideshowState,
    musicState: MusicPlayerState,
    config: SlideshowConfig,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = slideshowState.currentItem
    val total = slideshowState.totalItems
    val index = if (total > 0) slideshowState.currentIndex + 1 else 0
    val device = LocalDeviceProfile.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        PlaybackLetterboxBackground(
            type = config.playbackBackgroundType,
            customImageUri = config.playbackBackgroundImageUri,
            modifier = Modifier.fillMaxSize()
        )
        if (item != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .crossfade(300)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (item.type == MediaType.VIDEO) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("VIDEO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MemoriaPreviewBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    slideshowState.error ?: "Sin medios",
                    color = MemoriaMuted,
                    fontSize = 14.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = if (device.isTv) Modifier.focusGroup() else Modifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaCircleButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    onClick = onPrev,
                    size = 50.dp
                )
                CenterPlayPauseButton(
                    isPlaying = slideshowState.isPlaying,
                    onClick = onPlayPause,
                    buttonSize = 64.dp
                )
                MediaCircleButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    onClick = onNext,
                    size = 50.dp
                )
            }
        }

        if (total > 0) {
            Text(
                text = "$index / $total",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        musicState.currentTrack?.let { track ->
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${track.title} — ${track.artist}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MemoriaControlsPanel(
    config: SlideshowConfig,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onCaptionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MemoriaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MemoriaLine)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            val durationOptions = listOf(5, 8, 10, 15, 30, 60, 120)
            if (device.isTv) {
                SettingsDropdownItem(
                    title = "Duración por foto",
                    icon = Icons.Default.Timer,
                    currentValue = "${config.intervalSeconds} seg",
                    options = durationOptions.map { "$it seg" },
                    onSelect = { onIntervalChange(durationOptions[it.coerceIn(durationOptions.indices)]) }
                )
                Spacer(Modifier.height(4.dp))
                SettingsDropdownItem(
                    title = "Transición",
                    icon = Icons.Default.AutoAwesome,
                    currentValue = config.transition.displayName(),
                    options = TransitionType.entries.map { it.displayName() },
                    onSelect = { onTransitionChange(TransitionType.entries[it]) }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        SettingsDropdownItem(
                            title = "Duración por foto",
                            icon = Icons.Default.Timer,
                            currentValue = "${config.intervalSeconds} seg",
                            options = durationOptions.map { "$it seg" },
                            onSelect = { onIntervalChange(durationOptions[it.coerceIn(durationOptions.indices)]) }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SettingsDropdownItem(
                            title = "Transición",
                            icon = Icons.Default.AutoAwesome,
                            currentValue = config.transition.displayName(),
                            options = TransitionType.entries.map { it.displayName() },
                            onSelect = { onTransitionChange(TransitionType.entries[it]) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TvVolumeStepper(
                label = "Volumen música",
                icon = Icons.Default.VolumeUp,
                value = config.musicVolume,
                onValueChange = onVolumeChange
            )
            Spacer(Modifier.height(6.dp))
            if (device.isTv) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TvSwitchRow("Orden aleatorio", Icons.Default.Shuffle, config.shuffle, onShuffleChange)
                    TvSwitchRow("Mostrar leyenda", Icons.Default.Subtitles, config.showDate, onCaptionChange)
                    TvSwitchRow("Bucle continuo", Icons.Default.Repeat, config.loop, onLoopChange)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvSwitchRow("Orden aleatorio", Icons.Default.Shuffle, config.shuffle, onShuffleChange)
                    TvSwitchRow("Mostrar leyenda", Icons.Default.Subtitles, config.showDate, onCaptionChange)
                    TvSwitchRow("Bucle continuo", Icons.Default.Repeat, config.loop, onLoopChange)
                }
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
            StatCell(photoCount.toString(), "fotos")
            StatCell(videoCount.toString(), "videos")
            StatCell("%d:%02d".format(min, sec), "duración")
            StatCell(trackCount.toString(), "pistas")
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
private fun StatCell(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.focusProperties { canFocus = false }
    ) {
        Text(value, color = MemoriaInfoInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, color = MemoriaMuted, fontSize = 10.sp, maxLines = 1)
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
