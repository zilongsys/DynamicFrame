package com.dynamicframe.domain.repository

import androidx.media3.common.Player

/** Reproductor de vídeo del slideshow (implementación ExoPlayer en `data/`). */
interface SlideshowVideoPlayerRepository {
    val player: Player
    /** Carga y prepara un vídeo. Solo debe llamarse al CAMBIAR de URI. */
    fun prepare(uri: String, volume: Float, mute: Boolean, playing: Boolean)
    /** Reanuda/pausa sin reiniciar el vídeo. */
    fun setPlaying(playing: Boolean)
    /** Ajusta volumen/mute sin reiniciar el vídeo. */
    fun setVolume(volume: Float, mute: Boolean)
    /** Detiene la reproducción y libera el media item (al salir del slide de vídeo). */
    fun stop()
    /**
     * Detiene solo si el vídeo cargado sigue siendo [uri]. Evita que un slide al
     * desmontarse detenga el vídeo que ya cargó el siguiente slide (vídeo→vídeo).
     */
    fun stopIfCurrent(uri: String)
    fun setListeners(onEnded: () -> Unit, onError: () -> Unit)
    fun clearListeners()
    fun release()
}
