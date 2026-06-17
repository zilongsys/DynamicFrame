package com.dynamicframe.domain.playback

import com.dynamicframe.domain.model.MusicTrack
import com.dynamicframe.domain.repository.MusicPlaybackRepository
import com.dynamicframe.domain.usecase.GetMusicTracksUseCase
import com.dynamicframe.domain.usecase.GetSlideshowConfigUseCase
import javax.inject.Inject
import javax.inject.Singleton

/** Orquesta música de fondo del slideshow (capa de dominio). */
@Singleton
class SlideshowMusicCoordinator @Inject constructor(
    private val music: MusicPlaybackRepository,
    private val getMusicTracks: GetMusicTracksUseCase,
    private val getConfig: GetSlideshowConfigUseCase
) {
    val state get() = music.state

    suspend fun startForSession(freshSession: Boolean) {
        runCatching {
            val config = getConfig()
            val tracks = getMusicTracks().getOrDefault(emptyList())
            if (tracks.isEmpty()) return@runCatching

            music.ensureConnected()
            if (freshSession) {
                val startIndex = if (config.musicShuffle && tracks.isNotEmpty()) {
                    tracks.indices.random()
                } else {
                    0
                }
                music.setPlaylist(tracks, startIndex = startIndex, autoPlay = true)
                music.setShuffle(config.musicShuffle)
            } else {
                resumePlayback()
            }
            music.setVolume(config.musicVolume)
        }
    }

    suspend fun resumePlayback() {
        runCatching {
            val config = getConfig()
            val tracks = getMusicTracks().getOrDefault(emptyList())
            if (tracks.isEmpty()) return@runCatching

            music.ensureConnected()
            val current = music.state.value
            val playlistStale = current.playlist.isEmpty() ||
                current.playlist.map { it.id } != tracks.map { it.id }

            if (playlistStale) {
                music.setPlaylist(tracks, autoPlay = true)
                music.setShuffle(config.musicShuffle)
            } else if (!current.isPlaying) {
                music.play()
            }
            music.setVolume(config.musicVolume)
        }
    }

    suspend fun refreshPlaylist(force: Boolean = false, playAfter: Boolean = false) {
        runCatching {
            val config = getConfig()
            val tracks = getMusicTracks().getOrDefault(emptyList())
            if (tracks.isEmpty()) return@runCatching

            val playlistStale = music.state.value.playlist.isEmpty() ||
                music.state.value.playlist.map { it.id } != tracks.map { it.id }

            if (!force && !playlistStale && !playAfter) {
                music.setVolume(config.musicVolume)
                return@runCatching
            }

            music.ensureConnected()
            music.setPlaylist(tracks, autoPlay = playAfter)
            music.setVolume(config.musicVolume)
            music.setShuffle(config.musicShuffle)
        }
    }

    fun pause() = music.pause()

    fun play() = music.play()

    fun setVolume(volume: Float) = music.setVolume(volume)

    fun skipNext() = music.skipNext()

    fun onVideoStarted(behavior: com.dynamicframe.domain.model.VideoMusicBehavior, duckVolume: Float) =
        music.onVideoStarted(behavior, duckVolume)

    fun onPhotoShown(normalVolume: Float) = music.onPhotoShown(normalVolume)

    fun disconnect() = music.disconnect()

    fun musicConfigKey(
        musicSourceType: com.dynamicframe.domain.model.MusicSourceType,
        musicFolderUris: List<String>,
        musicShuffle: Boolean
    ): String = "$musicSourceType|$musicFolderUris|$musicShuffle"
}
