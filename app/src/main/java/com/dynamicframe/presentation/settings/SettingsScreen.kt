package com.dynamicframe.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.safeClickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.presentation.common.TvPickerChip
import com.dynamicframe.presentation.common.TvPickerChipStyle
import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.presentation.browser.FolderBrowserDialog
import com.dynamicframe.presentation.browser.StoragePicker
import com.dynamicframe.presentation.permissions.MediaPermissionDeniedBanner
import com.dynamicframe.ui.theme.NostalgiaActionButton
import com.dynamicframe.ui.theme.TvStepperChip
import com.dynamicframe.ui.theme.PlaybackLetterboxBackground
import com.dynamicframe.ui.theme.displayName as playbackBackgroundDisplayName
import com.dynamicframe.ui.theme.AppVersionLabel
import com.dynamicframe.ui.theme.MemoriaLine
import com.dynamicframe.ui.theme.MemoriaPurple

private enum class FolderTarget { PHOTO, VIDEO, MUSIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    showPermissionDenied: Boolean = false,
    requestMediaAccess: ((onGranted: () -> Unit) -> Unit)? = null,
    requestMusicAccess: ((onGranted: () -> Unit) -> Unit)? = null,
    onMediaChanged: () -> Unit = {}
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val debugModeEnabled by viewModel.debugModeEnabled.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val device = LocalDeviceProfile.current
    val context = LocalContext.current

    var folderBrowserTarget by remember { mutableStateOf<FolderTarget?>(null) }
    val useInAppBrowser = StoragePicker.shouldUseInAppBrowser(device.isTv, context)
    val systemPickerAvailable = StoragePicker.isSystemFolderPickerAvailable(context)

    var pendingFolderTarget by remember { mutableStateOf<FolderTarget?>(null) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            when (pendingFolderTarget) {
                FolderTarget.PHOTO -> {
                    viewModel.addPhotoFolder(it.toString())
                    onMediaChanged()
                }
                FolderTarget.VIDEO -> {
                    viewModel.addVideoFolder(it.toString())
                    onMediaChanged()
                }
                FolderTarget.MUSIC -> viewModel.addMusicFolder(it.toString())
                null -> Unit
            }
            pendingFolderTarget = null
        }
    }

    val backgroundImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateConfig(
                config.copy(
                    playbackBackgroundType = PlaybackBackgroundType.CUSTOM_IMAGE,
                    playbackBackgroundImageUri = it.toString()
                )
            )
        }
    }

    fun openFolderPicker(target: FolderTarget, requestAccess: ((() -> Unit) -> Unit)? = null) {
        val action = {
            pendingFolderTarget = target
            if (useInAppBrowser) folderBrowserTarget = target
            else folderLauncher.launch(null)
        }
        if (requestAccess != null) requestAccess(action) else action()
    }

    FolderBrowserDialog(
        visible = folderBrowserTarget == FolderTarget.PHOTO,
        title = "Carpeta de fotos",
        onDismiss = { folderBrowserTarget = null },
        onSelectFolder = { uri ->
            viewModel.addPhotoFolder(uri)
            onMediaChanged()
            folderBrowserTarget = null
        },
        listRoots = viewModel::storageRoots,
        listSubfolders = viewModel::storageSubfolders
    )

    FolderBrowserDialog(
        visible = folderBrowserTarget == FolderTarget.VIDEO,
        title = "Carpeta de videos",
        onDismiss = { folderBrowserTarget = null },
        onSelectFolder = { uri ->
            viewModel.addVideoFolder(uri)
            onMediaChanged()
            folderBrowserTarget = null
        },
        listRoots = viewModel::storageRoots,
        listSubfolders = viewModel::storageSubfolders
    )

    FolderBrowserDialog(
        visible = folderBrowserTarget == FolderTarget.MUSIC,
        title = "Carpeta de música",
        onDismiss = { folderBrowserTarget = null },
        onSelectFolder = { uri ->
            viewModel.addMusicFolder(uri)
            folderBrowserTarget = null
        },
        listRoots = viewModel::storageRoots,
        listSubfolders = viewModel::storageSubfolders
    )

    Scaffold(
        modifier = modifier.then(if (embedded) Modifier.fillMaxSize() else Modifier),
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text("Configuración") },
                    navigationIcon = {
                        Row(
                            modifier = Modifier
                                .safeClickable(onClick = onBack)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                            if (device.isTv) {
                                Text("Volver", fontSize = 14.sp)
                            }
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
                .padding(horizontal = 16.dp)
                .then(if (device.isTv) Modifier.focusGroup() else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
        ) {
            // Cabecera (solo en modo embebido)
            item { Spacer(Modifier.height(4.dp)) }

            if (embedded) {
                item {
                    Text("Ajustes", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Los permisos se solicitan al elegir carpetas o medios.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
            }

            if (showPermissionDenied) {
                item {
                    MediaPermissionDeniedBanner(
                        message = "Activa el acceso a fotos, vídeos o música en los ajustes del sistema para elegir carpetas."
                    )
                }
            }

            // ── FOTOS ─────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Fotos", Icons.Default.Photo)
                Text(
                    text = "Elige las carpetas con tus fotos. Deja vacío para usar la galería del dispositivo.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                NostalgiaActionButton(
                    text = if (useInAppBrowser) "Añadir carpeta de fotos" else "Elegir carpeta de fotos",
                    icon = Icons.Default.Photo,
                    onClick = { openFolderPicker(FolderTarget.PHOTO, requestMediaAccess) }
                )
            }
            items(config.photoFolderUris, key = { "photo:$it" }) { uri ->
                FolderChip(label = viewModel.folderLabel(uri), uri = uri, onRemove = {
                    viewModel.removePhotoFolder(uri); onMediaChanged()
                })
            }
            item {
                NostalgiaActionButton(
                    text = if (device.isTv) "Usar galería del dispositivo" else "Galería del dispositivo",
                    icon = Icons.Default.Collections,
                    onClick = {
                        val grant = { onMediaChanged() }
                        if (requestMediaAccess != null) requestMediaAccess(grant) else grant()
                    }
                )
            }
            if (albums.isNotEmpty() && !config.hasCustomMediaFolders()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Álbumes seleccionados", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp))
                    Text("Vacío = todos los medios", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                item {
                    if (device.isTv) {
                        albums.forEach { album ->
                            val selected = config.selectedAlbumIds.contains(album.id)
                            SettingsAlbumSelectRow(
                                label = "${album.name} (${album.itemCount})",
                                selected = selected,
                                onClick = {
                                    val newIds = if (selected) config.selectedAlbumIds - album.id
                                    else config.selectedAlbumIds + album.id
                                    viewModel.updateSelectedAlbums(newIds)
                                }
                            )
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(albums) { album ->
                                val selected = config.selectedAlbumIds.contains(album.id)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val newIds = if (selected) config.selectedAlbumIds - album.id
                                        else config.selectedAlbumIds + album.id
                                        viewModel.updateSelectedAlbums(newIds)
                                    },
                                    label = { Text("${album.name} (${album.itemCount})") },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsSwitchItem(
                    title = "Fotos aleatorias",
                    icon = Icons.Default.Photo,
                    checked = config.photoShuffle,
                    note = "Reproduce las fotos en orden aleatorio.",
                    onCheckedChange = viewModel::updatePhotoShuffle
                )
            }

            // ── VIDEOS ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Videos", Icons.Default.Videocam)
                Text(
                    text = "Elige las carpetas con tus videos y configura su reproducción.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                NostalgiaActionButton(
                    text = if (useInAppBrowser) "Añadir carpeta de videos" else "Elegir carpeta de videos",
                    icon = Icons.Default.Movie,
                    onClick = { openFolderPicker(FolderTarget.VIDEO, requestMediaAccess) }
                )
            }
            items(config.videoFolderUris, key = { "video:$it" }) { uri ->
                FolderChip(label = viewModel.folderLabel(uri), uri = uri, onRemove = {
                    viewModel.removeVideoFolder(uri); onMediaChanged()
                })
            }
            item {
                SettingsSwitchItem(
                    title = "Videos aleatorios",
                    icon = Icons.Default.Videocam,
                    checked = config.videoShuffle,
                    note = "Reproduce los videos en orden aleatorio.",
                    onCheckedChange = viewModel::updateVideoShuffle
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Silenciar audio de videos",
                    icon = Icons.Default.VolumeOff,
                    checked = config.muteVideoAudio,
                    note = "Silencia el audio original de los videos del slideshow.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(muteVideoAudio = it)) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Reproducir video completo",
                    icon = Icons.Default.Fullscreen,
                    checked = config.videoPlayFull,
                    note = "Espera a que el video termine antes de pasar al siguiente medio.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(videoPlayFull = it)) }
                )
            }

            // ── SLIDESHOW ─────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Slideshow", Icons.Default.Slideshow)
            }
            item {
                SettingsSliderItem(
                    title = "Tiempo por foto",
                    value = config.intervalSeconds.toFloat(),
                    valueRange = 3f..60f, steps = 56,
                    valueLabel = "${config.intervalSeconds}s",
                    note = "Segundos que se muestra cada foto antes de pasar a la siguiente.",
                    onValueChange = { viewModel.updateInterval(it.toInt()) }
                )
            }
            item {
                SettingsDropdownItem(
                    title = "Transición",
                    icon = Icons.Default.Animation,
                    currentValue = config.transition.displayName(),
                    options = TransitionType.entries.map { it.displayName() },
                    note = "Efecto visual entre una foto y la siguiente.",
                    onSelect = { idx -> viewModel.updateTransition(TransitionType.entries[idx]) }
                )
            }
            item {
                SettingsSliderItem(
                    title = "Duración de transición",
                    value = config.transitionDurationMs.toFloat(),
                    valueRange = 800f..2800f, steps = 19,
                    valueLabel = "${config.transitionDurationMs} ms",
                    note = "Duración del efecto de transición en milisegundos.",
                    onValueChange = { viewModel.updateConfig(config.copy(transitionDurationMs = it.toInt())) }
                )
            }
            item {
                SettingsDropdownItem(
                    title = "Mostrar",
                    icon = Icons.Default.Filter,
                    currentValue = config.mediaContentFilter.displayName(),
                    options = MediaContentFilter.entries.map { it.displayName() },
                    note = "Filtra si el slideshow muestra fotos, videos o ambos.",
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(mediaContentFilter = MediaContentFilter.entries[idx]))
                        onMediaChanged()
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Reproducir en bucle",
                    icon = Icons.Default.Repeat,
                    checked = config.loop,
                    note = "Vuelve al inicio cuando se terminen todos los medios.",
                    onCheckedChange = viewModel::updateLoop
                )
            }

            // ── MÚSICA DE FONDO ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Música de fondo", Icons.Default.MusicNote)
            }
            item {
                SettingsDropdownItem(
                    title = "Fuente de música",
                    icon = Icons.Default.LibraryMusic,
                    currentValue = config.musicSourceType.displayName(),
                    options = MusicSourceType.entries.map { it.displayName() },
                    note = "De dónde proviene la música que suena durante el slideshow.",
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(musicSourceType = MusicSourceType.entries[idx]))
                    }
                )
            }
            item {
                SettingsSliderItem(
                    title = "Volumen de música",
                    value = config.musicVolume,
                    valueRange = 0f..1f,
                    valueLabel = "${(config.musicVolume * 100).toInt()}%",
                    note = "Volumen general de la música de fondo.",
                    onValueChange = { viewModel.updateMusicVolume(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Música aleatoria",
                    icon = Icons.Default.Shuffle,
                    checked = config.musicShuffle,
                    note = "Reproduce las canciones en orden aleatorio.",
                    onCheckedChange = viewModel::updateMusicShuffle
                )
            }
            if (config.musicSourceType == MusicSourceType.LOCAL_FOLDER) {
                item {
                    NostalgiaActionButton(
                        text = if (useInAppBrowser) "Añadir carpeta de música" else "Elegir carpeta de música",
                        icon = Icons.Default.FolderOpen,
                        onClick = { openFolderPicker(FolderTarget.MUSIC, requestMusicAccess) }
                    )
                }
                items(config.musicFolderUris, key = { "music:$it" }) { uri ->
                    FolderChip(label = viewModel.folderLabel(uri), uri = uri,
                        onRemove = { viewModel.removeMusicFolder(uri) })
                }
            }
            if (config.musicSourceType == MusicSourceType.THEME) {
                item {
                    SettingsDropdownItem(
                        title = "Tema / ambiente",
                        icon = Icons.Default.Palette,
                        currentValue = config.musicTheme.displayName(),
                        options = MusicTheme.entries.map { it.displayName() },
                        note = "Estilo musical generado para ambientar el slideshow.",
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
                    Text("Reproducción Spotify: próximamente (requiere Premium).", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("Reproducción YouTube: próximamente.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            item {
                SettingsDropdownItem(
                    title = "Música durante video",
                    icon = Icons.Default.VolumeDown,
                    currentValue = config.videoMusicBehavior.displayName(),
                    options = VideoMusicBehavior.entries.map { it.displayName() },
                    note = "Qué hace la música de fondo mientras se reproduce un video.",
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(videoMusicBehavior = VideoMusicBehavior.entries[idx]))
                    }
                )
            }
            if (config.videoMusicBehavior == VideoMusicBehavior.DUCK) {
                item {
                    SettingsSliderItem(
                        title = "Volumen durante video",
                        value = config.duckedMusicVolume,
                        valueRange = 0f..0.5f,
                        valueLabel = "${(config.duckedMusicVolume * 100).toInt()}%",
                        note = "Volumen reducido de la música mientras el video está activo.",
                        onValueChange = { viewModel.updateConfig(config.copy(duckedMusicVolume = it)) }
                    )
                }
            }

            // ── VISUAL EN REPRODUCCIÓN ────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Visual en reproducción", Icons.Default.Fullscreen)
                Text(
                    text = "Toca/OK en pantalla completa para pausar. Los controles se ocultan solos.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Marco dorado (estilo cuadro)",
                    icon = Icons.Default.FilterFrames,
                    checked = config.showPictureFrame,
                    note = "Muestra un marco dorado decorativo alrededor de cada foto.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(showPictureFrame = it)) }
                )
            }
            item {
                SettingsSliderItem(
                    title = "Grosor del marco dorado",
                    value = config.playbackPictureFrameScale,
                    valueRange = 0.5f..1.5f,
                    valueLabel = "${(config.playbackPictureFrameScale * 100).toInt()}%",
                    note = "Ajusta el grosor del borde decorativo.",
                    onValueChange = { viewModel.updateConfig(config.copy(playbackPictureFrameScale = it)) },
                    steps = 9
                )
            }
            item {
                SettingsSliderItem(
                    title = "Zoom en pantalla completa",
                    value = config.playbackContentZoom,
                    valueRange = 0.75f..1f,
                    valueLabel = "${(config.playbackContentZoom * 100).toInt()}%",
                    note = "Zoom aplicado a cada foto o video durante la reproducción.",
                    onValueChange = { viewModel.updateConfig(config.copy(playbackContentZoom = it)) },
                    steps = 4
                )
            }
            item {
                SettingsDropdownItem(
                    title = "Fondo letterbox",
                    icon = Icons.Default.Wallpaper,
                    currentValue = config.playbackBackgroundType.playbackBackgroundDisplayName(),
                    options = PlaybackBackgroundType.entries.map { it.playbackBackgroundDisplayName() },
                    note = "Rellena el espacio alrededor de fotos que no ocupan toda la pantalla.",
                    onSelect = { idx ->
                        viewModel.updateConfig(config.copy(playbackBackgroundType = PlaybackBackgroundType.entries[idx]))
                    }
                )
            }
            item {
                PlaybackBackgroundPreviewRow(
                    selected = config.playbackBackgroundType,
                    onSelect = { viewModel.updateConfig(config.copy(playbackBackgroundType = it)) }
                )
            }
            if (config.playbackBackgroundType == PlaybackBackgroundType.CUSTOM_IMAGE) {
                item {
                    NostalgiaActionButton(text = "Elegir mi imagen de fondo",
                        icon = Icons.Default.Image,
                        onClick = { backgroundImageLauncher.launch(arrayOf("image/*")) })
                }
                if (config.playbackBackgroundImageUri.isNotBlank()) {
                    item {
                        Text("Imagen seleccionada", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
            }
            item {
                SettingsSwitchItem(
                    title = "Borde zona segura",
                    icon = Icons.Default.CropFree,
                    checked = config.playbackShowSafeBorder,
                    note = "Muestra un borde visual para comprobar que la imagen no queda recortada.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowSafeBorder = it)) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Solo imagen (inmersivo)",
                    icon = Icons.Default.Fullscreen,
                    checked = config.playbackImmersiveMode,
                    note = "Oculta todos los controles para máxima inmersión.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackImmersiveMode = it)) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Mostrar reloj al reproducir",
                    icon = Icons.Default.Schedule,
                    checked = config.playbackShowClock,
                    note = "Superpone el reloj encima de las fotos durante el slideshow.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowClock = it)) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Mostrar controles y música",
                    icon = Icons.Default.Tune,
                    checked = config.playbackShowOverlay,
                    note = "Muestra el título de la canción y los controles de reproducción.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowOverlay = it)) }
                )
            }

            // ── RELOJ Y FECHA ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Reloj y fecha", Icons.Default.AccessTime)
            }
            item {
                SettingsSwitchItem(
                    title = "Mostrar reloj",
                    icon = Icons.Default.Schedule,
                    checked = config.showClock,
                    note = "Muestra el reloj en el panel principal de la app.",
                    onCheckedChange = { viewModel.toggleClock(it) }
                )
            }
            if (config.showClock) {
                item {
                    SettingsSwitchItem(
                        title = "Mostrar fecha",
                        icon = Icons.Default.CalendarToday,
                        checked = config.showDate,
                        note = "Añade la fecha junto al reloj.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(showDate = it)) }
                    )
                }
                item {
                    SettingsDropdownItem(
                        title = "Posición del reloj",
                        icon = Icons.Default.MyLocation,
                        currentValue = config.clockPosition.displayName(),
                        options = ClockPosition.entries.map { it.displayName() },
                        note = "Esquina donde aparece el reloj en pantalla.",
                        onSelect = { idx -> viewModel.updateConfig(config.copy(clockPosition = ClockPosition.entries[idx])) }
                    )
                }
            }

            // ── PANTALLA TV ───────────────────────────────────────────────────
            if (device.isTv) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SettingsSectionHeader("Pantalla TV", Icons.Default.Tv)
                    Text(
                        text = "El borde de pantalla muestra los límites reales de tu TV para ajustar el recorte.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    SettingsSwitchItem(
                        title = "Mostrar borde de pantalla",
                        icon = Icons.Default.CropFree,
                        checked = config.showScreenBorder,
                        note = "Activa para ver el límite exacto de la imagen en tu TV.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(showScreenBorder = it)) }
                    )
                }
                item {
                    SettingsSliderItem(
                        title = "Zoom de interfaz",
                        value = config.uiScale,
                        valueRange = 0.75f..1.25f,
                        valueLabel = "${(config.uiScale * 100).toInt()}%",
                        note = "Escala todos los elementos de la interfaz para adaptarlos a tu TV.",
                        onValueChange = { viewModel.updateConfig(config.copy(uiScale = it)) },
                        steps = 9
                    )
                }
            }

            // ── SISTEMA ───────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Sistema", Icons.Default.Settings)
            }
            item {
                SettingsSwitchItem(
                    title = "Modo depuración",
                    icon = Icons.Default.BugReport,
                    checked = debugModeEnabled,
                    note = if (debugModeEnabled) "Consola flotante DBG activa. Toca el botón morado para ver el log."
                    else "Registra acciones, errores y navegación (útil al reportar fallos).",
                    onCheckedChange = { viewModel.updateDebugMode(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Iniciar automáticamente al arrancar",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = config.autoStartOnBoot,
                    note = "Arranca el slideshow automáticamente al encender el dispositivo.",
                    onCheckedChange = { viewModel.updateConfig(config.copy(autoStartOnBoot = it)) }
                )
            }

            // Versión al final — no navigable, solo informativo
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .focusProperties { canFocus = false }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp))
                        Text("Versión", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    AppVersionLabel(showBuildCode = true, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
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
    onCheckedChange: (Boolean) -> Unit,
    note: String = ""
) {
    val device = LocalDeviceProfile.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeClickable(interactionSource = source) { onCheckedChange(!checked) }
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
            if (device.isTv) {
                Icon(
                    imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (checked) "Activado" else "Desactivado",
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
        if (note.isNotBlank() && (!device.isTv || focused)) {
            Text(
                text = note,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 38.dp, bottom = 4.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    note: String = ""
) {
    val device = LocalDeviceProfile.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = title)
            Text(text = valueLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        if (device.isTv) {
            val step = if (steps > 0) (valueRange.endInclusive - valueRange.start) / (steps + 1)
            else (valueRange.endInclusive - valueRange.start) / 10f
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvStepperChip(icon = Icons.Default.Remove, desc = "Reducir",
                    onClick = { onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive)) })
                Text(valueLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 56.dp))
                TvStepperChip(icon = Icons.Default.Add, desc = "Aumentar",
                    onClick = { onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive)) })
            }
        } else {
            Slider(value = value, onValueChange = onValueChange, valueRange = valueRange,
                steps = steps, modifier = Modifier.fillMaxWidth())
        }
        if (note.isNotBlank()) {
            Text(text = note, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
        }
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    icon: ImageVector,
    currentValue: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    note: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val device = LocalDeviceProfile.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (device.isTv) {
            TvPickerChip(
                title = title,
                icon = icon,
                displayValue = currentValue,
                currentValue = currentValue,
                options = options,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
                style = TvPickerChipStyle.Full
            )
        } else {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeClickable { expanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = title,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp))
                        Text(text = title, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentValue, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
                    }
                }
            }
        }
        if (note.isNotBlank()) {
            Text(text = note, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = if (device.isTv) 0.dp else 34.dp, bottom = 4.dp, end = 8.dp))
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

/** Etiqueta corta para chips del panel álbum activo. */
fun TransitionType.dashboardLabel(): String = when (this) {
    TransitionType.CROSSFADE -> "Fundido"
    TransitionType.FADE -> "Clásico"
    TransitionType.DISSOLVE -> "Disol."
    TransitionType.KEN_BURNS -> "Ken Burns"
    TransitionType.BLUR_FADE -> "Zoom"
    TransitionType.SLIDE_LEFT -> "← Izq."
    TransitionType.SLIDE_RIGHT -> "→ Der."
    TransitionType.SLIDE_UP -> "↑"
    TransitionType.SLIDE_DOWN -> "↓"
    TransitionType.ZOOM_IN -> "Zoom +"
    TransitionType.ZOOM_OUT -> "Zoom -"
    TransitionType.ROTATE -> "Girar"
    TransitionType.FLIP_HORIZONTAL -> "Flip H"
    TransitionType.FLIP_VERTICAL -> "Flip V"
    TransitionType.WIPE_LEFT -> "Wipe ←"
    TransitionType.WIPE_RIGHT -> "Wipe →"
    TransitionType.DEPTH -> "3D"
    TransitionType.STACK -> "Pila"
    TransitionType.PARALLAX -> "Parallax"
    TransitionType.CUBE -> "Cubo"
    TransitionType.NONE -> "Ninguna"
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

@Composable
fun FolderChip(label: String, uri: String = "", onRemove: () -> Unit) {
    val device = LocalDeviceProfile.current
    val displayPath = remember(uri) { uriToDisplayPath(uri) }

    if (device.isTv) {
        // En TV: fila completa focusable, OK = quitar carpeta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .safeClickable(onClick = onRemove),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp))
                Column {
                    Text(text = label, fontSize = 15.sp, maxLines = 1)
                    if (displayPath.isNotBlank() && displayPath != label) {
                        Text(text = displayPath, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1)
                    }
                }
            }
            Text("Quitar (OK)", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = label, maxLines = 1)
                if (displayPath.isNotBlank() && displayPath != label) {
                    Text(text = displayPath, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 2)
                }
            }
            Row(
                modifier = Modifier
                    .safeClickable(onClick = onRemove)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Quitar carpeta",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun uriToDisplayPath(uri: String): String {
    if (uri.isBlank()) return ""
    return try {
        val decoded = java.net.URLDecoder.decode(uri, "UTF-8")
        when {
            decoded.startsWith("file://") -> decoded.removePrefix("file://")
            decoded.startsWith("content://") -> {
                // Extrae la ruta legible de content URIs del tipo
                // content://com.android.externalstorage.documents/tree/...
                val afterTree = decoded.substringAfter("tree/", "")
                    .substringAfter("document/", decoded)
                if (afterTree.isNotBlank()) {
                    java.net.URLDecoder.decode(afterTree, "UTF-8")
                        .replace(":", "/")
                } else decoded
            }
            else -> decoded
        }
    } catch (e: Exception) {
        uri
    }
}

@Composable
fun SettingsAlbumSelectRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp)
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (selected) "Seleccionado" else "No seleccionado",
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SettingsTextFieldItem(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val device = LocalDeviceProfile.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = title, modifier = Modifier.padding(bottom = 4.dp))
        if (device.isTv) {
            Text(
                text = value.ifBlank { placeholder },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (value.isBlank()) 0.45f else 0.85f),
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Text(
                text = "Edición de URL: disponible en móvil.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun PlaybackBackgroundPreviewRow(
    selected: PlaybackBackgroundType,
    onSelect: (PlaybackBackgroundType) -> Unit
) {
    val demos = listOf(
        PlaybackBackgroundType.DEMO_LAVENDER to "Lavanda",
        PlaybackBackgroundType.DEMO_SUNSET to "Atardecer",
        PlaybackBackgroundType.DEMO_MIDNIGHT to "Noche",
        PlaybackBackgroundType.BLACK to "Negro"
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "Vista previa",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(demos) { (type, label) ->
                val isSelected = selected == type
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MemoriaPurple else MemoriaLine,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .safeClickable { onSelect(type) }
                    ) {
                        PlaybackLetterboxBackground(
                            type = type,
                            customImageUri = "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            item {
                val isSelected = selected == PlaybackBackgroundType.CUSTOM_IMAGE
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MemoriaPurple else MemoriaLine,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .safeClickable { onSelect(PlaybackBackgroundType.CUSTOM_IMAGE) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Mía", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
