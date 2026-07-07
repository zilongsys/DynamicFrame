@file:Suppress("DEPRECATION")

package com.dynamicframe.ui.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.RequiresApi
import coil.size.Size
import coil.transform.Transformation

/**
 * Gaussian blur para Coil 2.x (el artefacto `coil-transformations` ya no existe).
 * Basado en la implementación oficial de Coil 1.x, adaptada a la API de [Transformation].
 */
@RequiresApi(18)
class BlurTransformation(
    private val context: Context,
    private val radius: Float = DEFAULT_RADIUS,
    private val sampling: Float = DEFAULT_SAMPLING
) : Transformation {

    init {
        require(radius in 0.0..25.0) { "radius must be in [0, 25]." }
        require(sampling > 0) { "sampling must be > 0." }
    }

    override val cacheKey: String = "${BlurTransformation::class.java.name}-$radius-$sampling"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val config = input.config ?: Bitmap.Config.ARGB_8888
        val scaledWidth = (input.width / sampling).toInt().coerceAtLeast(1)
        val scaledHeight = (input.height / sampling).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(scaledWidth, scaledHeight, config)
        Canvas(output).apply {
            scale(1 / sampling, 1 / sampling)
            drawBitmap(input, 0f, 0f, paint)
        }

        var script: RenderScript? = null
        var tmpIn: Allocation? = null
        var tmpOut: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            script = RenderScript.create(context)
            tmpIn = Allocation.createFromBitmap(
                script,
                output,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            tmpOut = Allocation.createTyped(script, tmpIn.type)
            blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
            blur.setRadius(radius.coerceIn(0f, 25f))
            blur.setInput(tmpIn)
            blur.forEach(tmpOut)
            tmpOut.copyTo(output)
        } finally {
            script?.destroy()
            tmpIn?.destroy()
            tmpOut?.destroy()
            blur?.destroy()
        }
        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is BlurTransformation &&
            radius == other.radius &&
            sampling == other.sampling
    }

    override fun hashCode(): Int {
        var result = radius.hashCode()
        result = 31 * result + sampling.hashCode()
        return result
    }

    private companion object {
        private const val DEFAULT_RADIUS = 10f
        private const val DEFAULT_SAMPLING = 1f
    }
}
