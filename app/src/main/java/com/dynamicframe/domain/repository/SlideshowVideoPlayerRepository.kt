package com.dynamicframe.domain.repository

import androidx.media3.common.Player

/** Reproductor de vídeo del slideshow (implementación ExoPlayer en `data/`). */
interface SlideshowVideoPlayerRepository {
    val player: Player
    fun prepare(uri: String, volume: Float, mute: Boolean, playing: Boolean)
    fun setListeners(onEnded: () -> Unit, onError: () -> Unit)
    fun clearListeners()
    fun release()
}
