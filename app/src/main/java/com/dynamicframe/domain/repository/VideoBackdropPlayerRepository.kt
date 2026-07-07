package com.dynamicframe.domain.repository

import androidx.media3.common.Player

/**
 * Segundo reproductor ExoPlayer (siempre mudo) para el fondo blur de vídeo en Paradise.
 * El reproductor principal sigue en [SlideshowVideoPlayerRepository].
 */
interface VideoBackdropPlayerRepository {
    val player: Player
    fun prepare(uri: String, playing: Boolean)
    fun setPlaying(playing: Boolean)
    fun stop()
    fun stopIfCurrent(uri: String)
    fun release()
}
