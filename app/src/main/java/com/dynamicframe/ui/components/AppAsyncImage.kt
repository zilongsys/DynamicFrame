package com.dynamicframe.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dynamicframe.ui.coil.buildSharpImageRequest

@Composable
fun rememberAppImageRequest(
    uri: String,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
    crossfadeMillis: Int = 0,
): ImageRequest {
    val context = LocalContext.current
    return remember(uri, decodeWidth, decodeHeight, crossfadeMillis) {
        buildSharpImageRequest(context, uri, decodeWidth, decodeHeight, crossfadeMillis)
    }
}

@Composable
fun rememberAppImagePainter(
    uri: String,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
    crossfadeMillis: Int = 0,
): AsyncImagePainter {
    val request = rememberAppImageRequest(uri, decodeWidth, decodeHeight, crossfadeMillis)
    return rememberAsyncImagePainter(model = request)
}

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
    val request = rememberAppImageRequest(uri, decodeWidth, decodeHeight, crossfadeMillis)
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = { onSuccess?.invoke() },
        onError = { onError?.invoke() }
    )
}

/**
 * Pinta la foto nítida solo cuando Coil confirma [AsyncImagePainter.State.Success].
 * Evita placeholders blancos y fondos sin imagen.
 */
@Composable
fun AppSharpImageWhenReady(
    uri: String,
    decodeWidth: Int,
    decodeHeight: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onError: () -> Unit = {},
    onSuccess: () -> Unit = {},
    content: @Composable (AsyncImagePainter) -> Unit,
) {
    val painter = rememberAppImagePainter(
        uri = uri,
        decodeWidth = decodeWidth,
        decodeHeight = decodeHeight,
        crossfadeMillis = 0,
    )
    LaunchedEffect(painter.state, uri) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> onSuccess()
            is AsyncImagePainter.State.Error -> onError()
            else -> Unit
        }
    }
    if (painter.state is AsyncImagePainter.State.Success) {
        Box(modifier) {
            content(painter)
        }
    }
}

@Composable
fun AppImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    decodeWidth: Int? = null,
    decodeHeight: Int? = null,
) {
    AppSharpImageWhenReady(
        uri = uri,
        decodeWidth = decodeWidth ?: 960,
        decodeHeight = decodeHeight ?: 540,
        modifier = modifier,
        contentScale = contentScale,
    ) { painter ->
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
