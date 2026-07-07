package com.dynamicframe.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

private const val NOISE_TILE_SIZE = 96

private fun createTiledNoiseBitmap(seed: Int = 0x6E01_5E): Bitmap {
    val size = NOISE_TILE_SIZE
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = Random(seed)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val lum = random.nextInt(256)
        val a = if (random.nextFloat() < 0.7f) random.nextInt(55) + 25 else 0
        pixels[i] = (a shl 24) or (lum shl 16) or (lum shl 8) or lum
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
}

/**
 * Textura de grano fino en mosaico (film grain) para fondos blur opacos.
 */
@Composable
fun FilmGrainOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.2f,
) {
    val paint = remember {
        val noise = createTiledNoiseBitmap()
        Paint().apply {
            isFilterBitmap = false
            isDither = false
            shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}
