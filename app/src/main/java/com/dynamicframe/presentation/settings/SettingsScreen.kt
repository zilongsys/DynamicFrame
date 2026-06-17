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

            if (showPermissionDenied) {
                item {
                    MediaPermissionDeniedBanner(
                        message = "Activa el acceso a fotos, vídeos o música en los ajustes del sistema para elegir carpetas."
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
                    text = "Pantalla completa: toca/OK para pausar. Las ayudas se ocultan a los 5 s; el texto de acciones, a 1 s.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Marco dorado (estilo cuadro)",
                    icon = Icons.Default.FilterFrames,
                    checked = config.showPictureFrame,
                    onCheckedChange = { viewModel.updateConfig(config.copy(showPictureFrame = it)) }
                )
            }

            item {
                SettingsSliderItem(
                    title = "Grosor del marco dorado",
                    value = config.playbackPictureFrameScale,
                    valueRange = 0.5f..1.5f,
                    valueLabel = "${(config.playbackPictureFrameScale * 100).toInt()}%",
                    onValueChange = {
                        viewModel.updateConfig(config.copy(playbackPictureFrameScale = it))
                    },
                    steps = 9
                )
            }

            item {
                SettingsSliderItem(
                    title = "Zoom en pantalla completa",
                    value = config.playbackContentZoom,
                    valueRange = 0.75f..1f,
                    valueLabel = "${(config.playbackContentZoom * 100).toInt()}%",
                    onValueChange = {
                        viewModel.updateConfig(config.copy(playbackContentZoom = it))
                    },
                    steps = 4
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("Fondo letterbox", Icons.Default.Wallpaper)
                Text(
                    text = "Rellena el espacio alrededor de fotos que no ocupan todo el marco.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingsDropdownItem(
                    title = "Tipo de fondo",
                    icon = Icons.Default.Palette,
                    currentValue = config.playbackBackgroundType.playbackBackgroundDisplayName(),
                    options = PlaybackBackgroundType.entries.map { it.playbackBackgroundDisplayName() },
                    onSelect = { idx ->
                        viewModel.updateConfig(
                            config.copy(playbackBackgroundType = PlaybackBackgroundType.entries[idx])
                        )
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
                    NostalgiaActionButton(
                        text = "Elegir mi imagen",
                        icon = Icons.Default.Image,
                        onClick = { backgroundImageLauncher.launch(arrayOf("image/*")) }
                    )
                }
                if (config.playbackBackgroundImageUri.isNotBlank()) {
                    item {
                        Text(
                            text = "Imagen seleccionada",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            item {
                SettingsSwitchItem(
                    title = "Borde rosa (zona segura)",
                    icon = Icons.Default.CropFree,
                    checked = config.playbackShowSafeBorder,
                    onCheckedChange = {
                        viewModel.updateConfig(config.copy(playbackShowSafeBorder = it))
                    }
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
                    title = "Fotos aleatorias",
                    icon = Icons.Default.Photo,
                    checked = config.photoShuffle,
                    onCheckedChange = viewModel::updatePhotoShuffle
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Videos aleatorios",
                    icon = Icons.Default.Videocam,
                    checked = config.videoShuffle,
                    onCheckedChange = viewModel::updateVideoShuffle
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Reproducir en bucle",
                    icon = Icons.Default.Repeat,
                    checked = config.loop,
                    onCheckedChange = viewModel::updateLoop
                )
            }

            // ── CARPETAS DE FOTOS Y VIDEOS ───────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Carpetas de medios", Icons.Default.Folder)
                Text(
                    text = if (useInAppBrowser) {
                        "Puedes añadir varias carpetas para fotos y videos por separado. Sin carpeta = galería."
                    } else {
                        "Añade tantas carpetas como necesites. Sin carpeta = galería del dispositivo."
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Text(
                    "Fotos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                NostalgiaActionButton(
                    text = if (useInAppBrowser) "Añadir carpeta de fotos"
                    else "Elegir carpeta de fotos",
                    icon = Icons.Default.Photo,
                    onClick = { openFolderPicker(FolderTarget.PHOTO, requestMediaAccess) }
                )
            }

            items(config.photoFolderUris, key = { it }) { uri ->
                FolderChip(
                    label = viewModel.folderLabel(uri),
                    onRemove = {
                        viewModel.removePhotoFolder(uri)
                        onMediaChanged()
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Videos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                NostalgiaActionButton(
                    text = if (useInAppBrowser) "Añadir carpeta de videos"
                    else "Elegir carpeta de videos",
                    icon = Icons.Default.Movie,
                    onClick = { openFolderPicker(FolderTarget.VIDEO, requestMediaAccess) }
                )
            }

            items(config.videoFolderUris, key = { it }) { uri ->
                FolderChip(
                    label = viewModel.folderLabel(uri),
                    onRemove = {
                        viewModel.removeVideoFolder(uri)
                        onMediaChanged()
                    }
                )
            }

            if (!useInAppBrowser && systemPickerAvailable) {
                item {
                    NostalgiaActionButton(
                        text = "Selector del sistema (fotos)",
                        icon = Icons.Default.FolderOpen,
                        onClick = { openFolderPicker(FolderTarget.PHOTO, requestMediaAccess) }
                    )
                }
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
                    NostalgiaActionButton(
                        text = if (useInAppBrowser) "Añadir carpeta de música"
                        else "Elegir carpeta de música",
                        icon = Icons.Default.FolderOpen,
                        onClick = { openFolderPicker(FolderTarget.MUSIC, requestMusicAccess) }
                    )
                }
                items(config.musicFolderUris, key = { it }) { uri ->
                    FolderChip(
                        label = viewModel.folderLabel(uri),
                        onRemove = { viewModel.removeMusicFolder(uri) }
                    )
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
                    onCheckedChange = viewModel::updateMusicShuffle
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
            if (albums.isNotEmpty() && !config.hasCustomMediaFolders()) {
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
                    if (device.isTv) {
                        albums.forEach { album ->
                            val selected = config.selectedAlbumIds.contains(album.id)
                            SettingsAlbumSelectRow(
                                label = "${album.name} (${album.itemCount})",
                                selected = selected,
                                onClick = {
                                    val newIds = if (selected)
                                        config.selectedAlbumIds - album.id
                                    else
                                        config.selectedAlbumIds + album.id
                                    viewModel.updateSelectedAlbums(newIds)
                                }
                            )
                        }
                    } else {
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
            }

            // ── SISTEMA ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader("Sistema", Icons.Default.Settings)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (device.isTv) 26.dp else 22.dp)
                        )
                        Text(
                            text = "Versión de la app",
                            fontSize = if (device.isTv) 16.sp else 14.sp
                        )
                    }
                    AppVersionLabel(
                        showBuildCode = true,
                        fontSize = if (device.isTv) 15.sp else 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
        if (device.isTv) {
            val step = if (steps > 0) {
                (valueRange.endInclusive - valueRange.start) / (steps + 1)
            } else {
                (valueRange.endInclusive - valueRange.start) / 10f
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvStepperChip(
                    icon = Icons.Default.Remove,
                    desc = "Reducir",
                    onClick = { onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive)) }
                )
                Text(
                    valueLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 56.dp)
                )
                TvStepperChip(
                    icon = Icons.Default.Add,
                    desc = "Aumentar",
                    onClick = { onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive)) }
                )
            }
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(text = title, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentValue,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
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
fun FolderChip(label: String, onRemove: () -> Unit) {
    val device = LocalDeviceProfile.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Row(
            modifier = Modifier
                .safeClickable(onClick = onRemove)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Quitar carpeta",
                tint = MaterialTheme.colorScheme.error
            )
            if (device.isTv) {
                Text("Quitar", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            }
        }
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
