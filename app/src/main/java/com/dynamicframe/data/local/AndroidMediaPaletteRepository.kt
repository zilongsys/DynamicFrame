package com.dynamicframe.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import com.dynamicframe.domain.model.MediaDynamicPalette
import com.dynamicframe.domain.repository.MediaPaletteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AndroidMediaPaletteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaPaletteRepository {

    private val cache = object : LinkedHashMap<String, MediaDynamicPalette>(MAX_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MediaDynamicPalette>): Boolean =
            size > MAX_CACHE
    }

    private val extractMutex = Mutex()

    override fun getCached(imageUri: String): MediaDynamicPalette? = synchronized(cache) {
        cache[imageUri]
    }

    override suspend fun preload(imageUri: String) {
        if (getCached(imageUri) != null) return
        extractFromImageUri(imageUri)
    }

    override suspend fun extractFromImageUri(imageUri: String): Result<MediaDynamicPalette> {
        getCached(imageUri)?.let { return Result.success(it) }
        return withContext(Dispatchers.IO) {
            extractMutex.withLock {
                getCached(imageUri)?.let { return@withContext Result.success(it) }
                runCatching {
                    val palette = decodePalette(imageUri)
                    synchronized(cache) { cache[imageUri] = palette }
                    palette
                }
            }
        }
    }

    private fun decodePalette(imageUri: String): MediaDynamicPalette {
        val uri = Uri.parse(imageUri)
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply { inSampleSize = SAMPLE_SIZE },
            )
        } ?: error("No se pudo decodificar la imagen para la paleta")

        try {
            val generated = Palette.from(bitmap).clearFilters().generate()
            val fallback = averageColor(bitmap)
            return MediaDynamicPalette(
                primary = generated.getDominantColor(fallback),
                secondary = generated.getVibrantColor(
                    generated.getMutedColor(fallback),
                ),
                tertiary = generated.getDarkMutedColor(
                    generated.getDarkVibrantColor(fallback),
                ),
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun averageColor(bitmap: android.graphics.Bitmap): Int {
        var r = 0L
        var g = 0L
        var b = 0L
        val stepX = (bitmap.width / 8).coerceAtLeast(1)
        val stepY = (bitmap.height / 8).coerceAtLeast(1)
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
                count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0) return 0xFF1A1A2E.toInt()
        return android.graphics.Color.rgb(
            (r / count).toInt(),
            (g / count).toInt(),
            (b / count).toInt(),
        )
    }

    companion object {
        private const val SAMPLE_SIZE = 16
        private const val MAX_CACHE = 64
    }
}
