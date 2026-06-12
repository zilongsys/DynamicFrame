package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.repository.MediaRepository
import com.dynamicframe.domain.repository.MusicRepository
import com.dynamicframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSlideshowItemsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<List<MediaItem>> {
        val config = settingsRepository.getConfig()

        val rawItems = when {
            config.mediaFolderUris.isNotEmpty() -> {
                val folders = if (config.selectedAlbumIds.isEmpty()) {
                    config.mediaFolderUris
                } else {
                    config.selectedAlbumIds.filter { it.startsWith("content://") }
                        .ifEmpty { config.mediaFolderUris }
                }
                mediaRepository.getMediaFromFolders(folders, config.mediaContentFilter)
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
        return when (config.musicSourceType) {
            MusicSourceType.LOCAL_FOLDER -> {
                val uri = config.musicFolderUri
                if (uri.isNullOrBlank()) Result.success(emptyList())
                else musicRepository.getTracksFromFolder(uri)
            }
            MusicSourceType.DEVICE_LIBRARY, MusicSourceType.THEME ->
                musicRepository.getLocalTracks()
            MusicSourceType.SPOTIFY, MusicSourceType.YOUTUBE ->
                Result.success(emptyList())
        }.map { tracks -> tracks }
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
