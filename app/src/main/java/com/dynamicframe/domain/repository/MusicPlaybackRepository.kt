package com.dynamicframe.domain.repository

import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.MusicTrack
import com.dynamicframe.domain.model.VideoMusicBehavior
import kotlinx.coroutines.flow.StateFlow

interface MusicPlaybackRepository {
    val state: StateFlow<MusicPlayerState>

    suspend fun ensureConnected()

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int = 0, autoPlay: Boolean = false)

    fun play()

    fun pause()

    fun skipNext()

    fun setVolume(volume: Float)

    fun setShuffle(enabled: Boolean)

    fun onVideoStarted(behavior: VideoMusicBehavior, duckVolume: Float)

    fun onPhotoShown(normalVolume: Float)

    fun disconnect()
}
