package com.dynamicframe.domain.slideshow

import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.domain.usecase.GetSlideshowItemsUseCase
import com.dynamicframe.domain.usecase.ObserveSlideshowConfigUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SlideshowEngine @Inject constructor(
    private val getSlideshowItems: GetSlideshowItemsUseCase,
    private val observeConfig: ObserveSlideshowConfigUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loadMutex = Mutex()

    private val _state = MutableStateFlow(SlideshowState())
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    private val _config = MutableStateFlow(SlideshowConfig())
    val config: StateFlow<SlideshowConfig> = _config.asStateFlow()

    private var mediaItems: List<MediaItem> = emptyList()
    private var shuffledItems: List<MediaItem> = emptyList()
    private var timerJob: Job? = null
    private var transitionJob: Job? = null
    private var isInitialized = false
    private var lastMediaKey: String = ""
    private var lastScheduleKey: String = ""

    init {
        scope.launch {
            observeConfig().collect { config ->
                val previousConfig = _config.value
                _config.value = config

                val mediaKey = "${config.photoFolderUris}|${config.videoFolderUris}|${config.mediaContentFilter}|${config.selectedAlbumIds}"
                if (mediaKey != lastMediaKey) {
                    lastMediaKey = mediaKey
                    if (isInitialized) loadMedia()
                } else if (
                    (previousConfig.photoShuffle != config.photoShuffle ||
                        previousConfig.videoShuffle != config.videoShuffle) &&
                    mediaItems.isNotEmpty()
                ) {
                    applyShuffleKeepingCurrent(config)
                }

                val scheduleKey = "${config.intervalSeconds}|${config.videoPlayFull}|${config.loop}"
                if (scheduleKey != lastScheduleKey) {
                    lastScheduleKey = scheduleKey
                    if (_state.value.isPlaying) scheduleNext()
                }
            }
        }
    }

    suspend fun initialize() {
        if (isInitialized) return
        loadMedia()
        isInitialized = true
        lastScheduleKey = scheduleKeyFor(_config.value)
    }

    suspend fun loadMedia() = loadMutex.withLock {
        val previous = _state.value
        val result = withContext(Dispatchers.IO) { getSlideshowItems() }
        withContext(Dispatchers.Main.immediate) {
            result.onSuccess { items ->
                mediaItems = items
                shuffledItems = buildPlaylist(items, _config.value)

                if (shuffledItems.isEmpty()) {
                    timerJob?.cancel()
                    _state.value = previous.copy(
                        totalItems = 0,
                        allItems = emptyList(),
                        playlistItems = emptyList(),
                        currentItem = null,
                        currentIndex = 0,
                        nextItem = null,
                        error = "No hay fotos o videos. Configura carpetas en Ajustes."
                    )
                    return@withContext
                }

                val previousId = previous.currentItem?.id
                val newIndex = when {
                    previousId != null -> {
                        val idx = shuffledItems.indexOfFirst { it.id == previousId }
                        if (idx >= 0) idx else previous.currentIndex.coerceIn(0, shuffledItems.lastIndex)
                    }
                    else -> previous.currentIndex.coerceIn(0, shuffledItems.lastIndex)
                }

                _state.value = previous.copy(
                    totalItems = items.size,
                    allItems = items,
                    playlistItems = shuffledItems,
                    currentIndex = newIndex,
                    currentItem = shuffledItems[newIndex],
                    error = null,
                    isTransitioning = false
                )
                preloadNext(newIndex)
                if (_state.value.isPlaying) scheduleNext()
            }.onFailure { e ->
                _state.value = previous.copy(error = e.message ?: "Error al cargar medios")
            }
        }
    }

    private fun applyShuffleKeepingCurrent(config: SlideshowConfig) {
        val current = _state.value.currentItem ?: return
        shuffledItems = buildPlaylist(mediaItems, config)
        val newIndex = shuffledItems.indexOfFirst { it.id == current.id }.let {
            if (it >= 0) it else 0
        }
        _state.value = _state.value.copy(
            playlistItems = shuffledItems,
            currentIndex = newIndex,
            currentItem = shuffledItems.getOrNull(newIndex)
        )
        preloadNext(newIndex)
    }

    fun start() {
        if (_state.value.isPlaying) return
        _state.value = _state.value.copy(isPlaying = true)
        scheduleNext()
    }

    fun beginSession() {
        timerJob?.cancel()
        transitionJob?.cancel()
        if (mediaItems.isEmpty()) {
            _state.value = _state.value.copy(
                isPlaying = false,
                error = "No hay fotos o videos. Configura carpetas en Ajustes."
            )
            return
        }
        val config = _config.value
        shuffledItems = buildPlaylist(mediaItems, config)
        val startIndex = if (config.photoShuffle || config.videoShuffle) {
            shuffledItems.indices.random()
        } else {
            0
        }
        val item = shuffledItems[startIndex]
        _state.value = _state.value.copy(
            playlistItems = shuffledItems,
            currentIndex = startIndex,
            currentItem = item,
            isPlaying = true,
            isTransitioning = false
        )
        preloadNext(startIndex)
        scheduleNext()
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        timerJob?.cancel()
    }

    fun releaseCurrentForDelete() {
        timerJob?.cancel()
        transitionJob?.cancel()
        _state.value = _state.value.copy(
            currentItem = null,
            nextItem = null,
            isTransitioning = false
        )
    }

    fun stop() {
        pause()
        transitionJob?.cancel()
        _state.value = SlideshowState()
        mediaItems = emptyList()
        shuffledItems = emptyList()
    }

    fun next() {
        val nextIdx = nextIndex() ?: return
        navigateTo(nextIdx)
    }

    fun previous() {
        if (shuffledItems.isEmpty()) return
        val currentIndex = _state.value.currentIndex
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else shuffledItems.lastIndex
        navigateTo(prevIndex)
    }

    fun jumpTo(index: Int) {
        if (shuffledItems.isEmpty()) return
        navigateTo(index.coerceIn(0, shuffledItems.lastIndex))
    }

    fun selectPreviewIndex(index: Int) = jumpTo(index)

    fun removeItem(itemId: String) {
        val removeIndex = _state.value.currentIndex
        mediaItems = mediaItems.filter { it.id != itemId }
        shuffledItems = buildPlaylist(mediaItems, _config.value)

        if (shuffledItems.isEmpty()) {
            timerJob?.cancel()
            _state.value = _state.value.copy(
                currentItem = null,
                currentIndex = 0,
                totalItems = 0,
                allItems = emptyList(),
                playlistItems = emptyList(),
                nextItem = null,
                error = "No hay fotos o videos. Configura carpetas en Ajustes."
            )
            return
        }

        val newIndex = removeIndex.coerceIn(0, shuffledItems.lastIndex)
        _state.value = _state.value.copy(
            totalItems = mediaItems.size,
            allItems = mediaItems,
            playlistItems = shuffledItems,
            currentIndex = newIndex,
            currentItem = shuffledItems[newIndex],
            isTransitioning = false
        )
        preloadNext(newIndex)
        if (_state.value.isPlaying) scheduleNext()
    }

    fun onVideoCompleted() {
        if (!_state.value.isPlaying) return
        if (!_config.value.videoPlayFull) return
        skipToNextOrPause()
    }

    /** Error de reproducción o carga: saltar al siguiente medio sin detener el slideshow. */
    fun onPlaybackError() {
        if (!_state.value.isPlaying) return
        skipToNextOrPause()
    }

    private fun skipToNextOrPause() {
        val nextIdx = nextIndex()
        if (nextIdx == null) pause() else navigateTo(nextIdx)
    }

    private fun scheduleNext() {
        timerJob?.cancel()
        val currentItem = _state.value.currentItem ?: return

        if (currentItem.type == MediaType.VIDEO && _config.value.videoPlayFull) {
            return
        }

        val intervalMs = _config.value.intervalSeconds * 1000L
        timerJob = scope.launch {
            delay(intervalMs)
            if (_state.value.isPlaying) {
                skipToNextOrPause()
            }
        }
    }

    private fun navigateTo(index: Int) {
        timerJob?.cancel()
        transitionJob?.cancel()
        if (shuffledItems.isEmpty()) return

        val safeIndex = index.coerceIn(0, shuffledItems.lastIndex)
        val item = shuffledItems[safeIndex]
        val nextIdx = (safeIndex + 1) % shuffledItems.size

        _state.value = _state.value.copy(
            currentIndex = safeIndex,
            currentItem = item,
            nextItem = shuffledItems.getOrNull(nextIdx),
            isTransitioning = true
        )

        val itemId = item.id
        transitionJob = scope.launch {
            val ms = transitionMillis(_config.value.transition, _config.value.transitionDurationMs)
            delay(ms.coerceAtLeast(300).toLong())
            if (_state.value.currentItem?.id == itemId) {
                _state.value = _state.value.copy(isTransitioning = false)
            }
        }

        if (_state.value.isPlaying) {
            scheduleNext()
        }
    }

    private fun nextIndex(): Int? {
        if (shuffledItems.isEmpty()) return null
        val current = _state.value.currentIndex
        return when {
            current < shuffledItems.lastIndex -> current + 1
            _config.value.loop -> 0
            else -> null
        }
    }

    private fun preloadNext(currentIndex: Int) {
        if (shuffledItems.isEmpty()) return
        val nextIndex = (currentIndex + 1) % shuffledItems.size
        _state.value = _state.value.copy(
            nextItem = shuffledItems.getOrNull(nextIndex)
        )
    }

    private fun scheduleKeyFor(config: SlideshowConfig): String =
        "${config.intervalSeconds}|${config.videoPlayFull}|${config.loop}"
}
