package com.dynamicframe.presentation.slideshow

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.data.player.MusicPlayerController
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.repository.SettingsRepository
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.GetMusicTracksUseCase
import com.dynamicframe.domain.usecase.GetSlideshowConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
    }

    fun startMusicIfReady() {
        viewModelScope.launch { setupMusic() }
    }

    private suspend fun setupMusic() {
        val config = getConfig()
        val tracks = getMusicTracks().getOrDefault(emptyList())
        if (tracks.isEmpty()) return
        musicController.ensureConnected()
        musicController.setPlaylist(tracks)
        musicController.setVolume(config.musicVolume)
        musicController.setShuffle(config.musicShuffle)
    }

    fun selectAlbum(albumId: String?) {
        viewModelScope.launch {
            settingsRepository.updateSelectedAlbums(
                if (albumId == null) emptyList() else listOf(albumId)
            )
            slideshowEngine.loadMedia()
        }
    }

    fun startSlideshow() {
        slideshowEngine.start()
        viewModelScope.launch { startMusicForSlideshow() }
    }

    private suspend fun startMusicForSlideshow() {
        val config = getConfig()
        val tracks = getMusicTracks().getOrDefault(emptyList())
        if (tracks.isEmpty()) return
        musicController.ensureConnected()
        val current = musicController.state.value
        if (current.playlist.isEmpty()) {
            musicController.setPlaylist(tracks)
            musicController.setVolume(config.musicVolume)
            musicController.setShuffle(config.musicShuffle)
        } else {
            musicController.setVolume(config.musicVolume)
            musicController.play()
        }
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
        viewModelScope.launch {
            if (musicState.value.isPlaying) {
                musicController.pause()
            } else {
                if (musicController.state.value.playlist.isEmpty()) {
                    setupMusic()
                } else {
                    musicController.ensureConnected()
                    musicController.play()
                }
            }
        }
    }

    fun skipNextTrack() = musicController.skipNext()

    // SlideshowEngine y MusicPlayerController son @Singleton de app: no destruir al salir de una pantalla.
    override fun onCleared() {
        super.onCleared()
    }

    private fun buildAlbumPills(config: SlideshowConfig, albums: List<MediaAlbum>): List<AlbumPillOption> {
        val pills = mutableListOf(AlbumPillOption(null, "Todos"))
        if (config.mediaFolderUris.isNotEmpty()) {
            config.mediaFolderUris.forEach { uri ->
                pills.add(AlbumPillOption(uri, folderLabel(uri)))
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
