package com.dynamicframe.data.local

import android.content.Context
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dynamicframe.domain.repository.ImageCacheRepository
import com.dynamicframe.ui.coil.BlurTransformation
import com.dynamicframe.ui.coil.buildSharpImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CoilImageCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageCacheRepository {

    override fun evict(uri: String) {
        runCatching {
            val loader = context.imageLoader
            loader.memoryCache?.remove(MemoryCache.Key(uri))
            loader.diskCache?.remove(uri)
        }
    }

    override fun preload(uri: String, width: Int, height: Int) {
        runCatching {
            context.imageLoader.enqueue(buildImageRequest(uri, width, height))
        }
    }

    override suspend fun preloadAndAwait(uri: String, width: Int, height: Int) {
        withContext(Dispatchers.IO) {
            runCatching {
                context.imageLoader.execute(buildImageRequest(uri, width, height))
            }
        }
    }

    override suspend fun preloadBlurAndAwait(
        uri: String,
        width: Int,
        height: Int,
        blurRadius: Float,
        blurSampling: Float,
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(width, height)
                    .transformations(BlurTransformation(context, blurRadius, blurSampling))
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
                context.imageLoader.execute(request)
            }
        }
    }

    private fun buildImageRequest(uri: String, width: Int, height: Int): ImageRequest =
        buildSharpImageRequest(context, uri, width, height)
}
