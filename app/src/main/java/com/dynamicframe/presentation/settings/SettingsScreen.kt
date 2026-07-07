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
import androidx.compose.ui.draw.alpha
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
import com.dynamicframe.ui.theme.ShuffleIcon

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
        // Estado de expansión por sección (todas abiertas por defecto).
        val sectionExpandedState = remember { mutableStateMapOf<String, Boolean>() }
        fun sectionExpanded(key: String): Boolean = sectionExpandedState[key] ?: true
        fun toggleSection(key: String) {
            sectionExpandedState[key] = !(sectionExpandedState[key] ?: true)
        }
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
                        message = "Activa el acceso a fotos, vídeos o música en los ajustes del sistema para elegir carpetas.",
                        onGrantAccess = requestMediaAccess?.let { req -> { req {} } }
                    )
                }
            }

            // ── FOTOS ─────────────────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Fotos", icon = Icons.Default.Photo, accent = SectionColorPhotos,
                    expanded = sectionExpanded("Fotos"), onToggle = { toggleSection("Fotos") },
                    intro = "Elige las carpetas con tus fotos. Deja vacío para usar la galería del dispositivo."
                ) {
                    NostalgiaActionButton(
                        text = if (useInAppBrowser) "Añadir carpeta de fotos" else "Elegir carpeta de fotos",
                        icon = Icons.Default.Photo,
                        onClick = { openFolderPicker(FolderTarget.PHOTO, requestMediaAccess) }
                    )
                    config.photoFolderUris.forEach { uri ->
                        FolderChip(
                            label = viewModel.folderLabel(uri), uri = uri,
                            enabled = uri !in config.disabledPhotoFolderUris,
                            onToggleEnabled = { viewModel.togglePhotoFolderEnabled(uri); onMediaChanged() },
                            onRemove = { viewModel.removePhotoFolder(uri); onMediaChanged() }
                        )
                    }
                    // Atenuado: opción de respaldo cuando no hay carpetas propias
                    Box(modifier = Modifier.alpha(0.55f)) {
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
                        Spacer(Modifier.height(4.dp))
                        Text("Álbumes seleccionados", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp))
                        Text("Vacío = todos los medios", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    SettingsSwitchItem(
                        title = "Fotos aleatorias",
                        icon = ShuffleIcon,
                        checked = config.photoShuffle,
                        note = "Reproduce las fotos en orden aleatorio.",
                        onCheckedChange = viewModel::updatePhotoShuffle
                    )
                }
            }

            // ── VIDEOS ────────────────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Videos", icon = Icons.Default.Videocam, accent = SectionColorVideos,
                    expanded = sectionExpanded("Videos"), onToggle = { toggleSection("Videos") },
                    intro = "Elige las carpetas con tus videos y configura su reproducción."
                ) {
                    NostalgiaActionButton(
                        text = if (useInAppBrowser) "Añadir carpeta de videos" else "Elegir carpeta de videos",
                        icon = Icons.Default.Movie,
                        onClick = { openFolderPicker(FolderTarget.VIDEO, requestMediaAccess) }
                    )
                    config.videoFolderUris.forEach { uri ->
                        FolderChip(
                            label = viewModel.folderLabel(uri), uri = uri,
                            enabled = uri !in config.disabledVideoFolderUris,
                            onToggleEnabled = { viewModel.toggleVideoFolderEnabled(uri); onMediaChanged() },
                            onRemove = { viewModel.removeVideoFolder(uri); onMediaChanged() }
                        )
                    }
                    SettingsSwitchItem(
                        title = "Videos aleatorios",
                        icon = ShuffleIcon,
                        checked = config.videoShuffle,
                        note = "Reproduce los videos en orden aleatorio.",
                        onCheckedChange = viewModel::updateVideoShuffle
                    )
                    SettingsSwitchItem(
                        title = "Silenciar audio de videos",
                        icon = Icons.Default.VolumeOff,
                        checked = config.muteVideoAudio,
                        note = "Silencia el audio original de los videos del slideshow.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(muteVideoAudio = it)) }
                    )
                    if (!config.muteVideoAudio) {
                        SettingsSliderItem(
                            title = "Volumen de video",
                            value = config.mediaVolume,
                            valueRange = 0f..1f,
                            valueLabel = "${(config.mediaVolume * 100).toInt()}%",
                            note = "Volumen del audio de los videos. Se sincroniza con el control de la reproducción.",
                            onValueChange = { viewModel.updateMediaVolume(it) }
                        )
                    }
                    SettingsSwitchItem(
                        title = "Reproducir video completo",
                        icon = Icons.Default.Fullscreen,
                        checked = config.videoPlayFull,
                        note = "Espera a que el video termine antes de pasar al siguiente medio.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(videoPlayFull = it)) }
                    )
                }
            }

            // ── MÚSICA DE FONDO ───────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Música de fondo", icon = Icons.Default.MusicNote, accent = SectionColorMusic,
                    expanded = sectionExpanded("Música"), onToggle = { toggleSection("Música") }
                ) {
                    Text("Fuentes activas", fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp))
                    Text("Puedes combinar varias fuentes a la vez.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp))
                    // Toggle por tipo de fuente (las implementadas)
                    listOf(MusicSourceType.DEVICE_LIBRARY, MusicSourceType.LOCAL_FOLDER).forEach { type ->
                        SettingsSwitchItem(
                            title = type.displayName(),
                            icon = Icons.Default.LibraryMusic,
                            checked = type in config.musicSourceTypes,
                            onCheckedChange = { viewModel.toggleMusicSource(type) }
                        )
                    }
                    // Placeholders futuras fuentes (deshabilitados/atenuados)
                    listOf(MusicSourceType.THEME, MusicSourceType.SPOTIFY, MusicSourceType.YOUTUBE).forEach { type ->
                        SettingsSwitchItem(
                            title = "${type.displayName()} (próximamente)",
                            icon = Icons.Default.LibraryMusic,
                            checked = false,
                            note = "Disponible en una versión futura.",
                            enabled = false,
                            onCheckedChange = { }
                        )
                    }
                    SettingsSliderItem(
                        title = "Volumen de música",
                        value = config.musicVolume,
                        valueRange = 0f..1f,
                        valueLabel = "${(config.musicVolume * 100).toInt()}%",
                        note = "Volumen general de la música de fondo.",
                        onValueChange = { viewModel.updateMusicVolume(it) }
                    )
                    SettingsSwitchItem(
                        title = "Música aleatoria",
                        icon = ShuffleIcon,
                        checked = config.musicShuffle,
                        note = "Reproduce las canciones en orden aleatorio.",
                        onCheckedChange = viewModel::updateMusicShuffle
                    )
                    if (MusicSourceType.LOCAL_FOLDER in config.musicSourceTypes) {
                        NostalgiaActionButton(
                            text = if (useInAppBrowser) "Añadir carpeta de música" else "Elegir carpeta de música",
                            icon = Icons.Default.FolderOpen,
                            onClick = { openFolderPicker(FolderTarget.MUSIC, requestMusicAccess) }
                        )
                        config.musicFolderUris.forEach { uri ->
                            FolderChip(
                                label = viewModel.folderLabel(uri), uri = uri,
                                enabled = uri !in config.disabledMusicFolderUris,
                                onToggleEnabled = { viewModel.toggleMusicFolderEnabled(uri) },
                                onRemove = { viewModel.removeMusicFolder(uri) }
                            )
                        }
                    }
                    if (MusicSourceType.THEME in config.musicSourceTypes) {
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
                    if (MusicSourceType.SPOTIFY in config.musicSourceTypes) {
                        SettingsTextFieldItem(
                            title = "URL playlist Spotify",
                            value = config.spotifyPlaylistUrl,
                            placeholder = "https://open.spotify.com/playlist/...",
                            onValueChange = { viewModel.updateConfig(config.copy(spotifyPlaylistUrl = it)) }
                        )
                        Text("Reproducción Spotify: próximamente (requiere Premium).", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    if (MusicSourceType.YOUTUBE in config.musicSourceTypes) {
                        SettingsTextFieldItem(
                            title = "URL lista YouTube",
                            value = config.youtubePlaylistUrl,
                            placeholder = "https://youtube.com/playlist?list=...",
                            onValueChange = { viewModel.updateConfig(config.copy(youtubePlaylistUrl = it)) }
                        )
                        Text("Reproducción YouTube: próximamente.", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
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
                    if (config.videoMusicBehavior == VideoMusicBehavior.DUCK) {
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
            }

            // ── SLIDESHOW ─────────────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Slideshow", icon = Icons.Default.Slideshow, accent = SectionColorSlideshow,
                    expanded = sectionExpanded("Slideshow"), onToggle = { toggleSection("Slideshow") }
                ) {
                    SettingsSliderItem(
                        title = "Tiempo por foto",
                        value = config.intervalSeconds.toFloat(),
                        valueRange = 3f..60f, steps = 56,
                        valueLabel = "${config.intervalSeconds}s",
                        note = "Segundos que se muestra cada foto antes de pasar a la siguiente.",
                        onValueChange = { viewModel.updateInterval(it.toInt()) }
                    )
                    SettingsDropdownItem(
                        title = "Transición",
                        icon = Icons.Default.Animation,
                        currentValue = config.transition.displayName(),
                        options = TransitionType.entries.map { it.displayName() },
                        note = config.transition.effectDescription(),
                        onSelect = { idx -> viewModel.updateTransition(TransitionType.entries[idx]) }
                    )
                    SlideshowTransitionPreview(
                        transitionType = config.transition,
                        durationMs = config.transitionDurationMs,
                    )
                    SettingsSliderItem(
                        title = "Duración de transición",
                        value = config.transitionDurationMs.toFloat(),
                        valueRange = 800f..2800f, steps = 19,
                        valueLabel = "${config.transitionDurationMs} ms",
                        note = "Duración del efecto de transición en milisegundos.",
                        onValueChange = { viewModel.updateConfig(config.copy(transitionDurationMs = it.toInt())) }
                    )
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
                    SettingsSwitchItem(
                        title = "Reproducir en bucle",
                        icon = Icons.Default.Repeat,
                        checked = config.loop,
                        note = "Vuelve al inicio cuando se terminen todos los medios.",
                        onCheckedChange = viewModel::updateLoop
                    )
                }
            }

            // ── VISUAL EN REPRODUCCIÓN ────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Visual en reproducción", icon = Icons.Default.Fullscreen, accent = SectionColorVisual,
                    expanded = sectionExpanded("Visual"), onToggle = { toggleSection("Visual") },
                    intro = "Toca/OK en pantalla completa para pausar. Los controles se ocultan solos."
                ) {
                    SettingsDropdownItem(
                        title = "Tema de la aplicación",
                        icon = Icons.Default.Palette,
                        currentValue = config.appTheme.appThemeDisplayName(),
                        options = AppTheme.entries.map { it.appThemeDisplayName() },
                        note = "Paradise: visualizador con fondo blur, viñetas y capas inmersivas (en construcción). Memoria: apariencia clásica de la app.",
                        onSelect = { idx ->
                            val theme = AppTheme.entries[idx]
                            viewModel.updateConfig(
                                config.copy(
                                    appTheme = theme,
                                    playbackTheme = if (theme == AppTheme.PARADISE) {
                                        PlaybackTheme.PARADISE
                                    } else {
                                        config.playbackTheme
                                    }
                                )
                            )
                        }
                    )
                    SettingsDropdownItem(
                        title = "Tema de la interfaz",
                        icon = Icons.Default.Style,
                        currentValue = config.playbackTheme.playbackThemeDisplayName(),
                        options = PlaybackTheme.entries.map { it.playbackThemeDisplayName() },
                        note = "Paradise: fondo blur que rellena las barras negras + capas inmersivas. Aurora Glass, Ambiente o Galería: estilo de controles (con Paradise activo, el fondo blur sigue aplicándose).",
                        onSelect = { idx ->
                            val theme = PlaybackTheme.entries[idx]
                            viewModel.updateConfig(
                                config.copy(
                                    playbackTheme = theme,
                                    appTheme = when (theme) {
                                        PlaybackTheme.PARADISE -> AppTheme.PARADISE
                                        else -> if (config.appTheme == AppTheme.PARADISE) {
                                            AppTheme.DEFAULT
                                        } else {
                                            config.appTheme
                                        }
                                    }
                                )
                            )
                        }
                    )
                    SettingsSwitchItem(
                        title = "Marco dorado (estilo cuadro)",
                        icon = Icons.Default.FilterFrames,
                        checked = config.showPictureFrame,
                        note = "Solo en tema Aurora Glass. Marco dorado decorativo alrededor de cada foto.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(showPictureFrame = it)) }
                    )
                    SettingsSliderItem(
                        title = "Grosor del marco",
                        value = config.playbackPictureFrameScale,
                        valueRange = 0.5f..1.5f,
                        valueLabel = "${(config.playbackPictureFrameScale * 100).toInt()}%",
                        note = "Ajusta el grosor del borde decorativo (marco dorado o paspartú de Galería).",
                        onValueChange = { viewModel.updateConfig(config.copy(playbackPictureFrameScale = it)) },
                        steps = 9
                    )
                    SettingsSliderItem(
                        title = "Zoom en pantalla completa",
                        value = config.playbackContentZoom,
                        valueRange = 0.75f..1f,
                        valueLabel = "${(config.playbackContentZoom * 100).toInt()}%",
                        note = "Zoom aplicado a cada foto o video durante la reproducción.",
                        onValueChange = { viewModel.updateConfig(config.copy(playbackContentZoom = it)) },
                        steps = 4
                    )
                    SettingsDropdownItem(
                        title = "Fondo letterbox",
                        icon = Icons.Default.Wallpaper,
                        currentValue = config.playbackBackgroundType.playbackBackgroundDisplayName(),
                        options = PlaybackBackgroundType.entries.map { it.playbackBackgroundDisplayName() },
                        note = "Rellena el espacio alrededor de fotos y vídeos. «Dinámico» usa los colores predominantes y un blur de la propia imagen. Ignorado con el tema Paradise (capa blur propia).",
                        onSelect = { idx ->
                            viewModel.updateConfig(config.copy(playbackBackgroundType = PlaybackBackgroundType.entries[idx]))
                        }
                    )
                    PlaybackBackgroundPreviewRow(
                        selected = config.playbackBackgroundType,
                        onSelect = { viewModel.updateConfig(config.copy(playbackBackgroundType = it)) }
                    )
                    if (config.playbackBackgroundType == PlaybackBackgroundType.CUSTOM_IMAGE) {
                        NostalgiaActionButton(text = "Elegir mi imagen de fondo",
                            icon = Icons.Default.Image,
                            onClick = { backgroundImageLauncher.launch(arrayOf("image/*")) })
                        if (config.playbackBackgroundImageUri.isNotBlank()) {
                            Text("Imagen seleccionada", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                    }
                    SettingsSwitchItem(
                        title = "Borde zona segura",
                        icon = Icons.Default.CropFree,
                        checked = config.playbackShowSafeBorder,
                        note = "Muestra un borde visual para comprobar que la imagen no queda recortada.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowSafeBorder = it)) }
                    )
                    SettingsSwitchItem(
                        title = "Solo imagen (inmersivo)",
                        icon = Icons.Default.Fullscreen,
                        checked = config.playbackImmersiveMode,
                        note = "Oculta todos los controles para máxima inmersión.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(playbackImmersiveMode = it)) }
                    )
                    SettingsSwitchItem(
                        title = "Mostrar reloj al reproducir",
                        icon = Icons.Default.Schedule,
                        checked = config.playbackShowClock,
                        note = "Superpone el reloj encima de las fotos durante el slideshow.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowClock = it)) }
                    )
                    SettingsSwitchItem(
                        title = "Mostrar controles y música",
                        icon = Icons.Default.Tune,
                        checked = config.playbackShowOverlay,
                        note = "Muestra el título de la canción y los controles de reproducción.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(playbackShowOverlay = it)) }
                    )
                }
            }

            // ── RELOJ Y FECHA ─────────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Reloj y fecha", icon = Icons.Default.AccessTime, accent = SectionColorClock,
                    expanded = sectionExpanded("Reloj"), onToggle = { toggleSection("Reloj") }
                ) {
                    SettingsSwitchItem(
                        title = "Mostrar reloj",
                        icon = Icons.Default.Schedule,
                        checked = config.showClock,
                        note = "Muestra el reloj en el panel principal de la app.",
                        onCheckedChange = { viewModel.toggleClock(it) }
                    )
                    if (config.showClock) {
                        SettingsSwitchItem(
                            title = "Mostrar fecha",
                            icon = Icons.Default.CalendarToday,
                            checked = config.showDate,
                            note = "Añade la fecha junto al reloj.",
                            onCheckedChange = { viewModel.updateConfig(config.copy(showDate = it)) }
                        )
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
            }

            // ── PANTALLA TV ───────────────────────────────────────────────────
            if (device.isTv) {
                item {
                    SettingsSectionCard(
                        title = "Pantalla TV", icon = Icons.Default.Tv, accent = SectionColorTv,
                        expanded = sectionExpanded("TV"), onToggle = { toggleSection("TV") },
                        intro = "El borde de pantalla muestra los límites reales de tu TV para ajustar el recorte."
                    ) {
                        SettingsSwitchItem(
                            title = "Mostrar borde de pantalla",
                            icon = Icons.Default.CropFree,
                            checked = config.showScreenBorder,
                            note = "Activa para ver el límite exacto de la imagen en tu TV.",
                            onCheckedChange = { viewModel.updateConfig(config.copy(showScreenBorder = it)) }
                        )
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
            }

            // ── SISTEMA ───────────────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    title = "Sistema", icon = Icons.Default.Settings, accent = SectionColorSystem,
                    expanded = sectionExpanded("Sistema"), onToggle = { toggleSection("Sistema") }
                ) {
                    SettingsSwitchItem(
                        title = "Modo depuración",
                        icon = Icons.Default.BugReport,
                        checked = debugModeEnabled,
                        note = if (debugModeEnabled) "Consola flotante DBG activa. Toca el botón morado para ver el log."
                        else "Registra acciones, errores y navegación (útil al reportar fallos).",
                        onCheckedChange = { viewModel.updateDebugMode(it) }
                    )
                    SettingsSwitchItem(
                        title = "Iniciar automáticamente al arrancar",
                        icon = Icons.Default.PowerSettingsNew,
                        checked = config.autoStartOnBoot,
                        note = "Arranca el slideshow automáticamente al encender el dispositivo.",
                        onCheckedChange = { viewModel.updateConfig(config.copy(autoStartOnBoot = it)) }
                    )
                }
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

// Colores de acento por sección — ayudan a saber visualmente en qué sección estás.
val SectionColorPhotos = Color(0xFF2E7BE6)
val SectionColorVideos = Color(0xFFE8612C)
val SectionColorMusic = Color(0xFF2BA84A)
val SectionColorSlideshow = Color(0xFF7C4DFF)
val SectionColorVisual = Color(0xFF0E9AA7)
val SectionColorClock = Color(0xFFE0A106)
val SectionColorTv = Color(0xFF5C6BC0)
val SectionColorSystem = Color(0xFF6B7280)

/**
 * Tarjeta de sección de Ajustes: cabecera con acento + contenido envuelto en un
 * contenedor con fondo del color de la sección. La cabecera es pulsable para
 * colapsar/expandir todas las opciones de esa configuración.
 */
@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    intro: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.07f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.14f))
                .safeClickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Contraer" else "Expandir",
                tint = accent
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (intro != null) {
                    Text(
                        intro,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Icon(icon, contentDescription = null, tint = accent)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
    HorizontalDivider(
        color = accent.copy(alpha = 0.35f),
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    note: String = "",
    enabled: Boolean = true
) {
    val device = LocalDeviceProfile.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val contentAlpha = if (enabled) 1f else 0.4f
    Column(modifier = Modifier.fillMaxWidth().alpha(contentAlpha)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) Modifier.safeClickable(interactionSource = source) { onCheckedChange(!checked) }
                    else Modifier
                )
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
                Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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
            // focusGroup permite que el D-pad entre en este Row y navegue entre los dos botones
            Row(
                modifier = Modifier.focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

fun PlaybackTheme.playbackThemeDisplayName() = when (this) {
    PlaybackTheme.AURORA_GLASS -> "Aurora Glass (recomendado)"
    PlaybackTheme.AMBIENT -> "Ambiente (minimalista)"
    PlaybackTheme.GALLERY -> "Galería (museo)"
    PlaybackTheme.PARADISE -> "Paradise (fondo blur)"
}

fun AppTheme.appThemeDisplayName() = when (this) {
    AppTheme.DEFAULT -> "Memoria (clásico)"
    AppTheme.PARADISE -> "Paradise (inmersivo)"
}

/**
 * Chip de carpeta con toggle activar/desactivar y botón papelera navegable por separado.
 * En TV ambos controles son accesibles con D-pad (focusGroup exterior).
 */
@Composable
fun FolderChip(
    label: String,
    uri: String = "",
    enabled: Boolean = true,
    onToggleEnabled: (() -> Unit)? = null,
    onRemove: () -> Unit
) {
    val device = LocalDeviceProfile.current
    val displayPath = remember(uri) { uriToDisplayPath(uri) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (device.isTv) Modifier.focusGroup() else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Información de la carpeta ──────────────────────────────────────
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .alpha(if (enabled) 1f else 0.45f)
        ) {
            Text(text = label, fontSize = if (device.isTv) 15.sp else 14.sp, maxLines = 1)
            if (displayPath.isNotBlank() && displayPath != label) {
                Text(
                    text = displayPath,
                    fontSize = if (device.isTv) 11.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // ── Toggle activar/desactivar ───────────────────────────────────────
        if (onToggleEnabled != null) {
            Box(
                modifier = Modifier
                    .size(if (device.isTv) 44.dp else 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .safeClickable(onClick = onToggleEnabled),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (enabled) "Desactivar" else "Activar",
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(if (device.isTv) 22.dp else 18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        // ── Botón eliminar (suelto, independiente) ─────────────────────────
        Box(
            modifier = Modifier
                .size(if (device.isTv) 44.dp else 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                .safeClickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Quitar carpeta",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(if (device.isTv) 22.dp else 18.dp)
            )
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
        PlaybackBackgroundType.DYNAMIC to "Dinámico",
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
