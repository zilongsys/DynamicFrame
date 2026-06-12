package com.dynamicframe.presentation.slideshow

import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.SlideshowState
import com.dynamicframe.domain.usecase.GetSlideshowItemsUseCase
import com.dynamicframe.domain.usecase.ObserveSlideshowConfigUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SlideshowEngine @Inject constructor(
    private val getSlideshowItems: GetSlideshowItemsUseCase,
    private val observeConfig: ObserveSlideshowConfigUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SlideshowState())
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    private val _config = MutableStateFlow(SlideshowConfig())
    val config: StateFlow<SlideshowConfig> = _config.asStateFlow()

    private var mediaItems: List<MediaItem> = emptyList()
    private var shuffledItems: List<MediaItem> = emptyList()
    private var timerJob: Job? = null
    private var isInitialized = false
    private var lastMediaKey: String = ""
    private var lastIntervalSeconds: Int = -1

    init {
        scope.launch {
            observeConfig().collect { config ->
                val previousConfig = _config.value
                _config.value = config

                val mediaKey = "${config.mediaFolderUris}|${config.mediaContentFilter}|${config.selectedAlbumIds}"
                if (mediaKey != lastMediaKey) {
                    lastMediaKey = mediaKey
                    if (isInitialized) loadMedia()
                } else if (previousConfig.shuffle != config.shuffle && mediaItems.isNotEmpty()) {
                    applyShuffleKeepingCurrent(config.shuffle)
                }

                if (config.intervalSeconds != lastIntervalSeconds) {
                    lastIntervalSeconds = config.intervalSeconds
                    if (_state.value.isPlaying) scheduleNext()
                }
            }
        }
    }

    suspend fun initialize() {
        if (isInitialized) return
        loadMedia()
        isInitialized = true
        lastIntervalSeconds = _config.value.intervalSeconds
    }

    suspend fun loadMedia() {
        _state.value = _state.value.copy(error = null)
        val result = getSlideshowItems()
        result.onSuccess { items ->
            mediaItems = items
            shuffledItems = if (_config.value.shuffle) items.shuffled() else items
            _state.value = _state.value.copy(
                totalItems = items.size,
                allItems = items,
                playlistItems = shuffledItems,
                currentItem = shuffledItems.firstOrNull(),
                currentIndex = 0,
                error = if (items.isEmpty()) "No hay fotos o videos. Configura carpetas en Ajustes." else null
            )
            preloadNext(0)
        }.onFailure { e ->
            _state.value = _state.value.copy(error = e.message ?: "Error al cargar medios")
        }
    }

    private fun applyShuffleKeepingCurrent(shuffle: Boolean) {
        val current = _state.value.currentItem ?: return
        shuffledItems = if (shuffle) mediaItems.shuffled() else mediaItems
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

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        timerJob?.cancel()
    }

    fun stop() {
        pause()
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

    fun onVideoCompleted() {
        if (!_state.value.isPlaying) return
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
                val nextIdx = nextIndex()
                if (nextIdx == null) pause() else navigateTo(nextIdx)
            }
        }
    }

    private fun navigateTo(index: Int) {
        timerJob?.cancel()
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

        scope.launch {
            val ms = transitionMillis(_config.value.transition, _config.value.transitionDurationMs)
            delay(ms.coerceAtLeast(300).toLong())
            _state.value = _state.value.copy(isTransitioning = false)
        }

        if (_state.value.isPlaying) {
            scheduleNext()
        }
    }

    /** null = fin del slideshow cuando loop está desactivado */
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
}
