package com.dynamicframe.data.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dynamicframe.domain.model.MusicPlayerState
import com.dynamicframe.domain.model.MusicTrack
import com.dynamicframe.domain.model.VideoMusicBehavior
import com.dynamicframe.domain.repository.MusicPlaybackRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicPlaybackRepository {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val connectMutex = Mutex()
    private var connectionDeferred: CompletableDeferred<Unit>? = null

    private val _state = MutableStateFlow(MusicPlayerState())
    override val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private var currentPlaylist: List<MusicTrack> = emptyList()
    private var pendingPlaylist: Pair<List<MusicTrack>, Int>? = null
    private var volumeBeforeVideo: Float? = null
    private var wasPlayingBeforeVideo = false

    /** Espera a que el MediaController esté listo (connect es asíncrono). */
    override suspend fun ensureConnected() = connectMutex.withLock {
        controller?.let { return }
        val deferred = connectionDeferred ?: CompletableDeferred<Unit>().also {
            connectionDeferred = it
            beginConnection(it)
        }
        deferred.await()
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val deferred = CompletableDeferred<Unit>()
        connectionDeferred = deferred
        beginConnection(deferred)
    }

    private fun beginConnection(deferred: CompletableDeferred<Unit>) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                flushPendingPlaylist()
                syncState()
                if (!deferred.isCompleted) deferred.complete(Unit)
            } catch (e: Exception) {
                controller = null
                controllerFuture = null
                connectionDeferred = null
                if (!deferred.isCompleted) deferred.completeExceptionally(e)
            }
        }, { it.run() })
    }

    override fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        connectionDeferred?.cancel()
        connectionDeferred = null
        currentPlaylist = emptyList()
        _state.value = MusicPlayerState()
    }

    override fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int, autoPlay: Boolean) {
        currentPlaylist = tracks
        if (controller == null) {
            pendingPlaylist = tracks to startIndex
            pendingAutoPlay = autoPlay
            _state.value = _state.value.copy(playlist = tracks, isPlaying = false)
            return
        }
        applyPlaylist(tracks, startIndex, autoPlay)
    }

    private var pendingAutoPlay: Boolean = false

    private fun applyPlaylist(tracks: List<MusicTrack>, startIndex: Int, autoPlay: Boolean) {
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .build()
        }
        controller?.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            if (autoPlay) play() else pause()
        }
        _state.value = _state.value.copy(
            playlist = tracks,
            isPlaying = autoPlay && controller?.isPlaying == true
        )
    }

    private fun flushPendingPlaylist() {
        pendingPlaylist?.let { (tracks, index) ->
            val autoPlay = pendingAutoPlay
            pendingPlaylist = null
            pendingAutoPlay = false
            applyPlaylist(tracks, index, autoPlay)
        }
    }

    override fun play() {
        controller?.play()
        syncState()
    }

    override fun pause() {
        controller?.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() = controller?.seekToPreviousMediaItem()

    override fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        controller?.volume = v
        _state.value = _state.value.copy(volume = v)
        if (_state.value.isDucked) {
            volumeBeforeVideo = v
        }
    }

    override fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        _state.value = _state.value.copy(isShuffle = enabled)
    }

    override fun onVideoStarted(behavior: VideoMusicBehavior, duckVolume: Float) {
        if (_state.value.isDucked) return
        volumeBeforeVideo = controller?.volume ?: _state.value.volume
        wasPlayingBeforeVideo = controller?.isPlaying == true
        when (behavior) {
            VideoMusicBehavior.PAUSE -> if (wasPlayingBeforeVideo) pause()
            VideoMusicBehavior.DUCK -> setVolume(duckVolume.coerceIn(0f, 1f))
        }
        _state.value = _state.value.copy(isDucked = true)
    }

    override fun onPhotoShown(normalVolume: Float) {
        if (!_state.value.isDucked) return
        val restoreVolume = volumeBeforeVideo ?: normalVolume
        setVolume(restoreVolume)
        if (wasPlayingBeforeVideo) play()
        volumeBeforeVideo = null
        wasPlayingBeforeVideo = false
        _state.value = _state.value.copy(isDucked = false)
    }

    private fun syncState() {
        controller?.let { ctrl ->
            val idx = ctrl.currentMediaItemIndex
            _state.value = _state.value.copy(
                isPlaying = ctrl.isPlaying,
                currentTrack = currentPlaylist.getOrNull(idx),
                currentIndex = idx,
                volume = ctrl.volume,
                isShuffle = ctrl.shuffleModeEnabled
            )
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = controller?.currentMediaItemIndex ?: 0
            _state.value = _state.value.copy(
                currentTrack = currentPlaylist.getOrNull(idx),
                currentIndex = idx
            )
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            if (currentPlaylist.isNotEmpty()) {
                controller?.seekToNextMediaItem()
                syncState()
            }
        }
    }
}
