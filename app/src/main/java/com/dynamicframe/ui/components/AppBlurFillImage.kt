package com.dynamicframe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dynamicframe.ui.coil.BlurTransformation

/**
 * Imagen de relleno Paradise: blur Coil + saturación ligeramente elevada.
 * Único punto de [BlurTransformation] en la UI.
 */
@Composable
fun AppBlurFillImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    blurRadius: Float = 25f,
    blurSampling: Float = 4f,
    alpha: Float = 1f,
    saturation: Float = 1.3f,
    decodeWidth: Int = 960,
    decodeHeight: Int = 540,
    crossfade: Boolean = true,
) {
    val context = LocalContext.current
    val colorMatrix = remember(saturation) {
        ColorMatrix().apply { setToSaturation(saturation) }
    }
    val model = remember(uri, blurRadius, blurSampling, decodeWidth, decodeHeight, crossfade) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(decodeWidth, decodeHeight)
            .transformations(BlurTransformation(context, blurRadius, blurSampling))
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(false)
            .crossfade(crossfade)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = ColorFilter.colorMatrix(colorMatrix),
        modifier = modifier
            .alpha(alpha)
    )
}
