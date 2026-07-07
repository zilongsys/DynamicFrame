package com.dynamicframe.ui.coil

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis

/** Request nítido alineado entre precarga (data) y UI (Coil). Mismas claves = caché compartida. */
fun buildSharpImageRequest(
    context: Context,
    uri: String,
    width: Int? = null,
    height: Int? = null,
    crossfadeMillis: Int = 0,
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(uri)
        .crossfade(crossfadeMillis)
        .allowHardware(false)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .videoFrameMillis(0)
    if (width != null && height != null) {
        builder.size(width, height)
    }
    return builder.build()
}
