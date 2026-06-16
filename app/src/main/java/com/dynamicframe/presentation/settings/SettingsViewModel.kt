package com.dynamicframe.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.domain.usecase.GetLocalAlbumsUseCase
import com.dynamicframe.domain.usecase.ObserveSlideshowConfigUseCase
import com.dynamicframe.domain.usecase.SaveSlideshowConfigUseCase
import com.dynamicframe.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeConfig: ObserveSlideshowConfigUseCase,
    private val saveConfig: SaveSlideshowConfigUseCase,
    private val settingsRepository: SettingsRepository,
    private val getLocalAlbums: GetLocalAlbumsUseCase
) : ViewModel() {

    private val configMutex = Mutex()

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
        viewModelScope.launch { settingsRepository.updateInterval(seconds) }
    }

    fun updateTransition(type: TransitionType) {
        viewModelScope.launch { settingsRepository.updateTransition(type) }
    }

    fun updateMusicVolume(volume: Float) {
        viewModelScope.launch { settingsRepository.updateMusicVolume(volume) }
    }

    fun toggleShuffle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.toggleShuffle(enabled) }
    }

    fun toggleClock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.toggleClock(enabled) }
    }

    fun updateSelectedAlbums(albumIds: List<String>) {
        viewModelScope.launch { settingsRepository.updateSelectedAlbums(albumIds) }
    }

    fun addPhotoFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                if (current.photoFolderUris.contains(uri)) return@withLock
                saveConfig(current.copy(photoFolderUris = current.photoFolderUris + uri))
            }
        }
    }

    fun removePhotoFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                saveConfig(current.copy(photoFolderUris = current.photoFolderUris - uri))
            }
        }
    }

    fun addVideoFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                if (current.videoFolderUris.contains(uri)) return@withLock
                saveConfig(current.copy(videoFolderUris = current.videoFolderUris + uri))
            }
        }
    }

    fun removeVideoFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                saveConfig(current.copy(videoFolderUris = current.videoFolderUris - uri))
            }
        }
    }

    fun addMusicFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                if (current.musicFolderUris.contains(uri)) return@withLock
                saveConfig(current.copy(musicFolderUris = current.musicFolderUris + uri))
            }
        }
    }

    fun removeMusicFolder(uri: String) {
        viewModelScope.launch {
            configMutex.withLock {
                val current = settingsRepository.getConfig()
                saveConfig(current.copy(musicFolderUris = current.musicFolderUris - uri))
            }
        }
    }
}
