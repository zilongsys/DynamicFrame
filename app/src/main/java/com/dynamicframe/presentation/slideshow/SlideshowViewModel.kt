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
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import com.dynamicframe.domain.usecase.DeleteMediaItemUseCase
import com.dynamicframe.domain.usecase.EvictMediaCacheUseCase
import com.dynamicframe.domain.usecase.GetFolderDisplayNameUseCase
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.ObserveMusicPlaybackUseCase
import com.dynamicframe.domain.usecase.PreloadSlideshowImagesUseCase
import com.dynamicframe.domain.usecase.UpdateSelectedAlbumsUseCase
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
    val slideshowVideoPlayer: SlideshowVideoPlayerRepository
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
                        MediaType.VIDEO -> musicCoordinator.onVideoStarted(
                            config.videoMusicBehavior,
                            config.duckedMusicVolume
                        )
                        MediaType.IMAGE -> musicCoordinator.onPhotoShown(config.musicVolume)
                        null -> Unit
                    }
                }
        }

        viewModelScope.launch {
            slideshowConfig
                .map { musicCoordinator.musicConfigKey(it.musicSourceType, it.musicFolderUris, it.musicShuffle) }
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

    fun setMusicVolume(volume: Float) = musicCoordinator.setVolume(volume)

    fun toggleMusicPlayback() {
        if (slideshowState.value.isPlaying) {
            pauseSlideshow()
        } else {
            startSlideshow()
        }
    }

    fun skipNextTrack() = musicCoordinator.skipNext()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun deleteItem(item: MediaItem) {
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
        slideshowVideoPlayer.release()
        super.onCleared()
    }

    private fun buildAlbumPills(config: SlideshowConfig, albums: List<MediaAlbum>): List<AlbumPillOption> {
        val pills = mutableListOf(AlbumPillOption(null, "Todos"))
        if (config.hasCustomMediaFolders()) {
            config.photoFolderUris.distinct().forEach { uri ->
                pills.add(AlbumPillOption(photoFolderPillId(uri), "Fotos: ${getFolderDisplayName(uri)}"))
            }
            config.videoFolderUris.distinct().forEach { uri ->
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
