package com.dynamicframe.data.local

import android.content.Context
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dynamicframe.domain.repository.ImageCacheRepository
import javax.inject.Inject
import javax.inject.Singleton

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
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .size(width, height)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }
}
