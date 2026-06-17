package com.dynamicframe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/** Único punto de Coil para imágenes en UI (presentation no importa Coil directamente). */
@Composable
fun AppAsyncImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
    crossfadeMillis: Int = 450,
    onSuccess: (() -> Unit)? = null,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val requestBuilder = ImageRequest.Builder(context)
        .data(uri)
        .crossfade(crossfadeMillis)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)

    if (decodeWidth != null && decodeHeight != null) {
        requestBuilder.size(decodeWidth, decodeHeight)
    }

    AsyncImage(
        model = requestBuilder.build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = { onSuccess?.invoke() },
        onError = { onError?.invoke() }
    )
}
