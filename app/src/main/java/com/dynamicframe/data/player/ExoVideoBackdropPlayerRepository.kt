package com.dynamicframe.data.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dynamicframe.domain.repository.VideoBackdropPlayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoVideoBackdropPlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoBackdropPlayerRepository {

    private var exoPlayer: ExoPlayer? = null
    private var currentUri: String? = null

    override val player: Player
        get() = obtainPlayer()

    private fun obtainPlayer(): ExoPlayer =
        exoPlayer ?: ExoPlayer.Builder(context).build().apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                false,
            )
        }.also { exoPlayer = it }

    override fun prepare(uri: String, playing: Boolean) {
        runCatching {
            val p = obtainPlayer()
            p.setMediaItem(MediaItem.fromUri(uri))
            p.volume = 0f
            p.prepare()
            if (playing) p.play() else p.pause()
            currentUri = uri
        }
    }

    override fun setPlaying(playing: Boolean) {
        runCatching {
            val p = exoPlayer ?: return
            if (playing) p.play() else p.pause()
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

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        currentUri = null
    }
}
