package com.dynamicframe.presentation.slideshow

import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.domain.model.DeleteMediaResult
import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.PlaybackBackgroundType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.WeatherInfo
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.domain.model.isParadiseActive
import com.dynamicframe.domain.model.photoFolderPillId
import com.dynamicframe.domain.model.videoFolderPillId
import com.dynamicframe.domain.playback.SlideshowMusicCoordinator
import com.dynamicframe.domain.slideshow.DynamicBackdropPrefetcher
import com.dynamicframe.domain.slideshow.SlideshowEngine
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import com.dynamicframe.domain.usecase.DeleteMediaItemUseCase
import com.dynamicframe.domain.usecase.EvictMediaCacheUseCase
import com.dynamicframe.domain.usecase.TakeDeleteConsentUseCase
import com.dynamicframe.domain.usecase.GetFolderDisplayNameUseCase
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.GetVideoBlurThumbnailUseCase
import com.dynamicframe.domain.usecase.ObserveMusicPlaybackUseCase
import com.dynamicframe.domain.usecase.PreloadSlideshowImagesUseCase
import com.dynamicframe.domain.usecase.UpdateSelectedAlbumsUseCase
import com.dynamicframe.domain.usecase.UpdateSlideshowConfigUseCase
import com.dynamicframe.domain.usecase.WeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumPillOption(
    val id: String?,
    val label: String,
    /** URI de la 1.ª foto del álbum/carpeta (miniatura en HUD Aurora). */
    val thumbnailUri: String? = null,
    val itemCount: Int = 0
)

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val slideshowEngine: SlideshowEngine,
    private val musicCoordinator: SlideshowMusicCoordinator,
    observeMusicPlayback: ObserveMusicPlaybackUseCase,
    private val getLocalAlbums: GetLocalAlbumsUseCase,
    private val deleteMediaItem: DeleteMediaItemUseCase,
    private val takeDeleteConsent: TakeDeleteConsentUseCase,
    private val evictMediaCache: EvictMediaCacheUseCase,
    private val preloadSlideshowImages: PreloadSlideshowImagesUseCase,
    private val updateSelectedAlbums: UpdateSelectedAlbumsUseCase,
    private val getFolderDisplayName: GetFolderDisplayNameUseCase,
    val slideshowVideoPlayer: SlideshowVideoPlayerRepository,
    val videoBackdropPlayer: VideoBackdropPlayerRepository,
    private val getVideoBlurThumbnail: GetVideoBlurThumbnailUseCase,
    private val backdropPrefetcher: DynamicBackdropPrefetcher,
    private val updateSlideshowConfig: UpdateSlideshowConfigUseCase,
    private val weatherUseCase: WeatherUseCase,
    private val debug: AppDebugLogger
) : ViewModel() {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _deleteConsentIntentSender = MutableStateFlow<IntentSender?>(null)
    val deleteConsentIntentSender: StateFlow<IntentSender?> = _deleteConsentIntentSender.asStateFlow()

    private var deleteContext: DeleteContext? = null

    private data class DeleteContext(
        val item: MediaItem,
        val resumeAfter: Boolean,
    )

    val slideshowState = slideshowEngine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), slideshowEngine.state.value)

    val slideshowConfig = slideshowEngine.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowConfig())

    val musicState = observeMusicPlayback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), musicCoordinator.state.value)

    private val _storeAlbums = MutableStateFlow<List<MediaAlbum>>(emptyList())

    val albumPills: StateFlow<List<AlbumPillOption>> = combine(
        slideshowConfig,
        _storeAlbums,
        slideshowState
    ) { config, albums, state ->
        buildAlbumPills(config, albums, state.playlistItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(AlbumPillOption(null, "Todos")))

    val selectedAlbumId: StateFlow<String?> = slideshowConfig
        .map { it.selectedAlbumIds.singleOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _videoBlurThumbnailUri = MutableStateFlow<String?>(null)
    val videoBlurThumbnailUri: StateFlow<String?> = _videoBlurThumbnailUri.asStateFlow()

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo.asStateFlow()

    private val _presentationPhase = MutableStateFlow(SlideshowPresentationPhase.Idle)
    val presentationPhase: StateFlow<SlideshowPresentationPhase> = _presentationPhase.asStateFlow()
    private var presentationJob: Job? = null
    private var fullscreenSessionActive = false

    val dynamicPalettes: StateFlow<Map<String, MediaDynamicPalette>> = backdropPrefetcher.palettes

    val dynamicLetterboxPalette: StateFlow<MediaDynamicPalette?> = combine(
        slideshowState.map { it.currentItem?.id }.distinctUntilChanged(),
        backdropPrefetcher.palettes,
        slideshowConfig.map { it.playbackBackgroundType }.distinctUntilChanged(),
        _presentationPhase,
    ) { itemId, palettes, backgroundType, phase ->
        if (phase != SlideshowPresentationPhase.Presenting) null
        else if (backgroundType != PlaybackBackgroundType.DYNAMIC || itemId == null) null
        else palettes[itemId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _controlsVisible = MutableStateFlow(false)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()
    private var hideControlsJob: Job? = null

    init {
        slideshowEngine.setBeforeNavigate { item ->
            awaitDynamicBackdropForNavigation(item)
        }

        viewModelScope.launch {
            _storeAlbums.value = runCatching {
                getLocalAlbums().getOrDefault(emptyList())
            }.getOrDefault(emptyList())
            runCatching { slideshowEngine.initialize() }
        }

        viewModelScope.launch {
            slideshowConfig
                .map {
                    musicCoordinator.musicConfigKey(
                        it.musicSourceTypes,
                        it.musicFolderUris,
                        it.disabledMusicFolderUris,
                        it.musicShuffle
                    )
                }
                .distinctUntilChanged()
                .collect {
                    if (_presentationPhase.value != SlideshowPresentationPhase.Presenting) return@collect
                    musicCoordinator.refreshPlaylist(force = false, playAfter = musicCoordinator.state.value.isPlaying)
                }
        }

        viewModelScope.launch {
            slideshowState
                .map { it.currentItem }
                .distinctUntilChanged()
                .collect { item ->
                    if (_presentationPhase.value != SlideshowPresentationPhase.Presenting) return@collect
                    viewModelScope.launch { updateVideoBlurThumbnail(item) }
                    applyMusicDuckForCurrentSlide()
                }
        }

        viewModelScope.launch {
            slideshowState
                .map { it.currentIndex }
                .distinctUntilChanged()
                .collect { index ->
                    if (!fullscreenSessionActive || !needsDynamicBackdrop()) return@collect
                    val playlist = slideshowState.value.playlistItems
                    if (playlist.isNotEmpty()) {
                        backdropPrefetcher.scheduleWindow(playlist, index)
                    }
                }
        }

        viewModelScope.launch {
            slideshowConfig
                .map { it.isParadiseActive() }
                .distinctUntilChanged()
                .collect { paradiseActive ->
                    if (paradiseActive) refreshWeather() else {
                        _weatherInfo.value = null
                        hideControls()
                    }
                }
        }

        viewModelScope.launch {
            combine(
                slideshowState.map { it.playlistItems to it.currentIndex }.distinctUntilChanged(),
                slideshowConfig.map { it.playbackBackgroundType }.distinctUntilChanged(),
            ) { (playlist, index), backgroundType ->
                if (backgroundType == PlaybackBackgroundType.DYNAMIC && playlist.isNotEmpty()) {
                    backdropPrefetcher.scheduleWindow(playlist, index)
                }
            }.collect { }
        }
    }

    private suspend fun awaitDynamicBackdropForNavigation(item: MediaItem) {
        if (!needsDynamicBackdrop()) return
        val playlist = slideshowState.value.playlistItems
        if (playlist.isEmpty()) return
        val index = playlist.indexOfFirst { it.id == item.id }
        if (index < 0) return
        backdropPrefetcher.scheduleWindow(playlist, index)
        backdropPrefetcher.awaitReady(item)
    }

    private fun refreshWeather() {
        viewModelScope.launch {
            if (!slideshowConfig.value.isParadiseActive()) {
                _weatherInfo.value = null
                return@launch
            }
            _weatherInfo.value = weatherUseCase()
        }
    }

    fun selectAlbum(albumId: String?) {
        viewModelScope.launch {
            updateSelectedAlbums(if (albumId == null) emptyList() else listOf(albumId))
        }
    }

    /** OK/Center en TV Paradise: muestra controles y auto-oculta tras 4 s. */
    fun onRemoteOkPressed() {
        _controlsVisible.value = true
        scheduleControlsAutoHide()
    }

    /** Reinicia el temporizador de auto-ocultado mientras el usuario usa los controles. */
    fun onControlsInteraction() {
        if (!_controlsVisible.value) return
        scheduleControlsAutoHide()
    }

    fun hideControls() {
        hideControlsJob?.cancel()
        _controlsVisible.value = false
    }

    private fun scheduleControlsAutoHide() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(4_000L)
            _controlsVisible.value = false
        }
    }

    fun onSlideshowScreenVisible() {
        musicCoordinator.setPlaybackAllowed(false)
        musicCoordinator.pause()
        if (slideshowConfig.value.playbackBackgroundType == PlaybackBackgroundType.DYNAMIC) {
            _presentationPhase.value = SlideshowPresentationPhase.Preparing
        }
        presentationJob?.cancel()
        presentationJob = viewModelScope.launch { presentFullscreenSession() }
    }

    fun enterSlideshowFullscreen() = onSlideshowScreenVisible()

    private suspend fun presentFullscreenSession() {
        debug.i("UI", "presentFullscreenSession")
        val dynamic = needsDynamicBackdrop()
        if (dynamic) {
            _presentationPhase.value = SlideshowPresentationPhase.Preparing
        }
        musicCoordinator.setPlaybackAllowed(false)
        musicCoordinator.pause()
        slideshowEngine.pause()
        runCatching { slideshowVideoPlayer.stop() }
        runCatching { videoBackdropPlayer.stop() }
        fullscreenSessionActive = false

        slideshowEngine.beginSession(startPaused = true)
        val state = slideshowState.value
        val item = state.currentItem
        if (item == null) {
            _presentationPhase.value = SlideshowPresentationPhase.Idle
            return
        }

        if (dynamic) {
            backdropPrefetcher.scheduleWindow(state.playlistItems, state.currentIndex)
            backdropPrefetcher.awaitFirstSlideReady(item)
        }

        fullscreenSessionActive = true
        beginPresenting(freshSession = true)
    }

    private suspend fun updateVideoBlurThumbnail(item: MediaItem?) {
        _videoBlurThumbnailUri.value = null
        if (item?.type == MediaType.VIDEO && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            _videoBlurThumbnailUri.value = getVideoBlurThumbnail(item.uri).getOrNull()
        }
    }

    fun startSlideshow(freshSession: Boolean = false) {
        if (freshSession) {
            onSlideshowScreenVisible()
            return
        }
        if (_presentationPhase.value != SlideshowPresentationPhase.Presenting) return
        debug.i("UI", "startSlideshow resume")
        slideshowEngine.start()
        viewModelScope.launch {
            musicCoordinator.setPlaybackAllowed(true)
            musicCoordinator.resumePlayback()
            applyMusicDuckForCurrentSlide()
        }
    }

    private suspend fun beginPresenting(freshSession: Boolean) {
        _presentationPhase.value = SlideshowPresentationPhase.Presenting
        musicCoordinator.setPlaybackAllowed(true)
        slideshowEngine.start()
        musicCoordinator.startForSession(freshSession = freshSession)
        applyMusicDuckForCurrentSlide()
        refreshWeather()
    }

    private fun haltAllPlayback() {
        musicCoordinator.setPlaybackAllowed(false)
        musicCoordinator.pause()
        slideshowEngine.pause()
        runCatching { slideshowVideoPlayer.stop() }
        runCatching { videoBackdropPlayer.stop() }
    }

    private fun applyMusicDuckForCurrentSlide() {
        if (_presentationPhase.value != SlideshowPresentationPhase.Presenting) return
        val item = slideshowState.value.currentItem ?: return
        val config = slideshowConfig.value
        when (item.type) {
            MediaType.VIDEO ->
                if (config.muteVideoAudio) {
                    musicCoordinator.onPhotoShown(config.musicVolume)
                } else {
                    musicCoordinator.onVideoStarted(
                        config.videoMusicBehavior,
                        config.duckedMusicVolume,
                    )
                }
            MediaType.IMAGE -> musicCoordinator.onPhotoShown(config.musicVolume)
        }
    }

    private fun needsDynamicBackdrop(): Boolean =
        slideshowConfig.value.playbackBackgroundType == PlaybackBackgroundType.DYNAMIC

    fun restartSlideshow() = onSlideshowScreenVisible()

    fun pauseSlideshow() {
        slideshowEngine.pause()
        musicCoordinator.pause()
    }

    /**
     * Detiene TODO lo que se está reproduciendo: slideshow, vídeo en curso y música.
     * Se llama al salir del modo pantalla completa para que nada siga sonando/reproduciéndose.
     */
    fun stopSlideshow() {
        hideControls()
        presentationJob?.cancel()
        fullscreenSessionActive = false
        backdropPrefetcher.clear()
        _presentationPhase.value = SlideshowPresentationPhase.Idle
        haltAllPlayback()
    }

    fun nextSlide() = slideshowEngine.next()
    fun previousSlide() = slideshowEngine.previous()
    fun onVideoCompleted() = slideshowEngine.onVideoCompleted()
    fun onPlaybackError() = slideshowEngine.onPlaybackError()

    fun preloadImages(uris: List<String>, width: Int, height: Int) {
        if (uris.isEmpty()) return
        preloadSlideshowImages(uris, width, height)
        if (slideshowConfig.value.playbackBackgroundType != PlaybackBackgroundType.DYNAMIC) return
        viewModelScope.launch {
            val uriSet = uris.toSet()
            slideshowState.value.playlistItems
                .filter { it.type == MediaType.IMAGE && it.uri in uriSet }
                .forEach { backdropPrefetcher.scheduleItem(it) }
        }
    }

    fun jumpToSlide(index: Int) = slideshowEngine.jumpTo(index)

    fun reloadMedia() {
        viewModelScope.launch {
            backdropPrefetcher.clear()
            slideshowEngine.loadMedia()
        }
    }

    /**
     * Ajusta el volumen de la música. Se aplica en vivo Y se persiste en config
     * para que quede enlazado con el mismo control en Ajustes/Inicio/Música.
     */
    fun setMusicVolume(volume: Float) {
        musicCoordinator.setVolume(volume)
        viewModelScope.launch {
            updateSlideshowConfig { it.copy(musicVolume = volume) }
        }
    }

    /** Ajusta el volumen del audio del vídeo en reproducción (0.0–1.0). Persistido y enlazado. */
    fun setMediaVolume(volume: Float) {
        viewModelScope.launch {
            updateSlideshowConfig { it.copy(mediaVolume = volume) }
        }
    }

    /** Reproduce/pausa SOLO la música de fondo, sin afectar al slideshow de fotos/vídeos. */
    fun toggleMusicPlayback() {
        if (musicState.value.isPlaying) {
            musicCoordinator.pause()
        } else {
            viewModelScope.launch { musicCoordinator.resumePlayback() }
        }
    }

    fun skipNextTrack() = musicCoordinator.skipNext()

    fun clearToast() {
        _toastMessage.value = null
    }

    /** Pausa slideshow, vídeo y música al abrir el diálogo de borrado. */
    fun prepareDelete(item: MediaItem) {
        deleteContext = DeleteContext(item, slideshowState.value.isPlaying)
        pauseForDelete()
    }

    fun cancelDelete() {
        val resume = deleteContext?.resumeAfter == true
        deleteContext = null
        if (resume) resumeAfterDelete()
    }

    fun confirmDelete(item: MediaItem) {
        val ctx = deleteContext ?: DeleteContext(item, false)
        deleteContext = null
        performDelete(ctx.item, ctx.resumeAfter)
    }

    fun clearDeleteConsentIntent() {
        _deleteConsentIntentSender.value = null
    }

    fun onDeleteConsentResult(granted: Boolean) {
        clearDeleteConsentIntent()
        val ctx = deleteContext
        deleteContext = null
        if (!granted || ctx == null) {
            if (ctx?.resumeAfter == true) resumeAfterDelete()
            if (!granted && ctx != null) {
                _toastMessage.value = "Borrado cancelado"
            }
            return
        }
        viewModelScope.launch { onDeleteSucceeded(ctx.item, ctx.resumeAfter) }
    }

    private fun pauseForDelete() {
        haltAllPlayback()
        slideshowState.value.currentItem?.uri?.let { uri ->
            runCatching { slideshowVideoPlayer.stopIfCurrent(uri) }
            runCatching { videoBackdropPlayer.stopIfCurrent(uri) }
        }
    }

    private fun performDelete(item: MediaItem, resumeAfter: Boolean) {
        viewModelScope.launch {
            pauseForDelete()
            runCatching { slideshowVideoPlayer.stopIfCurrent(item.uri) }
            runCatching { videoBackdropPlayer.stopIfCurrent(item.uri) }
            evictMediaCache(item.uri)

            when (val result = deleteMediaItem(item)) {
                DeleteMediaResult.Deleted -> onDeleteSucceeded(item, resumeAfter)
                is DeleteMediaResult.NeedsUserConsent -> {
                    deleteContext = DeleteContext(item, resumeAfter)
                    val sender = takeDeleteConsent(result.consentHandle) as? IntentSender
                    if (sender != null) {
                        _deleteConsentIntentSender.value = sender
                    } else {
                        onDeleteFailed("No se pudo solicitar permiso para borrar", resumeAfter)
                    }
                }
                is DeleteMediaResult.Failed -> onDeleteFailed(result.message, resumeAfter)
            }
        }
    }

    private suspend fun onDeleteSucceeded(item: MediaItem, resumeAfter: Boolean) {
        slideshowEngine.removeItem(item.id)
        _toastMessage.value = if (item.type == MediaType.VIDEO) {
            "Vídeo eliminado"
        } else {
            "Foto eliminada"
        }
        if (resumeAfter && slideshowEngine.state.value.playlistItems.isNotEmpty()) {
            resumeAfterDelete()
        }
    }

    private fun onDeleteFailed(message: String, resumeAfter: Boolean) {
        _toastMessage.value = message
        if (resumeAfter) resumeAfterDelete()
    }

    private fun resumeAfterDelete() {
        if (_presentationPhase.value != SlideshowPresentationPhase.Presenting) return
        if (slideshowEngine.state.value.playlistItems.isEmpty()) return
        slideshowEngine.start()
        viewModelScope.launch {
            musicCoordinator.setPlaybackAllowed(true)
            musicCoordinator.resumePlayback()
            applyMusicDuckForCurrentSlide()
        }
    }

    fun deleteItem(item: MediaItem) {
        performDelete(item, slideshowState.value.isPlaying)
    }

    fun deleteCurrentSlide() {
        slideshowState.value.currentItem?.let { confirmDelete(it) }
    }

    override fun onCleared() {
        slideshowEngine.setBeforeNavigate(null)
        musicCoordinator.disconnect()
        slideshowVideoPlayer.release()
        videoBackdropPlayer.release()
        super.onCleared()
    }

    private fun buildAlbumPills(
        config: SlideshowConfig,
        albums: List<MediaAlbum>,
        playlistItems: List<MediaItem>
    ): List<AlbumPillOption> {
        fun thumbOfImage(items: List<MediaItem>): String? =
            items.firstOrNull { it.type == MediaType.IMAGE }?.let { it.thumbnailUri ?: it.uri }

        fun thumbOfVideo(items: List<MediaItem>): String? =
            items.firstOrNull { it.type == MediaType.VIDEO }?.let { it.thumbnailUri ?: it.uri }

        fun thumbOfMixed(items: List<MediaItem>): String? =
            thumbOfImage(items) ?: thumbOfVideo(items)

        fun pill(
            id: String?,
            label: String,
            items: List<MediaItem>,
            coverFallback: String? = null,
            countOverride: Int? = null,
            preferVideoThumb: Boolean = false
        ) = AlbumPillOption(
            id = id,
            label = label,
            thumbnailUri = when {
                preferVideoThumb -> thumbOfVideo(items) ?: coverFallback
                else -> thumbOfMixed(items) ?: coverFallback
            },
            itemCount = countOverride ?: items.size
        )

        val pills = mutableListOf(
            pill(null, "Todos", playlistItems)
        )
        if (config.hasCustomMediaFolders()) {
            config.activePhotoFolderUris().distinct().forEach { uri ->
                val folderName = getFolderDisplayName(uri)
                val items = playlistItems.filter {
                    it.type == MediaType.IMAGE && itemBelongsToFolder(it, uri, folderName)
                }
                pills.add(
                    pill(photoFolderPillId(uri), "Fotos: $folderName", items)
                )
            }
            config.activeVideoFolderUris().distinct().forEach { uri ->
                val folderName = getFolderDisplayName(uri)
                val items = playlistItems.filter {
                    it.type == MediaType.VIDEO && itemBelongsToFolder(it, uri, folderName)
                }
                pills.add(
                    pill(
                        id = videoFolderPillId(uri),
                        label = "Videos: $folderName",
                        items = items,
                        preferVideoThumb = true
                    )
                )
            }
        } else {
            albums.forEach { album ->
                val items = playlistItems.filter { it.albumId == album.id }
                pills.add(
                    pill(
                        id = album.id,
                        label = album.name,
                        items = items,
                        coverFallback = album.coverUri,
                        countOverride = items.size.takeIf { it > 0 } ?: album.itemCount
                    )
                )
            }
        }
        return pills
    }

    private fun itemBelongsToFolder(item: MediaItem, folderUri: String, folderName: String): Boolean {
        if (item.uri.startsWith(folderUri)) return true
        if (folderUri.startsWith("file:")) {
            val path = folderUri.removePrefix("file://")
            return item.uri.contains(path, ignoreCase = true)
        }
        return item.albumName.equals(folderName, ignoreCase = true) ||
            item.albumId.equals(folderName, ignoreCase = true)
    }
}
