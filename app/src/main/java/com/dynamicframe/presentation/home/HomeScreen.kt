package com.dynamicframe.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.BuildConfig
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.presentation.browser.FolderBrowserDialog
import com.dynamicframe.presentation.browser.StoragePicker
import com.dynamicframe.presentation.permissions.MediaPermissionKind
import com.dynamicframe.presentation.permissions.MediaPermissionDeniedBanner
import com.dynamicframe.presentation.permissions.MediaPermissionState
import com.dynamicframe.presentation.permissions.rememberMediaPermissions
import com.dynamicframe.presentation.settings.SettingsScreen
import com.dynamicframe.presentation.settings.SettingsViewModel
import com.dynamicframe.presentation.settings.SettingsDropdownItem
import com.dynamicframe.presentation.settings.SettingsSliderItem
import com.dynamicframe.presentation.settings.displayName
import com.dynamicframe.presentation.device.HomeSection
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.presentation.device.navLabel
import com.dynamicframe.presentation.slideshow.AlbumPillOption
import com.dynamicframe.presentation.common.ConfirmDeleteDialog
import com.dynamicframe.presentation.slideshow.SlideshowViewModel
import com.dynamicframe.ui.components.AppAsyncImage
import com.dynamicframe.ui.theme.NostalgiaAccent
import com.dynamicframe.ui.theme.NostalgiaActionButton
import com.dynamicframe.ui.theme.AppVersionLabel
import com.dynamicframe.ui.theme.NostalgiaCard
import com.dynamicframe.ui.theme.PaperBackground
import com.dynamicframe.ui.theme.PaperInk
import com.dynamicframe.ui.theme.PaperLine
import com.dynamicframe.ui.theme.PaperMuted
import com.dynamicframe.ui.theme.PaperSelected
import com.dynamicframe.ui.theme.PaperSurface
import com.dynamicframe.ui.theme.requestFocusWhenReady
import com.dynamicframe.ui.theme.safeClickable
import com.dynamicframe.ui.theme.tvFocusRequester
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    isTV: Boolean,
    onOpenFullscreen: () -> Unit,
    slideshowViewModel: SlideshowViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    mediaPermissionDenied: Boolean = false,
    permissions: MediaPermissionState? = null
) {
    var destination by remember { mutableStateOf<MemoriaDestination>(MemoriaDestination.AlbumActive) }

    val device = LocalDeviceProfile.current

    val slideshowState by slideshowViewModel.slideshowState.collectAsStateWithLifecycle()
    val config by slideshowViewModel.slideshowConfig.collectAsStateWithLifecycle()
    val musicState by slideshowViewModel.musicState.collectAsStateWithLifecycle()
    val albumPills by slideshowViewModel.albumPills.collectAsStateWithLifecycle()
    val selectedAlbumId by slideshowViewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val settingsConfig by settingsViewModel.config.collectAsStateWithLifecycle()
    val albums by settingsViewModel.albums.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val toastMessage by slideshowViewModel.toastMessage.collectAsStateWithLifecycle()

    val localPermissions = rememberMediaPermissions()
    val activePermissions = permissions ?: localPermissions

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            slideshowViewModel.clearToast()
        }
    }

    ConfirmDeleteDialog(
        visible = showDeleteConfirm,
        title = "¿Borrar esta foto o vídeo?",
        message = "Se eliminará del dispositivo. Esta acción no se puede deshacer.",
        onConfirm = { slideshowViewModel.deleteCurrentSlide() },
        onDismiss = { showDeleteConfirm = false }
    )

    val albumLabel = albumPills.find { it.id == selectedAlbumId }?.label
        ?: albumPills.firstOrNull()?.label
        ?: "Álbum activo"

    val mainContent: @Composable () -> Unit = {
        when (val dest = destination) {
            MemoriaDestination.AlbumActive -> MemoriaDashboard(
                slideshowState = slideshowState,
                config = settingsConfig,
                musicState = musicState,
                albumLabel = albumLabel,
                showPermissionDenied = mediaPermissionDenied,
                onOpenFullscreen = onOpenFullscreen,
                onIntervalChange = settingsViewModel::updateInterval,
                onTransitionChange = settingsViewModel::updateTransition,
                onPhotoShuffleChange = settingsViewModel::updatePhotoShuffle,
                onVideoShuffleChange = settingsViewModel::updateVideoShuffle,
                onMusicShuffleChange = settingsViewModel::updateMusicShuffle,
                onLoopChange = settingsViewModel::updateLoop,
                onVolumeChange = settingsViewModel::updateMusicVolume,
                onShowDateChange = settingsViewModel::updateShowDate
            )

            MemoriaDestination.Albums -> AlbumsPanel(
                albumPills = albumPills,
                albums = albums,
                selectedAlbumId = selectedAlbumId,
                config = settingsConfig,
                showPermissionDenied = mediaPermissionDenied,
                onSelectAlbum = slideshowViewModel::selectAlbum,
                onReload = slideshowViewModel::reloadMedia
            )

            MemoriaDestination.Music -> MusicPanel(
                config = settingsConfig,
                musicState = musicState,
                isPlaybackActive = slideshowState.isPlaying,
                permissions = activePermissions,
                settingsViewModel = settingsViewModel,
                onUpdateConfig = settingsViewModel::updateConfig,
                onUpdateVolume = settingsViewModel::updateMusicVolume,
                onToggleMusic = slideshowViewModel::toggleMusicPlayback,
                onSkipTrack = slideshowViewModel::skipNextTrack
            )

            MemoriaDestination.Settings -> SettingsPanel(
                settingsViewModel = settingsViewModel,
                permissions = activePermissions,
                showPermissionDenied = mediaPermissionDenied,
                onReloadMedia = slideshowViewModel::reloadMedia
            )

            MemoriaDestination.FeatureCatalog -> FeatureCatalogPanel()

            is MemoriaDestination.Roadmap -> ComingSoonPanel(dest.feature)
        }
    }

    if (device.useSidebarNav) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = com.dynamicframe.ui.theme.MemoriaBg
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MemoriaSidebar(
                    selected = destination,
                    photoCount = slideshowState.totalItems,
                    onSelect = { destination = it }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            horizontal = device.contentPaddingH,
                            vertical = device.contentPaddingV
                        )
                ) {
                    mainContent()
                }
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = com.dynamicframe.ui.theme.MemoriaBg,
            bottomBar = {
                Column {
                    MemoriaPhoneBottomNav(selected = destination, onSelect = { destination = it })
                    AppVersionLabel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.dynamicframe.ui.theme.MemoriaSurface)
                            .padding(bottom = 6.dp),
                        fontSize = 10.sp,
                        showBuildCode = true
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        horizontal = device.contentPaddingH,
                        vertical = device.contentPaddingV
                    )
            ) {
                mainContent()
            }
        }
    }
}

@Composable
private fun EditorialSidebar(
    selected: HomeSection,
    photoCount: Int,
    currentIndex: Int,
    onSelect: (HomeSection) -> Unit
) {
    val device = LocalDeviceProfile.current
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(device.isTv) {
        if (device.isTv) {
            delay(350)
            firstItemFocus.requestFocusWhenReady()
        }
    }

    Column(
        modifier = Modifier
            .width(device.sidebarWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NostalgiaCard, PaperSurface, PaperBackground)
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "DYNFRAME",
                style = MaterialTheme.typography.labelSmall,
                color = NostalgiaAccent,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "recuerdos · música · marco",
                fontSize = 11.sp,
                color = PaperMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(28.dp))
            HomeSection.entries.forEachIndexed { index, item ->
                SidebarItem(
                    label = item.navLabel(device.isTv),
                    icon = item.icon,
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    modifier = if (index == 0) Modifier.tvFocusRequester(firstItemFocus) else Modifier
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Column {
            if (photoCount > 0) {
                Text(
                    text = "${currentIndex + 1} / $photoCount fotos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperMuted,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "Sin medios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = PaperMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PhoneBottomNav(
    selected: HomeSection,
    onSelect: (HomeSection) -> Unit
) {
    val device = LocalDeviceProfile.current
    NavigationBar(
        containerColor = PaperSurface,
        tonalElevation = 0.dp
    ) {
        HomeSection.entries.forEach { item ->
            val isSelected = item == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.navLabel(device.isTv),
                        modifier = Modifier.size(device.navIconSize),
                        tint = if (isSelected) PaperInk else PaperMuted
                    )
                },
                label = {
                    Text(
                        text = item.navLabel(device.isTv),
                        fontSize = device.navLabelSize,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PaperInk else PaperMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PaperInk,
                    selectedTextColor = PaperInk,
                    unselectedIconColor = PaperMuted,
                    unselectedTextColor = PaperMuted,
                    indicatorColor = PaperSelected
                )
            )
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (device.isTv) 52.dp else 44.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PaperSelected else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) NostalgiaAccent.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .safeClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (device.isTv) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) PaperInk else PaperMuted,
            modifier = Modifier.size(device.navIconSize)
        )
        if (device.showNavLabels) {
            Text(
                text = label,
                color = if (selected) PaperInk else PaperMuted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = device.navLabelSize
            )
        }
    }
}

@Composable
private fun SlideshowPanel(
    largeClock: Boolean,
    currentTime: String,
    currentDate: String,
    slideshowState: com.dynamicframe.domain.model.SlideshowState,
    config: SlideshowConfig,
    musicState: com.dynamicframe.domain.model.MusicPlayerState,
    onPlayPause: () -> Unit,
    onNextSlide: () -> Unit,
    onPreviousSlide: () -> Unit,
    onToggleMusic: () -> Unit,
    onSkipTrack: () -> Unit,
    onSelectThumbnail: (Int) -> Unit,
    onOpenFullscreen: () -> Unit,
    onDeleteCurrent: () -> Unit,
    onIntervalChange: (Int) -> Unit,
    largeClockToggle: (Boolean) -> Unit,
    sidebarVisible: Boolean,
    onSidebarToggle: (Boolean) -> Unit,
    canDelete: Boolean
) {
    val device = LocalDeviceProfile.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (config.showClock) {
                    val clockSize = if (largeClock) device.clockLargeSp else device.clockCompactSp
                    Text(
                        text = currentTime,
                        fontSize = clockSize,
                        fontWeight = FontWeight.Light,
                        color = PaperInk,
                        letterSpacing = (-1).sp
                    )
                    if (config.showDate) {
                        Text(
                            text = currentDate.replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) },
                            color = PaperMuted,
                            fontSize = if (device.isTv) 18.sp else 14.sp
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(if (device.isTv) 12.dp else 8.dp)) {
                DeviceActionButton(
                    icon = if (slideshowState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (slideshowState.isPlaying) "Pausar" else "Reproducir",
                    onClick = onPlayPause
                )
                DeviceActionButton(
                    icon = Icons.Default.SkipNext,
                    label = "Siguiente",
                    onClick = onNextSlide
                )
                DeviceActionButton(
                    icon = Icons.Default.Fullscreen,
                    label = if (device.isTv) "Pantalla completa" else "Pantalla",
                    onClick = onOpenFullscreen
                )
                if (canDelete) {
                    DeviceActionButton(
                        icon = Icons.Default.DeleteOutline,
                        label = "Borrar",
                        onClick = onDeleteCurrent,
                        destructive = true
                    )
                }
            }
        }

        Spacer(Modifier.height(if (device.isTv) 28.dp else 16.dp))

        ThumbnailRow(
            items = slideshowState.playlistItems,
            currentIndex = slideshowState.currentIndex,
            onSelect = onSelectThumbnail
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val track = musicState.currentTrack
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(PaperInk, CircleShape)
                )
                Text(
                    text = if (track != null) "${track.title} · ${track.artist}"
                    else "Sin música configurada",
                    color = PaperInk,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.isTv) {
                    Text("ajustar música", color = PaperMuted, fontSize = 14.sp)
                    Icon(Icons.Default.Tune, contentDescription = null, tint = PaperMuted, modifier = Modifier.size(20.dp))
                }
                DeviceActionButton(
                    icon = if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (musicState.isPlaying) "Pausar" else "Música",
                    onClick = onToggleMusic,
                    compact = !device.isTv
                )
                DeviceActionButton(
                    icon = Icons.Default.SkipNext,
                    label = "Canción",
                    onClick = onSkipTrack,
                    compact = !device.isTv
                )
            }
        }

        Spacer(Modifier.weight(1f))

        QuickSettingsPanel(
            largeClock = largeClock,
            sidebarVisible = sidebarVisible,
            intervalSeconds = config.intervalSeconds,
            currentIndex = slideshowState.currentIndex,
            windowSize = minOf(4, maxOf(slideshowState.playlistItems.size, 1)),
            onLargeClockChange = largeClockToggle,
            onSidebarToggle = onSidebarToggle,
            onIntervalChange = onIntervalChange
        )
    }
}

@Composable
private fun ThumbnailRow(
    items: List<com.dynamicframe.domain.model.MediaItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit
) {
    if (items.isEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { idx ->
                PlaceholderThumbnail(index = idx, selected = idx == 0)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ve a Ajustes para elegir carpetas de fotos o videos.",
            color = PaperMuted,
            fontSize = 13.sp
        )
        return
    }

    val windowSize = minOf(4, items.size)
    val indices = (0 until windowSize).map { offset ->
        (currentIndex + offset) % items.size
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(indices) { _, index ->
            val item = items[index]
            val selected = index == currentIndex
            ThumbnailCard(
                item = item,
                selected = selected,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun ThumbnailCard(
    item: com.dynamicframe.domain.model.MediaItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val device = LocalDeviceProfile.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(width = device.thumbnailWidth, height = device.thumbnailHeight)
            .clip(shape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) PaperInk else PaperLine,
                shape = shape
            )
            .safeClickable(onClick = onClick)
    ) {
        AppAsyncImage(
            uri = item.uri,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            crossfadeMillis = 300
        )
        if (item.type == MediaType.VIDEO) {
            Icon(
                Icons.Default.PlayCircleOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderThumbnail(index: Int, selected: Boolean) {
    val device = LocalDeviceProfile.current
    val colors = listOf(
        Color(0xFFB8D4E8),
        Color(0xFFE8DFC8),
        Color(0xFFC8E8D4),
        Color(0xFFD4C8E8)
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(width = device.thumbnailWidth, height = device.thumbnailHeight)
            .clip(shape)
            .background(colors[index.coerceIn(0, colors.lastIndex)])
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = PaperInk,
                shape = shape
            )
    )
}

@Composable
private fun QuickSettingsPanel(
    largeClock: Boolean,
    sidebarVisible: Boolean,
    intervalSeconds: Int,
    currentIndex: Int,
    windowSize: Int,
    onLargeClockChange: (Boolean) -> Unit,
    onSidebarToggle: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    val device = LocalDeviceProfile.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperSurface)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        QuickSettingRow(
            label = "Estilo de reloj",
            value = if (largeClock) "Grande" else "Compacto",
            onClick = { onLargeClockChange(!largeClock) }
        )
        HorizontalDivider(color = PaperLine)
        if (device.isTv) {
            QuickSettingRowWithSwitch(
                label = "Sidebar en TV",
                checked = sidebarVisible,
                onCheckedChange = onSidebarToggle
            )
            HorizontalDivider(color = PaperLine)
        }
        QuickSettingRow(
            label = "Intervalo",
            value = "${intervalSeconds}s",
            onClick = {
                val next = when {
                    intervalSeconds >= 30 -> 5
                    intervalSeconds >= 15 -> 30
                    intervalSeconds >= 10 -> 15
                    else -> intervalSeconds + 5
                }
                onIntervalChange(next)
            }
        )
        HorizontalDivider(color = PaperLine)
        QuickSettingRow(
            label = "Slide visible",
            value = if (windowSize > 0) "${(currentIndex % windowSize) + 1} de $windowSize" else "—",
            onClick = {}
        )
    }
}

@Composable
private fun QuickSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PaperInk, fontSize = 15.sp)
        Text(value, color = PaperMuted, fontSize = 15.sp)
    }
}

@Composable
private fun QuickSettingRowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val device = LocalDeviceProfile.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (device.isTv) Modifier.safeClickable { onCheckedChange(!checked) }
                else Modifier
            )
            .padding(vertical = if (device.isTv) 10.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PaperInk, fontSize = 15.sp)
        if (device.isTv) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (checked) "Activado" else "Desactivado",
                tint = if (checked) NostalgiaAccent else PaperMuted,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun DeviceActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false,
    destructive: Boolean = false
) {
    val device = LocalDeviceProfile.current
    val useTvButton = device.isTv && !compact
    val size = if (compact) 40.dp else device.actionButtonSize
    val borderColor = if (destructive) NostalgiaAccent.copy(alpha = 0.5f) else PaperLine
    val iconTint = if (destructive) NostalgiaAccent else PaperInk

    if (useTvButton) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (destructive) PaperSelected else PaperSurface)
                .border(1.dp, borderColor, RoundedCornerShape(50))
                .safeClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(label, color = iconTint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    } else if (device.isTv) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (destructive) PaperSelected else PaperSurface)
                .border(1.dp, borderColor, CircleShape)
                .safeClickable(onClick = onClick, showFocusBorder = false),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(if (destructive) PaperSelected else PaperSurface, CircleShape)
                .border(1.dp, borderColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = iconTint)
        }
    }
}

@Composable
private fun AlbumsPanel(
    albumPills: List<AlbumPillOption>,
    albums: List<MediaAlbum>,
    selectedAlbumId: String?,
    config: SlideshowConfig,
    showPermissionDenied: Boolean,
    onSelectAlbum: (String?) -> Unit,
    onReload: () -> Unit
) {
    val device = LocalDeviceProfile.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (device.isTv) Modifier.focusGroup() else Modifier)
    ) {
        Text("Álbumes", style = MaterialTheme.typography.titleMedium, color = PaperInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Elige qué carpeta o álbum mostrar en el slideshow.",
            color = PaperMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        if (showPermissionDenied) {
            MediaPermissionDeniedBanner(
                message = "Activa el acceso a fotos y vídeos en Ajustes del sistema para ver tus álbumes."
            )
            Spacer(Modifier.height(16.dp))
        }

        if (config.hasCustomMediaFolders()) {
            albumPills.forEach { pill ->
                AlbumSelectRow(
                    label = pill.label,
                    selected = pill.id == selectedAlbumId || (pill.id == null && selectedAlbumId == null),
                    onClick = { onSelectAlbum(pill.id) }
                )
            }
        } else if (albums.isNotEmpty()) {
            albums.forEach { album ->
                AlbumSelectRow(
                    label = "${album.name} (${album.itemCount})",
                    selected = config.selectedAlbumIds.contains(album.id),
                    onClick = {
                        val ids = if (config.selectedAlbumIds.contains(album.id)) emptyList()
                        else listOf(album.id)
                        onSelectAlbum(ids.singleOrNull())
                    }
                )
            }
        } else {
            Text(
                "No hay álbumes. Configura carpetas en Ajustes.",
                color = PaperMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AlbumSelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PaperSelected else Color.Transparent)
            .safeClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PaperInk)
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = PaperInk)
    }
    HorizontalDivider(color = PaperLine)
}

@Composable
private fun MusicPanel(
    config: SlideshowConfig,
    musicState: com.dynamicframe.domain.model.MusicPlayerState,
    isPlaybackActive: Boolean,
    permissions: MediaPermissionState,
    settingsViewModel: SettingsViewModel,
    onUpdateConfig: (SlideshowConfig) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleMusic: () -> Unit,
    onSkipTrack: () -> Unit
) {
    val context = LocalContext.current
    val device = LocalDeviceProfile.current
    var showMusicFolderBrowser by remember { mutableStateOf(false) }
    val useInAppBrowser = StoragePicker.shouldUseInAppBrowser(device.isTv, context)

    val musicFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val folders = config.musicFolderUris
            if (!folders.contains(it.toString())) {
                onUpdateConfig(config.copy(musicFolderUris = folders + it.toString()))
            }
        }
    }

    FolderBrowserDialog(
        visible = showMusicFolderBrowser,
        title = "Carpeta de música",
        onDismiss = { showMusicFolderBrowser = false },
        onSelectFolder = { uri ->
            if (!config.musicFolderUris.contains(uri)) {
                onUpdateConfig(config.copy(musicFolderUris = config.musicFolderUris + uri))
            }
            showMusicFolderBrowser = false
        },
        listRoots = settingsViewModel::storageRoots,
        listSubfolders = settingsViewModel::storageSubfolders
    )

    LazyColumn(
        modifier = if (device.isTv) Modifier.focusGroup() else Modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("Música", style = MaterialTheme.typography.titleMedium, color = PaperInk)
            Spacer(Modifier.height(16.dp))
        }

        item {
            SettingsSliderItem(
                title = "Volumen",
                value = config.musicVolume,
                valueRange = 0f..1f,
                valueLabel = "${(config.musicVolume * 100).toInt()}%",
                onValueChange = onUpdateVolume
            )
        }

        item {
            SettingsDropdownItem(
                title = "Fuente",
                icon = Icons.Default.LibraryMusic,
                currentValue = config.musicSourceType.displayName(),
                options = com.dynamicframe.domain.model.MusicSourceType.entries.map { it.displayName() },
                onSelect = { idx ->
                    onUpdateConfig(config.copy(musicSourceType = com.dynamicframe.domain.model.MusicSourceType.entries[idx]))
                }
            )
        }

        if (config.musicSourceType == com.dynamicframe.domain.model.MusicSourceType.LOCAL_FOLDER) {
            item {
                NostalgiaActionButton(
                    text = if (useInAppBrowser) "Añadir carpeta de música"
                    else "Elegir carpeta de música",
                    icon = Icons.Default.FolderOpen,
                    onClick = {
                        permissions.requestFor(MediaPermissionKind.MUSIC) {
                            if (useInAppBrowser) showMusicFolderBrowser = true
                            else musicFolderLauncher.launch(null)
                        }
                    }
                )
            }
            items(config.musicFolderUris, key = { "music:$it" }) { uri ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperSurface)
                        .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(settingsViewModel.folderLabel(uri), color = PaperInk, maxLines = 1, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .safeClickable {
                                onUpdateConfig(config.copy(musicFolderUris = config.musicFolderUris - uri))
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar", tint = PaperMuted)
                        if (device.isTv) {
                            Text("Quitar", color = PaperMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceActionButton(
                    icon = if (isPlaybackActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (isPlaybackActive) "Pausar" else "Reproducir",
                    onClick = onToggleMusic
                )
                DeviceActionButton(
                    icon = Icons.Default.SkipNext,
                    label = "Siguiente",
                    onClick = onSkipTrack
                )
            }
        }

        musicState.currentTrack?.let { track ->
            item {
                Text("${track.title} · ${track.artist}", color = PaperMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    settingsViewModel: SettingsViewModel,
    permissions: MediaPermissionState,
    showPermissionDenied: Boolean,
    onReloadMedia: () -> Unit
) {
    SettingsScreen(
        viewModel = settingsViewModel,
        onBack = {},
        embedded = true,
        modifier = Modifier.fillMaxSize(),
        showPermissionDenied = showPermissionDenied,
        requestMediaAccess = { onGranted ->
            permissions.requestFor(MediaPermissionKind.PHOTOS_VIDEOS, onGranted)
        },
        requestMusicAccess = { onGranted ->
            permissions.requestFor(MediaPermissionKind.MUSIC, onGranted)
        },
        onMediaChanged = onReloadMedia
    )
}
