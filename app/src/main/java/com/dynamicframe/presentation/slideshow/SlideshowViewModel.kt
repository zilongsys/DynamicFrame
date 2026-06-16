package com.dynamicframe.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.data.player.MusicPlayerController
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.domain.model.photoFolderPillId
import com.dynamicframe.domain.model.videoFolderPillId
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.repository.SettingsRepository
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.usecase.DeleteMediaItemUseCase
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.GetMusicTracksUseCase
import com.dynamicframe.domain.usecase.GetSlideshowConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumPillOption(val id: String?, val label: String)

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val slideshowEngine: SlideshowEngine,
    private val musicController: MusicPlayerController,
    private val getMusicTracks: GetMusicTracksUseCase,
    private val getConfig: GetSlideshowConfigUseCase,
    private val getLocalAlbums: GetLocalAlbumsUseCase,
    private val deleteMediaItem: DeleteMediaItemUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val slideshowState = slideshowEngine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), slideshowEngine.state.value)

    val slideshowConfig = slideshowEngine.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowConfig())

    val musicState = musicController.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), musicController.state.value)

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
                        MediaType.VIDEO -> musicController.onVideoStarted(
                            config.videoMusicBehavior,
                            config.duckedMusicVolume
                        )
                        MediaType.IMAGE -> musicController.onPhotoShown(config.musicVolume)
                        null -> Unit
                    }
                }
        }

        viewModelScope.launch {
            slideshowConfig
                .map { musicConfigKey(it) }
                .distinctUntilChanged()
                .collect { refreshMusicPlaylist(force = true) }
        }
    }

    fun selectAlbum(albumId: String?) {
        viewModelScope.launch {
            settingsRepository.updateSelectedAlbums(
                if (albumId == null) emptyList() else listOf(albumId)
            )
        }
    }

    fun startSlideshow() {
        slideshowEngine.start()
        viewModelScope.launch { refreshMusicPlaylist(playAfter = true) }
    }

    fun pauseSlideshow() {
        slideshowEngine.pause()
        musicController.pause()
    }

    fun nextSlide() = slideshowEngine.next()
    fun previousSlide() = slideshowEngine.previous()
    fun onVideoCompleted() = slideshowEngine.onVideoCompleted()
    fun jumpToSlide(index: Int) = slideshowEngine.jumpTo(index)

    fun reloadMedia() {
        viewModelScope.launch { slideshowEngine.loadMedia() }
    }

    fun setMusicVolume(volume: Float) = musicController.setVolume(volume)

    fun toggleMusicPlayback() {
        if (slideshowState.value.isPlaying) {
            pauseSlideshow()
        } else {
            startSlideshow()
        }
    }

    fun skipNextTrack() = musicController.skipNext()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            deleteMediaItem(item)
                .onSuccess { slideshowEngine.removeItem(item.id) }
                .onFailure { e ->
                    _toastMessage.value = e.message ?: "No se pudo borrar el archivo"
                }
        }
    }

    fun deleteCurrentSlide() {
        slideshowState.value.currentItem?.let { deleteItem(it) }
    }

    override fun onCleared() {
        super.onCleared()
    }

    private suspend fun refreshMusicPlaylist(force: Boolean = false, playAfter: Boolean = false) {
        runCatching {
            val config = getConfig()
            val tracks = getMusicTracks().getOrDefault(emptyList())
            if (tracks.isEmpty()) return@runCatching

            val playlistStale = musicController.state.value.playlist.isEmpty() ||
                musicController.state.value.playlist.map { it.id } != tracks.map { it.id }

            if (!force && !playlistStale && !playAfter) {
                musicController.setVolume(config.musicVolume)
                return@runCatching
            }

            musicController.ensureConnected()
            musicController.setPlaylist(tracks)
            musicController.setVolume(config.musicVolume)
            musicController.setShuffle(config.musicShuffle)
            if (playAfter) musicController.play()
        }
    }

    private fun musicConfigKey(config: SlideshowConfig): String =
        "${config.musicSourceType}|${config.musicFolderUris}|${config.musicShuffle}"

    private fun buildAlbumPills(config: SlideshowConfig, albums: List<MediaAlbum>): List<AlbumPillOption> {
        val pills = mutableListOf(AlbumPillOption(null, "Todos"))
        if (config.hasCustomMediaFolders()) {
            config.photoFolderUris.distinct().forEach { uri ->
                pills.add(AlbumPillOption(photoFolderPillId(uri), "Fotos: ${folderLabel(uri)}"))
            }
            config.videoFolderUris.distinct().forEach { uri ->
                pills.add(AlbumPillOption(videoFolderPillId(uri), "Videos: ${folderLabel(uri)}"))
            }
        } else {
            albums.forEach { album ->
                pills.add(AlbumPillOption(album.id, album.name))
            }
        }
        return pills
    }

    private fun folderLabel(uriString: String): String =
        com.dynamicframe.data.local.LocalStorageBrowser.folderDisplayName(uriString)
}
