package com.dynamicframe.domain.slideshow

import android.os.Build
import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.usecase.GetMediaDynamicPaletteUseCase
import com.dynamicframe.domain.usecase.GetVideoBlurThumbnailUseCase
import com.dynamicframe.domain.usecase.PreloadSlideshowImagesUseCase
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ventana rolling: slide actual + [LOOKAHEAD_COUNT] siguientes.
 * «Listo» = paleta + imagen nítida + blur en caché (listo para mostrar).
 */
@Singleton
class DynamicBackdropPrefetcher @Inject constructor(
    private val getMediaDynamicPalette: GetMediaDynamicPaletteUseCase,
    private val getVideoBlurThumbnail: GetVideoBlurThumbnailUseCase,
    private val preloadSlideshowImages: PreloadSlideshowImagesUseCase,
    private val debug: AppDebugLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val itemLocks = ConcurrentHashMap<String, Mutex>()

    private val _palettes = MutableStateFlow<Map<String, MediaDynamicPalette>>(emptyMap())
    val palettes: StateFlow<Map<String, MediaDynamicPalette>> = _palettes.asStateFlow()

    private val readyIds = ConcurrentHashMap.newKeySet<String>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Unit>>()

    fun isReady(itemId: String): Boolean = itemId in readyIds

    fun clear() {
        readyIds.clear()
        _palettes.value = emptyMap()
        inFlight.values.forEach { runCatching { it.cancel() } }
        inFlight.clear()
    }

    fun windowItems(playlist: List<MediaItem>, currentIndex: Int): List<MediaItem> {
        if (playlist.isEmpty()) return emptyList()
        return (0..LOOKAHEAD_COUNT).map { offset ->
            playlist[(currentIndex + offset) % playlist.size]
        }.distinctBy { it.id }
    }

    fun scheduleWindow(playlist: List<MediaItem>, currentIndex: Int) {
        windowItems(playlist, currentIndex).forEach { scheduleItem(it) }
    }

    fun scheduleItem(item: MediaItem) {
        if (isReady(item.id)) return
        inFlight.computeIfAbsent(item.id) {
            scope.async {
                itemLocks.computeIfAbsent(item.id) { Mutex() }.withLock {
                    if (!isReady(item.id)) {
                        prefetchOneLocked(item)
                    }
                }
            }
        }
    }

    suspend fun awaitReady(item: MediaItem, timeoutMs: Long = AWAIT_TIMEOUT_MS) {
        if (isReady(item.id)) return
        scheduleItem(item)
        val deferred = inFlight[item.id]
        withTimeoutOrNull(timeoutMs) {
            runCatching { deferred?.await() }
        }
        if (!isReady(item.id)) {
            ensureFallback(item)
        }
    }

    /** Bloquea hasta que la 1.ª diapositiva (paleta + nítida + blur) esté lista. */
    suspend fun awaitFirstSlideReady(item: MediaItem) {
        awaitReady(item)
    }

    private suspend fun prefetchOneLocked(item: MediaItem) {
        val sourceUri = resolveSourceUri(item)
        val palette = getMediaDynamicPalette(sourceUri).getOrNull() ?: fallbackPalette()
        _palettes.update { it + (item.id to palette) }
        runCatching {
            preloadSlideshowImages.awaitSharp(
                listOf(sourceUri),
                DECODE_WIDTH,
                DECODE_HEIGHT,
            )
        }.onFailure { e ->
            debug.w("BackdropPrefetch", "sharp ${item.id.takeLast(12)}: ${e.message}")
        }
        if (needsBlurPreload(item)) {
            runCatching {
                preloadSlideshowImages.awaitBlur(
                    sourceUri,
                    DECODE_WIDTH,
                    DECODE_HEIGHT,
                    blurSampling = BLUR_SAMPLING,
                )
            }.onFailure { e ->
                debug.w("BackdropPrefetch", "blur ${item.id.takeLast(12)}: ${e.message}")
            }
        }
        readyIds.add(item.id)
        debug.d("BackdropPrefetch", "Listo ${item.id.takeLast(12)}")
    }

    private fun needsBlurPreload(item: MediaItem): Boolean = when (item.type) {
        MediaType.IMAGE -> true
        MediaType.VIDEO -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    private suspend fun resolveSourceUri(item: MediaItem): String = when (item.type) {
        MediaType.IMAGE -> item.uri
        MediaType.VIDEO -> getVideoBlurThumbnail(item.uri).getOrNull() ?: item.uri
    }

    private fun ensureFallback(item: MediaItem) {
        if (isReady(item.id)) return
        _palettes.update { it + (item.id to fallbackPalette()) }
        readyIds.add(item.id)
        debug.w("BackdropPrefetch", "Fallback ${item.id.takeLast(12)}")
    }

    private fun fallbackPalette() = MediaDynamicPalette(
        primary = 0xFF2A2840.toInt(),
        secondary = 0xFF4A6080.toInt(),
        tertiary = 0xFF1A1A28.toInt(),
    )

    companion object {
        const val LOOKAHEAD_COUNT = 5
        const val DECODE_WIDTH = 960
        const val DECODE_HEIGHT = 540
        const val BLUR_SAMPLING = 5f
        private const val AWAIT_TIMEOUT_MS = 8_000L
    }
}
