package com.dynamicframe.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.safeClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.data.local.LocalStorageBrowser
import com.dynamicframe.domain.model.*
import com.dynamicframe.presentation.browser.FolderBrowserDialog
import com.dynamicframe.presentation.browser.StoragePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    requestMediaAccess: ((onGranted: () -> Unit) -> Unit)? = null,
    requestMusicAccess: ((onGranted: () -> Unit) -> Unit)? = null,
    onMediaChanged: () -> Unit = {}
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val device = LocalDeviceProfile.current
    val context = LocalContext.current
    var showMediaFolderBrowser by remember { mutableStateOf(false) }
    var showMusicFolderBrowser by remember { mutableStateOf(false) }
    val useInAppBrowser = StoragePicker.shouldUseInAppBrowser(device.isTv, context)
    val systemPickerAvailable = StoragePicker.isSystemFolderPickerAvailable(context)

    val mediaFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addMediaFolder(it.toString())
            onMediaChanged()
        }
    }

    val musicFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setMusicFolder(it.toString())
        }
    }

    fun openMediaFolderPicker() {
        val action = {
            if (useInAppBrowser) showMediaFolderBrowser = true
            else mediaFolderLauncher.launch(null)
        }
        if (requestMediaAccess != null) requestMediaAccess(action) else action()
    }

    fun openMusicFolderPicker() {
        val action = {
            if (useInAppBrowser) showMusicFolderBrowser = true
            else musicFolderLauncher.launch(null)
        }
        if (requestMusicAccess != null) requestMusicAccess(action) else action()
    }

    FolderBrowserDialog(
        visible = showMediaFolderBrowser,
        title = "Carpeta de fotos y vídeos",
        onDismiss = { showMediaFolderBrowser = false },
        onSelectFolder = { uri ->
            viewModel.addMediaFolder(uri)
            onMediaChanged()
        }
    )

    FolderBrowserDialog(
        visible = showMusicFolderBrowser,
        title = "Carpeta de música",
        onDismiss = { showMusicFolderBrowser = false },
        onSelectFolder = { uri ->
            viewModel.setMusicFolder(uri)
        }
    )

    Scaffold(
        modifier = modifier.then(if (embedded) Modifier.fillMaxSize() else Modifier),
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text("Configuración") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = if (embedded) 0.dp else 16.dp)
        ) {
            if (embedded) {
                item {
                    Text(
                        "Ajustes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Los permisos se solicitan al elegir carpetas o medios.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // ── SLIDESHOW ────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Slideshow", Icons.Default.Slideshow)
            }

            item {
                // Intervalo entre fotos
                SettingsSliderItem(
                    title = "Tiempo por foto",
                    value = config.intervalSeconds.toFloat(),
                    valueRange = 3f..60f,
                    steps = 56,
                    valueLabel = "${config.intervalSeconds}s",
                    onValueChange = { viewModel.updateInterval(it.toInt()) }
                )
            }

            item {
                // Tipo de transición
                SettingsDropdownItem(
                    title = "Transición",
                    icon = Icons.Default.Animation,
                    currentValue = config.transition.displayName(),
                    options = TransitionType.entries.map { it.displayName() },
                    onSelect = { idx -> viewModel.updateTransition(TransitionType.entries[idx]) }
                )
            }

            item {
                SettingsSliderItem(
                    title = "Duración de transición",
                    value = config.transitionDurationMs.toFloat(),
                    valueRange = 800f..2800f,
                    steps = 19,
                    valueLabel = "${config.transitionDurationMs} ms",
                    onValueChange = {
                        viewModel.updateConfig(config.copy(transitionDurationMs = it.toInt()))
                    }
                )
            }

            if (device.isTv) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SettingsSectionHeader("Ajuste de pantalla TV", Icons.Default.Tv)
                    Text(
                        text = "Borde rojo/azul = límite de la pantalla. Zoom escala menús, botones y texto.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    SettingsSwitchItem(
                        title = "Mostrar borde de pantalla",
                        icon = Icons.Default.CropFree,
                        checked = config.showScreenBorder,
                        onCheckedChange = {
                            viewModel.updateConfig(config.copy(showScreenBorder = it))
                        }
                    )
                }
                item {
                    SettingsSliderItem(
                        title = "Zoom de interfaz",
                        value = config.uiScale,
                        valueRange = 0.75f..1.25f,
                        valueLabel = "${(config.uiScale * 100).toInt()}%",
                        onValueChange = { viewModel.updateConfig(config.copy(uiScale = it)) },
                        steps = 9
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("Modo reproducción", Icons.Default.Fullscreen)
                Text(
                    text = "Solo aplica en pantalla completa. En modo solo imagen, toca la pantalla para ver controles.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Solo imagen (inmersivo)",
                    icon = Icons.Default.Fullscreen,
                    checked = config.playbackImmersiveMode,
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackImmersiveMode = it)) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Mostrar reloj al reproducir",
                    icon = Icons.Default.Schedule,
                    checked = config.playbackShowClock,
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowClock = it)) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Mostrar controles y música",
                    icon = Icons.Default.Tune,
                    checked = config.playbackShowOverlay,
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowOverlay = it)) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Mezclar aleatoriamente",
                    icon = Icons.Default.Shuffle,
                    checked = config.shuffle,
                    onCheckedChange = { viewModel.toggleShuffle(it) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Reproducir en bucle",
                    icon = Icons.Default.Repeat,
                    checked = config.loop,
                    onCheckedChange = { viewModel.updateConfig(config.copy(loop = it)) }
                )
            }

            // ── CARPETAS DE FOTOS/VIDEOS ─────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Carpetas de fotos y videos", Icons.Default.Folder)
                Text(
                    text = if (useInAppBrowser) {
                        "Explorador integrado (no requiere otra app). Sin carpeta = galería del dispositivo."
                    } else {
                        "Incluye subcarpetas. Sin carpeta = galería del dispositivo."
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                OutlinedButton(
                    onClick = { openMediaFolderPicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(if (device.isTv) 24.dp else 20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (useInAppBrowser) "Explorar carpetas (integrado)"
                        else if (device.isTv) "Elegir carpeta de medios"
                        else "Carpeta de medios"
                    )
                }
            }

            if (!useInAppBrowser && systemPickerAvailable) {
                item {
                    OutlinedButton(
                        onClick = {
                            val launch = { mediaFolderLauncher.launch(null) }
                            if (requestMediaAccess != null) requestMediaAccess(launch) else launch()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Selector del sistema")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val grant = { onMediaChanged() }
                        if (requestMediaAccess != null) requestMediaAccess(grant) else grant()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(if (device.isTv) 24.dp else 20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (device.isTv) "Usar galería del dispositivo" else "Galería del dispositivo")
                }
            }

            items(config.mediaFolderUris) { uri ->
                FolderChip(
                    label = folderLabel(uri),
                    onRemove = {
                        viewModel.removeMediaFolder(uri)
                        onMediaChanged()
                    }
                )
            }

            item {
                SettingsDropdownItem(
                    title = "Mostrar",
                    icon = Icons.Default.Filter,
                    currentValue = config.mediaContentFilter.displayName(),
                    options = MediaContentFilter.entries.map { it.displayName() },
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(mediaContentFilter = MediaContentFilter.entries[idx]))
                        onMediaChanged()
                    }
                )
            }

            // ── RELOJ ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Reloj y fecha", Icons.Default.AccessTime)
            }

            item {
                SettingsSwitchItem(
                    title = "Mostrar reloj",
                    icon = Icons.Default.Schedule,
                    checked = config.showClock,
                    onCheckedChange = { viewModel.toggleClock(it) }
                )
            }

            if (config.showClock) {
                item {
                    SettingsSwitchItem(
                        title = "Mostrar fecha",
                        icon = Icons.Default.CalendarToday,
                        checked = config.showDate,
                        onCheckedChange = { viewModel.updateConfig(config.copy(showDate = it)) }
                    )
                }

                item {
                    SettingsDropdownItem(
                        title = "Posición del reloj",
                        icon = Icons.Default.MyLocation,
                        currentValue = config.clockPosition.displayName(),
                        options = ClockPosition.entries.map { it.displayName() },
                        onSelect = { idx -> viewModel.updateConfig(config.copy(clockPosition = ClockPosition.entries[idx])) }
                    )
                }
            }

            // ── MÚSICA ───────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Música de fondo", Icons.Default.MusicNote)
            }

            item {
                SettingsSliderItem(
                    title = "Volumen de música",
                    value = config.musicVolume,
                    valueRange = 0f..1f,
                    valueLabel = "${(config.musicVolume * 100).toInt()}%",
                    onValueChange = { viewModel.updateMusicVolume(it) }
                )
            }

            item {
                SettingsDropdownItem(
                    title = "Fuente de música",
                    icon = Icons.Default.LibraryMusic,
                    currentValue = config.musicSourceType.displayName(),
                    options = MusicSourceType.entries.map { it.displayName() },
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(musicSourceType = MusicSourceType.entries[idx]))
                    }
                )
            }

            if (config.musicSourceType == MusicSourceType.LOCAL_FOLDER) {
                item {
                    OutlinedButton(
                        onClick = { openMusicFolderPicker() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (config.musicFolderUri != null) folderLabel(config.musicFolderUri!!)
                            else "Elegir carpeta de música"
                        )
                    }
                }
            }

            if (config.musicSourceType == MusicSourceType.THEME) {
                item {
                    SettingsDropdownItem(
                        title = "Tema / ambiente",
                        icon = Icons.Default.Palette,
                        currentValue = config.musicTheme.displayName(),
                        options = MusicTheme.entries.map { it.displayName() },
                        onSelect = { idx ->
                            viewModel.updateConfig(config.copy(musicTheme = MusicTheme.entries[idx]))
                        }
                    )
                }
            }

            if (config.musicSourceType == MusicSourceType.SPOTIFY) {
                item {
                    SettingsTextFieldItem(
                        title = "URL playlist Spotify",
                        value = config.spotifyPlaylistUrl,
                        placeholder = "https://open.spotify.com/playlist/...",
                        onValueChange = { viewModel.updateConfig(config.copy(spotifyPlaylistUrl = it)) }
                    )
                    Text(
                        text = "Reproducción Spotify: próximamente (requiere Premium).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            if (config.musicSourceType == MusicSourceType.YOUTUBE) {
                item {
                    SettingsTextFieldItem(
                        title = "URL lista YouTube",
                        value = config.youtubePlaylistUrl,
                        placeholder = "https://youtube.com/playlist?list=...",
                        onValueChange = { viewModel.updateConfig(config.copy(youtubePlaylistUrl = it)) }
                    )
                    Text(
                        text = "Reproducción YouTube: próximamente.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            item {
                SettingsSwitchItem(
                    title = "Música aleatoria",
                    icon = Icons.Default.Shuffle,
                    checked = config.musicShuffle,
                    onCheckedChange = { viewModel.updateConfig(config.copy(musicShuffle = it)) }
                )
            }

            // ── VIDEO ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Videos", Icons.Default.VideoLibrary)
            }

            item {
                SettingsSwitchItem(
                    title = "Silenciar audio de videos",
                    icon = Icons.Default.VolumeOff,
                    checked = config.muteVideoAudio,
                    onCheckedChange = { viewModel.updateConfig(config.copy(muteVideoAudio = it)) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Reproducir video completo",
                    icon = Icons.Default.Fullscreen,
                    checked = config.videoPlayFull,
                    onCheckedChange = { viewModel.updateConfig(config.copy(videoPlayFull = it)) }
                )
            }

            item {
                SettingsDropdownItem(
                    title = "Música durante video",
                    icon = Icons.Default.VolumeDown,
                    currentValue = config.videoMusicBehavior.displayName(),
                    options = VideoMusicBehavior.entries.map { it.displayName() },
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(videoMusicBehavior = VideoMusicBehavior.entries[idx]))
                    }
                )
            }

            if (config.videoMusicBehavior == VideoMusicBehavior.DUCK) {
                item {
                    SettingsSliderItem(
                        title = "Volumen bajo durante video",
                        value = config.duckedMusicVolume,
                        valueRange = 0f..0.5f,
                        valueLabel = "${(config.duckedMusicVolume * 100).toInt()}%",
                        onValueChange = {
                            viewModel.updateConfig(config.copy(duckedMusicVolume = it))
                        }
                    )
                }
            }

            // ── ÁLBUMES ──────────────────────────────────────────────────────
            if (albums.isNotEmpty() && config.mediaFolderUris.isEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SettingsSectionHeader("Álbumes seleccionados", Icons.Default.PhotoAlbum)
                    Text(
                        text = "Vacío = todos los medios",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(albums) { album ->
                            val selected = config.selectedAlbumIds.contains(album.id)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newIds = if (selected)
                                        config.selectedAlbumIds - album.id
                                    else
                                        config.selectedAlbumIds + album.id
                                    viewModel.updateSelectedAlbums(newIds)
                                },
                                label = { Text("${album.name} (${album.itemCount})") },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // ── SISTEMA ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Sistema", Icons.Default.Settings)
            }

            item {
                SettingsSwitchItem(
                    title = "Iniciar automáticamente al arrancar",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = config.autoStartOnBoot,
                    onCheckedChange = { viewModel.updateConfig(config.copy(autoStartOnBoot = it)) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Componentes reutilizables ─────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun SettingsSwitchItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val device = LocalDeviceProfile.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable { onCheckedChange(!checked) }
            .padding(vertical = if (device.isTv) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(if (device.isTv) 26.dp else 22.dp)
            )
            Text(text = title, fontSize = if (device.isTv) 16.sp else 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0
) {
    val device = LocalDeviceProfile.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title)
            Text(
                text = valueLabel,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (device.isTv) Modifier.focusable() else Modifier)
        )
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    icon: ImageVector,
    currentValue: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val device = LocalDeviceProfile.current

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeClickable { expanded = true }
                .padding(vertical = if (device.isTv) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (device.isTv) 26.dp else 22.dp)
                )
                Text(text = title, fontSize = if (device.isTv) 16.sp else 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentValue,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = if (device.isTv) 15.sp else 14.sp
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
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

// ── Extensiones de display ────────────────────────────────────────────────────

fun TransitionType.displayName() = when (this) {
    TransitionType.CROSSFADE -> "Fundido suave (recomendado)"
    TransitionType.FADE -> "Fundido clásico"
    TransitionType.DISSOLVE -> "Disolvencia lenta"
    TransitionType.KEN_BURNS -> "Ken Burns (zoom lento)"
    TransitionType.BLUR_FADE -> "Fundido con zoom"
    TransitionType.SLIDE_LEFT -> "Deslizar izquierda"
    TransitionType.SLIDE_RIGHT -> "Deslizar derecha"
    TransitionType.SLIDE_UP -> "Deslizar arriba"
    TransitionType.SLIDE_DOWN -> "Deslizar abajo"
    TransitionType.ZOOM_IN -> "Zoom acercar"
    TransitionType.ZOOM_OUT -> "Zoom alejar"
    TransitionType.ROTATE -> "Rotación suave"
    TransitionType.FLIP_HORIZONTAL -> "Volteo horizontal"
    TransitionType.FLIP_VERTICAL -> "Volteo vertical"
    TransitionType.WIPE_LEFT -> "Barrido izquierda"
    TransitionType.WIPE_RIGHT -> "Barrido derecha"
    TransitionType.DEPTH -> "Profundidad 3D"
    TransitionType.STACK -> "Apilado"
    TransitionType.PARALLAX -> "Parallax"
    TransitionType.CUBE -> "Cubo"
    TransitionType.NONE -> "Sin transición"
}

fun ClockPosition.displayName() = when (this) {
    ClockPosition.TOP_LEFT -> "Arriba izquierda"
    ClockPosition.TOP_RIGHT -> "Arriba derecha"
    ClockPosition.BOTTOM_LEFT -> "Abajo izquierda"
    ClockPosition.BOTTOM_RIGHT -> "Abajo derecha"
    ClockPosition.CENTER -> "Centro"
}

fun MediaContentFilter.displayName() = when (this) {
    MediaContentFilter.ALL -> "Fotos y videos"
    MediaContentFilter.PHOTOS_ONLY -> "Solo fotos"
    MediaContentFilter.VIDEOS_ONLY -> "Solo videos"
}

fun MusicSourceType.displayName() = when (this) {
    MusicSourceType.LOCAL_FOLDER -> "Carpeta local"
    MusicSourceType.DEVICE_LIBRARY -> "Biblioteca del dispositivo"
    MusicSourceType.THEME -> "Tema / ambiente"
    MusicSourceType.SPOTIFY -> "Spotify"
    MusicSourceType.YOUTUBE -> "YouTube"
}

fun MusicTheme.displayName() = when (this) {
    MusicTheme.RELAX -> "Relajante"
    MusicTheme.ENERGETIC -> "Energético"
    MusicTheme.CLASSIC -> "Clásico"
    MusicTheme.NATURE -> "Naturaleza"
    MusicTheme.AMBIENT -> "Ambiente"
}

fun VideoMusicBehavior.displayName() = when (this) {
    VideoMusicBehavior.PAUSE -> "Pausar música"
    VideoMusicBehavior.DUCK -> "Bajar volumen"
}

fun folderLabel(uriString: String): String = LocalStorageBrowser.folderDisplayName(uriString)

@Composable
fun FolderChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, maxLines = 1, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Quitar carpeta")
        }
    }
}

@Composable
fun SettingsTextFieldItem(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = title, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
