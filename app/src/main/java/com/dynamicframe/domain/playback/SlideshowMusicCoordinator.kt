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

    /** Si el aleatorio está activo, baraja la lista ANTES de reproducir y se reproduce en ese orden. */
    private fun ordered(tracks: List<MusicTrack>, shuffle: Boolean): List<MusicTrack> =
        if (shuffle) tracks.shuffled() else tracks

    /** Compara por CONJUNTO de ids (no por orden): así una lista ya barajada no se considera obsoleta. */
    private fun sameTracks(a: List<MusicTrack>, b: List<MusicTrack>): Boolean =
        a.map { it.id }.toSet() == b.map { it.id }.toSet()

    suspend fun startForSession(freshSession: Boolean) {
        runCatching {
            val config = getConfig()
            val tracks = getMusicTracks().getOrDefault(emptyList())
            if (tracks.isEmpty()) return@runCatching

            music.ensureConnected()
            if (freshSession) {
                // Barajar la lista completa y empezar por el principio del orden ya barajado.
                val playlist = ordered(tracks, config.musicShuffle)
                music.setPlaylist(playlist, startIndex = 0, autoPlay = true)
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
            // Si la sesión fue interrumpida durante un vídeo (isDucked=true), el estado
            // de atenuación quedó colgado. Lo limpiamos ANTES de restaurar el volumen
            // para que setVolume() no piense que sigue en modo duck y sobrescriba volumeBeforeVideo.
            if (music.state.value.isDucked) music.resetDuckedState()

            val current = music.state.value
            val playlistStale = current.playlist.isEmpty() || !sameTracks(current.playlist, tracks)

            if (playlistStale) {
                music.setPlaylist(ordered(tracks, config.musicShuffle), autoPlay = true)
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
                !sameTracks(music.state.value.playlist, tracks)

            if (!force && !playlistStale && !playAfter) {
                music.setVolume(config.musicVolume)
                return@runCatching
            }

            music.ensureConnected()
            music.setPlaylist(ordered(tracks, config.musicShuffle), autoPlay = playAfter)
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
        musicSourceTypes: Set<com.dynamicframe.domain.model.MusicSourceType>,
        musicFolderUris: List<String>,
        disabledMusicFolderUris: Set<String>,
        musicShuffle: Boolean
    ): String = "$musicSourceTypes|$musicFolderUris|$disabledMusicFolderUris|$musicShuffle"
}
