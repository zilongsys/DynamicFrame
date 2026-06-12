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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dynamicframe.BuildConfig
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.presentation.permissions.MediaPermissionKind
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
import com.dynamicframe.presentation.slideshow.SlideshowViewModel
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
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var section by remember { mutableStateOf(HomeSection.SLIDESHOW) }
    var sidebarVisible by remember { mutableStateOf(isTV) }
    var largeClock by remember { mutableStateOf(true) }

    val device = LocalDeviceProfile.current

    val slideshowState by slideshowViewModel.slideshowState.collectAsStateWithLifecycle()
    val config by slideshowViewModel.slideshowConfig.collectAsStateWithLifecycle()
    val musicState by slideshowViewModel.musicState.collectAsStateWithLifecycle()
    val albumPills by slideshowViewModel.albumPills.collectAsStateWithLifecycle()
    val selectedAlbumId by slideshowViewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val settingsConfig by settingsViewModel.config.collectAsStateWithLifecycle()
    val albums by settingsViewModel.albums.collectAsStateWithLifecycle()

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es")).format(now)
            delay(1000)
        }
    }

    val mainContent: @Composable () -> Unit = {
        when (section) {
            HomeSection.SLIDESHOW -> SlideshowPanel(
                largeClock = largeClock,
                currentTime = currentTime,
                currentDate = currentDate,
                slideshowState = slideshowState,
                config = config,
                musicState = musicState,
                onPlayPause = {
                    if (slideshowState.isPlaying) slideshowViewModel.pauseSlideshow()
                    else slideshowViewModel.startSlideshow()
                },
                onNextSlide = slideshowViewModel::nextSlide,
                onPreviousSlide = slideshowViewModel::previousSlide,
                onToggleMusic = slideshowViewModel::toggleMusicPlayback,
                onSkipTrack = slideshowViewModel::skipNextTrack,
                onSelectThumbnail = slideshowViewModel::jumpToSlide,
                onOpenFullscreen = onOpenFullscreen,
                onIntervalChange = settingsViewModel::updateInterval,
                largeClockToggle = { largeClock = it },
                sidebarVisible = sidebarVisible,
                onSidebarToggle = { sidebarVisible = it }
            )

            HomeSection.ALBUMS -> AlbumsPanel(
                albumPills = albumPills,
                albums = albums,
                selectedAlbumId = selectedAlbumId,
                config = settingsConfig,
                onSelectAlbum = slideshowViewModel::selectAlbum,
                onReload = slideshowViewModel::reloadMedia
            )

            HomeSection.MUSIC -> MusicPanel(
                config = settingsConfig,
                musicState = musicState,
                onUpdateConfig = settingsViewModel::updateConfig,
                onUpdateVolume = settingsViewModel::updateMusicVolume,
                onToggleMusic = slideshowViewModel::toggleMusicPlayback,
                onSkipTrack = slideshowViewModel::skipNextTrack
            )

            HomeSection.SETTINGS -> SettingsPanel(
                settingsViewModel = settingsViewModel,
                onReloadMedia = slideshowViewModel::reloadMedia
            )
        }
    }

    if (device.useSidebarNav) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperBackground)
                .then(if (device.isTv) Modifier.focusGroup() else Modifier)
        ) {
            if (sidebarVisible) {
                EditorialSidebar(
                    selected = section,
                    photoCount = slideshowState.totalItems,
                    currentIndex = slideshowState.currentIndex,
                    onSelect = { section = it }
                )
            }
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
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperBackground)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = device.contentPaddingH,
                        vertical = device.contentPaddingV
                    )
            ) {
                mainContent()
            }
            PhoneBottomNav(
                selected = section,
                onSelect = { section = it }
            )
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
            .background(PaperSurface)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "DYNFRAME",
                style = MaterialTheme.typography.labelSmall,
                color = PaperMuted,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(32.dp))
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
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) PaperSelected else Color.Transparent)
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
    onIntervalChange: (Int) -> Unit,
    largeClockToggle: (Boolean) -> Unit,
    sidebarVisible: Boolean,
    onSidebarToggle: (Boolean) -> Unit
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PaperInk, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeviceActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val device = LocalDeviceProfile.current
    val useTvButton = device.isTv && !compact
    val size = if (compact) 40.dp else device.actionButtonSize

    if (useTvButton) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PaperSurface)
                .border(1.dp, PaperLine, RoundedCornerShape(50))
                .safeClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = PaperInk, modifier = Modifier.size(22.dp))
            Text(label, color = PaperInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    } else if (device.isTv) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(PaperSurface)
                .border(1.dp, PaperLine, CircleShape)
                .safeClickable(onClick = onClick, showFocusBorder = false),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = PaperInk, modifier = Modifier.size(22.dp))
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(PaperSurface, CircleShape)
                .border(1.dp, PaperLine, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = PaperInk)
        }
    }
}

@Composable
private fun AlbumsPanel(
    albumPills: List<AlbumPillOption>,
    albums: List<MediaAlbum>,
    selectedAlbumId: String?,
    config: SlideshowConfig,
    onSelectAlbum: (String?) -> Unit,
    onReload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Álbumes", style = MaterialTheme.typography.titleMedium, color = PaperInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Elige qué carpeta o álbum mostrar en el slideshow.",
            color = PaperMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        if (config.mediaFolderUris.isNotEmpty()) {
            albumPills.forEach { pill ->
                AlbumSelectRow(
                    label = pill.label,
                    selected = pill.id == selectedAlbumId || (pill.id == null && selectedAlbumId == null),
                    onClick = { onSelectAlbum(pill.id); onReload() }
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
                        onReload()
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
    onUpdateConfig: (SlideshowConfig) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleMusic: () -> Unit,
    onSkipTrack: () -> Unit
) {
    val permissions = rememberMediaPermissions()
    val context = LocalContext.current

    val musicFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onUpdateConfig(config.copy(musicFolderUri = it.toString()))
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                OutlinedButton(
                    onClick = {
                        permissions.requestFor(MediaPermissionKind.MUSIC) {
                            musicFolderLauncher.launch(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (config.musicFolderUri != null) "Cambiar carpeta de música"
                        else "Elegir carpeta de música"
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceActionButton(
                    icon = if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (musicState.isPlaying) "Pausar" else "Reproducir",
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
    onReloadMedia: () -> Unit
) {
    val permissions = rememberMediaPermissions()

    SettingsScreen(
        viewModel = settingsViewModel,
        onBack = {},
        embedded = true,
        modifier = Modifier.fillMaxSize(),
        requestMediaAccess = { onGranted ->
            permissions.requestFor(MediaPermissionKind.PHOTOS_VIDEOS, onGranted)
        },
        requestMusicAccess = { onGranted ->
            permissions.requestFor(MediaPermissionKind.MUSIC, onGranted)
        },
        onMediaChanged = onReloadMedia
    )
}
