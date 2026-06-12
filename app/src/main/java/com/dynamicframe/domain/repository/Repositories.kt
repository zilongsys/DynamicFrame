package com.dynamicframe.domain.repository

import com.dynamicframe.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getAlbums(source: MediaSource): Result<List<MediaAlbum>>
    suspend fun getMediaItems(albumId: String, source: MediaSource): Result<List<MediaItem>>
    suspend fun getAllMediaItems(sources: List<MediaSource>): Result<List<MediaItem>>
    suspend fun getMediaFromFolders(
        folderUris: List<String>,
        filter: MediaContentFilter
    ): Result<List<MediaItem>>
    fun observeLocalMedia(): Flow<List<MediaItem>>
}

interface MusicRepository {
    suspend fun getLocalTracks(): Result<List<MusicTrack>>
    suspend fun getTracksFromFolder(folderUri: String): Result<List<MusicTrack>>
    suspend fun getLocalAlbums(): Result<List<MediaAlbum>>
    fun observeLocalTracks(): Flow<List<MusicTrack>>
}

interface SettingsRepository {
    fun observeConfig(): Flow<SlideshowConfig>
    suspend fun getConfig(): SlideshowConfig
    suspend fun saveConfig(config: SlideshowConfig)
    suspend fun updateInterval(seconds: Int)
    suspend fun updateTransition(type: TransitionType)
    suspend fun updateMusicVolume(volume: Float)
    suspend fun toggleShuffle(enabled: Boolean)
    suspend fun toggleClock(enabled: Boolean)
    suspend fun updateSelectedAlbums(albumIds: List<String>)
}
