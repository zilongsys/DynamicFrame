package com.dynamicframe.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ExoSlideshowVideoPlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SlideshowVideoPlayerRepository {

    private var exoPlayer: ExoPlayer? = null
    private var attachedListener: Player.Listener? = null
    private var onEnded: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    override val player: Player
        get() = obtainPlayer()

    private fun obtainPlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { exoPlayer = it }
    }

    override fun prepare(uri: String, volume: Float, mute: Boolean, playing: Boolean) {
        runCatching {
            val p = obtainPlayer()
            p.stop()
            p.clearMediaItems()
            p.setMediaItem(MediaItem.fromUri(uri))
            p.volume = if (mute) 0f else volume.coerceIn(0f, 1f)
            p.prepare()
            if (playing) p.play() else p.pause()
        }.onFailure {
            onError?.invoke()
        }
    }

    override fun setListeners(onEnded: () -> Unit, onError: () -> Unit) {
        this.onEnded = onEnded
        this.onError = onError
        val p = obtainPlayer()
        attachedListener?.let { p.removeListener(it) }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    this@ExoSlideshowVideoPlayerRepository.onEnded?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                this@ExoSlideshowVideoPlayerRepository.onError?.invoke()
            }
        }
        attachedListener = listener
        p.addListener(listener)
    }

    override fun clearListeners() {
        attachedListener?.let { exoPlayer?.removeListener(it) }
        attachedListener = null
        onEnded = null
        onError = null
    }

    override fun release() {
        clearListeners()
        exoPlayer?.release()
        exoPlayer = null
    }
}
