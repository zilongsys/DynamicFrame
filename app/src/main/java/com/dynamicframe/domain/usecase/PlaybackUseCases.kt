package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.model.StorageRoot
import com.dynamicframe.domain.model.StorageSubfolder
import com.dynamicframe.domain.playback.SlideshowMusicCoordinator
import com.dynamicframe.domain.repository.MusicPlaybackRepository
import com.dynamicframe.domain.repository.StorageBrowserRepository
import com.dynamicframe.domain.slideshow.SlideshowEngine
import kotlinx.coroutines.flow.StateFlow
import com.dynamicframe.domain.model.MusicPlayerState
import javax.inject.Inject

class ObserveMusicPlaybackUseCase @Inject constructor(
    private val music: MusicPlaybackRepository
) {
    operator fun invoke(): StateFlow<MusicPlayerState> = music.state
}

class GetFolderDisplayNameUseCase @Inject constructor(
    private val storageBrowser: StorageBrowserRepository
) {
    operator fun invoke(folderUri: String): String = storageBrowser.displayName(folderUri)
}

class ListStorageRootsUseCase @Inject constructor(
    private val storageBrowser: StorageBrowserRepository
) {
    operator fun invoke(): List<StorageRoot> = storageBrowser.listDefaultRoots()
}

class ListStorageSubfoldersUseCase @Inject constructor(
    private val storageBrowser: StorageBrowserRepository
) {
    operator fun invoke(folderUri: String): List<StorageSubfolder> =
        storageBrowser.listSubfolders(folderUri)
}

class PauseAppPlaybackUseCase @Inject constructor(
    private val slideshowEngine: SlideshowEngine,
    private val musicCoordinator: SlideshowMusicCoordinator
) {
    fun pauseAll() {
        slideshowEngine.pause()
        musicCoordinator.pause()
    }

    fun disconnectAll() {
        slideshowEngine.pause()
        musicCoordinator.disconnect()
    }
}
