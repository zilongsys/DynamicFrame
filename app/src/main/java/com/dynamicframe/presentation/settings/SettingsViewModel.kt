package com.dynamicframe.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.domain.usecase.GetFolderDisplayNameUseCase
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.ListStorageRootsUseCase
import com.dynamicframe.domain.usecase.ListStorageSubfoldersUseCase
import com.dynamicframe.domain.usecase.ObserveSlideshowConfigUseCase
import com.dynamicframe.domain.usecase.SaveSlideshowConfigUseCase
import com.dynamicframe.domain.usecase.ToggleClockUseCase
import com.dynamicframe.domain.usecase.ToggleShuffleUseCase
import com.dynamicframe.domain.usecase.UpdateIntervalUseCase
import com.dynamicframe.domain.usecase.UpdateMusicVolumeUseCase
import com.dynamicframe.domain.usecase.UpdateSelectedAlbumsUseCase
import com.dynamicframe.domain.usecase.UpdateSlideshowConfigUseCase
import com.dynamicframe.domain.usecase.UpdateTransitionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeConfig: ObserveSlideshowConfigUseCase,
    private val saveConfig: SaveSlideshowConfigUseCase,
    private val updateSlideshowConfig: UpdateSlideshowConfigUseCase,
    private val setInterval: UpdateIntervalUseCase,
    private val setTransition: UpdateTransitionUseCase,
    private val setMusicVolume: UpdateMusicVolumeUseCase,
    private val setShuffle: ToggleShuffleUseCase,
    private val setClock: ToggleClockUseCase,
    private val setSelectedAlbums: UpdateSelectedAlbumsUseCase,
    private val getLocalAlbums: GetLocalAlbumsUseCase,
    private val getFolderDisplayName: GetFolderDisplayNameUseCase,
    private val listStorageRoots: ListStorageRootsUseCase,
    private val listStorageSubfolders: ListStorageSubfoldersUseCase
) : ViewModel() {

    val config: StateFlow<SlideshowConfig> = observeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowConfig())

    private val _albums = MutableStateFlow<List<MediaAlbum>>(emptyList())
    val albums: StateFlow<List<MediaAlbum>> = _albums.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _albums.value = runCatching {
                getLocalAlbums().getOrDefault(emptyList())
            }.getOrDefault(emptyList())
        }
    }

    fun updateConfig(newConfig: SlideshowConfig) {
        viewModelScope.launch { saveConfig(newConfig) }
    }

    fun updateInterval(seconds: Int) {
        viewModelScope.launch { setInterval(seconds) }
    }

    fun updateTransition(type: TransitionType) {
        viewModelScope.launch { setTransition(type) }
    }

    fun updateMusicVolume(volume: Float) {
        viewModelScope.launch { setMusicVolume(volume) }
    }

    fun toggleShuffle(enabled: Boolean) {
        viewModelScope.launch { setShuffle(enabled) }
    }

    fun toggleClock(enabled: Boolean) {
        viewModelScope.launch { setClock(enabled) }
    }

    fun updatePhotoShuffle(enabled: Boolean) = updateConfigField { it.copy(photoShuffle = enabled) }

    fun updateVideoShuffle(enabled: Boolean) = updateConfigField { it.copy(videoShuffle = enabled) }

    fun updateMusicShuffle(enabled: Boolean) = updateConfigField { it.copy(musicShuffle = enabled) }

    fun updateLoop(enabled: Boolean) = updateConfigField { it.copy(loop = enabled) }

    fun updateShowDate(enabled: Boolean) = updateConfigField { it.copy(showDate = enabled) }

    private fun updateConfigField(transform: (SlideshowConfig) -> SlideshowConfig) {
        viewModelScope.launch { updateSlideshowConfig(transform) }
    }

    fun updateSelectedAlbums(albumIds: List<String>) {
        viewModelScope.launch { setSelectedAlbums(albumIds) }
    }

    fun addPhotoFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { current ->
                if (current.photoFolderUris.contains(uri)) current
                else current.copy(photoFolderUris = current.photoFolderUris + uri)
            }
        }
    }

    fun removePhotoFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { it.copy(photoFolderUris = it.photoFolderUris - uri) }
        }
    }

    fun addVideoFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { current ->
                if (current.videoFolderUris.contains(uri)) current
                else current.copy(videoFolderUris = current.videoFolderUris + uri)
            }
        }
    }

    fun removeVideoFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { it.copy(videoFolderUris = it.videoFolderUris - uri) }
        }
    }

    fun addMusicFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { current ->
                if (current.musicFolderUris.contains(uri)) current
                else current.copy(musicFolderUris = current.musicFolderUris + uri)
            }
        }
    }

    fun removeMusicFolder(uri: String) {
        viewModelScope.launch {
            updateSlideshowConfig { it.copy(musicFolderUris = it.musicFolderUris - uri) }
        }
    }

    fun folderLabel(uri: String): String = getFolderDisplayName(uri)

    fun storageRoots() = listStorageRoots()

    fun storageSubfolders(folderUri: String) = listStorageSubfolders(folderUri)
}
