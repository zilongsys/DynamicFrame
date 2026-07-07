package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.repository.ImageCacheRepository
import com.dynamicframe.domain.repository.MediaRepository
import com.dynamicframe.domain.repository.MusicRepository
import com.dynamicframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

class GetSlideshowItemsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<List<MediaItem>> {
        val config = settingsRepository.getConfig()

        val rawItems = when {
            config.hasCustomMediaFolders() -> {
                val items = mutableListOf<MediaItem>()
                val photoFolders = foldersForAlbumSelection(
                    config.activePhotoFolderUris(),
                    config.selectedAlbumIds,
                    MediaFolderKind.PHOTO
                )
                val videoFolders = foldersForAlbumSelection(
                    config.activeVideoFolderUris(),
                    config.selectedAlbumIds,
                    MediaFolderKind.VIDEO
                )

                if (photoFolders.isNotEmpty()) {
                    mediaRepository.getMediaFromFolders(photoFolders, MediaContentFilter.PHOTOS_ONLY)
                        .getOrNull()
                        ?.let { items.addAll(it) }
                }
                if (videoFolders.isNotEmpty()) {
                    mediaRepository.getMediaFromFolders(videoFolders, MediaContentFilter.VIDEOS_ONLY)
                        .getOrNull()
                        ?.let { items.addAll(it) }
                }

                Result.success(items.distinctBy { it.id })
            }
            config.selectedAlbumIds.isNotEmpty() -> {
                val items = mutableListOf<MediaItem>()
                config.selectedAlbumIds.forEach { albumId ->
                    mediaRepository.getMediaItems(albumId, MediaSource.LOCAL).getOrNull()?.let {
                        items.addAll(it)
                    }
                }
                Result.success(items)
            }
            else -> mediaRepository.getAllMediaItems(listOf(MediaSource.LOCAL))
        }

        return rawItems.map { items ->
            items.filter { config.mediaContentFilter.allows(it.type) }
                .sortedByDescending { it.dateAdded }
        }
    }

}

class ObserveSlideshowConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<SlideshowConfig> = settingsRepository.observeConfig()
}

class GetMusicTracksUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<List<MusicTrack>> {
        val config = settingsRepository.getConfig()
        // Multi-fuente: fusiona las pistas de todas las fuentes activas.
        val tracks = mutableListOf<MusicTrack>()
        config.musicSourceTypes.forEach { source ->
            val fromSource: List<MusicTrack> = when (source) {
                MusicSourceType.LOCAL_FOLDER -> {
                    val folders = config.activeMusicFolderUris()
                    if (folders.isEmpty()) emptyList()
                    else musicRepository.getTracksFromFolders(folders).getOrNull().orEmpty()
                }
                MusicSourceType.DEVICE_LIBRARY, MusicSourceType.THEME ->
                    musicRepository.getLocalTracks().getOrNull().orEmpty()
                MusicSourceType.SPOTIFY, MusicSourceType.YOUTUBE ->
                    emptyList()
            }
            tracks.addAll(fromSource)
        }
        return Result.success(tracks.distinctBy { it.id })
    }
}

class ObserveLocalMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> = mediaRepository.observeLocalMedia()
}

class GetLocalAlbumsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(): Result<List<MediaAlbum>> =
        mediaRepository.getAlbums(MediaSource.LOCAL)
}

class SaveSlideshowConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(config: SlideshowConfig) =
        settingsRepository.saveConfig(config)
}

class GetSlideshowConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): SlideshowConfig =
        settingsRepository.getConfig()
}

class DeleteMediaItemUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(item: MediaItem): Result<Unit> =
        mediaRepository.deleteMediaItem(item)
}

class EvictMediaCacheUseCase @Inject constructor(
    private val imageCacheRepository: ImageCacheRepository
) {
    operator fun invoke(uri: String) {
        imageCacheRepository.evict(uri)
    }
}

class PreloadSlideshowImagesUseCase @Inject constructor(
    private val imageCacheRepository: ImageCacheRepository
) {
    operator fun invoke(uris: Iterable<String>, width: Int, height: Int) {
        uris.forEach { uri -> imageCacheRepository.preload(uri, width, height) }
    }

    suspend fun awaitSharp(uris: Iterable<String>, width: Int, height: Int) {
        uris.forEach { uri -> imageCacheRepository.preloadAndAwait(uri, width, height) }
    }

    suspend fun awaitBlur(
        uri: String,
        width: Int,
        height: Int,
        blurRadius: Float = 25f,
        blurSampling: Float = 5f,
    ) {
        imageCacheRepository.preloadBlurAndAwait(uri, width, height, blurRadius, blurSampling)
    }
}

@Singleton
class UpdateSlideshowConfigUseCase @Inject constructor(
    private val getConfig: GetSlideshowConfigUseCase,
    private val saveConfig: SaveSlideshowConfigUseCase
) {
    private val mutex = Mutex()

    suspend operator fun invoke(transform: (SlideshowConfig) -> SlideshowConfig) {
        mutex.withLock {
            saveConfig(transform(getConfig()))
        }
    }
}

class UpdateSelectedAlbumsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumIds: List<String>) =
        settingsRepository.updateSelectedAlbums(albumIds)
}

class UpdateIntervalUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(seconds: Int) = settingsRepository.updateInterval(seconds)
}

class UpdateTransitionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(type: TransitionType) = settingsRepository.updateTransition(type)
}

class UpdateMusicVolumeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(volume: Float) = settingsRepository.updateMusicVolume(volume)
}

class ToggleShuffleUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.toggleShuffle(enabled)
}

class ToggleClockUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.toggleClock(enabled)
}
