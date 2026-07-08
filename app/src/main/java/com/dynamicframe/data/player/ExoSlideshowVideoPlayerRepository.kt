package com.dynamicframe.data.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.repository.SlideshowVideoPlayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoSlideshowVideoPlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debug: AppDebugLogger
) : SlideshowVideoPlayerRepository {

    private var exoPlayer: ExoPlayer? = null
    private var attachedListener: Player.Listener? = null
    private var onEnded: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null
    private var currentUri: String? = null
    private var audioMuted: Boolean = true

    override val player: Player
        get() = obtainPlayer()

    private fun obtainPlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().apply {
            applyAudioAttributes(this, audioMuted)
        }.also { exoPlayer = it }
    }

    private fun applyAudioAttributes(player: ExoPlayer, muted: Boolean) {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ !muted,
        )
    }

    override fun prepare(uri: String, volume: Float, mute: Boolean, playing: Boolean) {
        debug.d("Video", "prepare playing=$playing mute=$mute uri=${uri.takeLast(48)}")
        runCatching {
            val p = obtainPlayer()
            audioMuted = mute
            applyAudioAttributes(p, mute)
            p.setMediaItem(MediaItem.fromUri(uri))
            p.volume = if (mute) 0f else volume.coerceIn(0f, 1f)
            p.prepare()
            if (playing) p.play() else p.pause()
            currentUri = uri
        }.onFailure {
            debug.e("Video", "prepare falló", it.message)
            currentUri = null
            onError?.invoke()
        }
    }

    override fun setPlaying(playing: Boolean) {
        runCatching {
            val p = exoPlayer ?: return
            if (playing) p.play() else p.pause()
        }
    }

    override fun setVolume(volume: Float, mute: Boolean) {
        runCatching {
            val p = exoPlayer ?: return
            if (audioMuted != mute) {
                audioMuted = mute
                applyAudioAttributes(p, mute)
            }
            p.volume = if (mute) 0f else volume.coerceIn(0f, 1f)
        }
    }

    override fun stop() {
        runCatching {
            exoPlayer?.let {
                it.stop()
                it.clearMediaItems()
            }
            currentUri = null
        }
    }

    override fun stopIfCurrent(uri: String) {
        if (currentUri == uri) stop()
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
                debug.e("Video", "onPlayerError", error.errorCodeName)
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
