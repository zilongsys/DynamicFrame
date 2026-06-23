package com.dynamicframe.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.domain.model.photoFolderPillId
import com.dynamicframe.domain.model.videoFolderPillId
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.playback.SlideshowMusicCoordinator
import com.dynamicframe.domain.slideshow.SlideshowEngine
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.usecase.DeleteMediaItemUseCase
import com.dynamicframe.domain.usecase.EvictMediaCacheUseCase
import com.dynamicframe.domain.usecase.GetFolderDisplayNameUseCase
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.ObserveMusicPlaybackUseCase
import com.dynamicframe.domain.usecase.PreloadSlideshowImagesUseCase
import com.dynamicframe.domain.usecase.UpdateSelectedAlbumsUseCase
import com.dynamicframe.domain.usecase.UpdateSlideshowConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumPillOption(val id: String?, val label: String)

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val slideshowEngine: SlideshowEngine,
    private val musicCoordinator: SlideshowMusicCoordinator,
    observeMusicPlayback: ObserveMusicPlaybackUseCase,
    private val getLocalAlbums: GetLocalAlbumsUseCase,
    private val deleteMediaItem: DeleteMediaItemUseCase,
    private val evictMediaCache: EvictMediaCacheUseCase,
    private val preloadSlideshowImages: PreloadSlideshowImagesUseCase,
    private val updateSelectedAlbums: UpdateSelectedAlbumsUseCase,
    private val getFolderDisplayName: GetFolderDisplayNameUseCase,
    val slideshowVideoPlayer: SlideshowVideoPlayerRepository,
    private val updateSlideshowConfig: UpdateSlideshowConfigUseCase,
    private val debug: AppDebugLogger
) : ViewModel() {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val slideshowState = slideshowEngine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), slideshowEngine.state.value)

    val slideshowConfig = slideshowEngine.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowConfig())

    val musicState = observeMusicPlayback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), musicCoordinator.state.value)

    private val _storeAlbums = MutableStateFlow<List<MediaAlbum>>(emptyList())

    val albumPills: StateFlow<List<AlbumPillOption>> = combine(
        slideshowConfig,
        _storeAlbums
    ) { config, albums ->
        buildAlbumPills(config, albums)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(AlbumPillOption(null, "Todos")))

    val selectedAlbumId: StateFlow<String?> = slideshowConfig
        .map { it.selectedAlbumIds.singleOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _storeAlbums.value = runCatching {
                getLocalAlbums().getOrDefault(emptyList())
            }.getOrDefault(emptyList())
            runCatching { slideshowEngine.initialize() }
        }

        viewModelScope.launch {
            combine(
                slideshowEngine.state.map { it.currentItem?.type }.distinctUntilChanged(),
                slideshowConfig
            ) { type, config -> type to config }
                .collect { (type, config) ->
                    when (type) {
                        MediaType.VIDEO ->
                            // Si el vídeo está silenciado, la música sigue sonando normal
                            // (no tiene sentido atenuarla o pausarla por un vídeo mudo).
                            if (config.muteVideoAudio) {
                                musicCoordinator.onPhotoShown(config.musicVolume)
                            } else {
                                musicCoordinator.onVideoStarted(
                                    config.videoMusicBehavior,
                                    config.duckedMusicVolume
                                )
                            }
                        MediaType.IMAGE -> musicCoordinator.onPhotoShown(config.musicVolume)
                        null -> Unit
                    }
                }
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
                .collect { musicCoordinator.refreshPlaylist(force = true, playAfter = false) }
        }
    }

    fun selectAlbum(albumId: String?) {
        viewModelScope.launch {
            updateSelectedAlbums(if (albumId == null) emptyList() else listOf(albumId))
        }
    }

    fun startSlideshow(freshSession: Boolean = false) {
        debug.i("UI", "startSlideshow freshSession=$freshSession")
        if (freshSession) {
            slideshowEngine.beginSession()
        } else {
            slideshowEngine.start()
        }
        viewModelScope.launch { musicCoordinator.startForSession(freshSession) }
    }

    fun restartSlideshow() = startSlideshow(freshSession = true)

    fun pauseSlideshow() {
        slideshowEngine.pause()
        musicCoordinator.pause()
    }

    /**
     * Detiene TODO lo que se está reproduciendo: slideshow, vídeo en curso y música.
     * Se llama al salir del modo pantalla completa para que nada siga sonando/reproduciéndose.
     */
    fun stopSlideshow() {
        slideshowEngine.pause()
        runCatching { slideshowVideoPlayer.stop() }
        musicCoordinator.pause()
    }

    fun nextSlide() = slideshowEngine.next()
    fun previousSlide() = slideshowEngine.previous()
    fun onVideoCompleted() = slideshowEngine.onVideoCompleted()
    fun onPlaybackError() = slideshowEngine.onPlaybackError()

    fun preloadImages(uris: List<String>, width: Int, height: Int) {
        if (uris.isEmpty()) return
        preloadSlideshowImages(uris, width, height)
    }

    fun jumpToSlide(index: Int) = slideshowEngine.jumpTo(index)

    fun reloadMedia() {
        viewModelScope.launch { slideshowEngine.loadMedia() }
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

    fun deleteItem(item: MediaItem) {
        debug.w("UI", "deleteItem ${item.type} ${item.name}", item.uri)
        viewModelScope.launch {
            val wasPlaying = slideshowState.value.isPlaying
            slideshowEngine.pause()
            slideshowEngine.releaseCurrentForDelete()
            evictMediaCache(item.uri)
            delay(300)

            deleteMediaItem(item)
                .onSuccess {
                    slideshowEngine.removeItem(item.id)
                    if (wasPlaying && slideshowEngine.state.value.playlistItems.isNotEmpty()) {
                        slideshowEngine.start()
                        musicCoordinator.resumePlayback()
                    }
                }
                .onFailure { e ->
                    debug.e("UI", "deleteItem falló", e.message)
                    _toastMessage.value = e.message ?: "No se pudo borrar el archivo"
                    slideshowEngine.loadMedia()
                    if (wasPlaying) {
                        slideshowEngine.start()
                        musicCoordinator.resumePlayback()
                    }
                }
        }
    }

    fun deleteCurrentSlide() {
        slideshowState.value.currentItem?.let { deleteItem(it) }
    }

    override fun onCleared() {
        musicCoordinator.disconnect()
        slideshowVideoPlayer.release()
        super.onCleared()
    }

    private fun buildAlbumPills(config: SlideshowConfig, albums: List<MediaAlbum>): List<AlbumPillOption> {
        val pills = mutableListOf(AlbumPillOption(null, "Todos"))
        if (config.hasCustomMediaFolders()) {
            config.activePhotoFolderUris().distinct().forEach { uri ->
                pills.add(AlbumPillOption(photoFolderPillId(uri), "Fotos: ${getFolderDisplayName(uri)}"))
            }
            config.activeVideoFolderUris().distinct().forEach { uri ->
                pills.add(AlbumPillOption(videoFolderPillId(uri), "Videos: ${getFolderDisplayName(uri)}"))
            }
        } else {
            albums.forEach { album ->
                pills.add(AlbumPillOption(album.id, album.name))
            }
        }
        return pills
    }
}
