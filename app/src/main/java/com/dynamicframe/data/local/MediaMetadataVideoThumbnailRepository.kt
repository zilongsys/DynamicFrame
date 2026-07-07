package com.dynamicframe.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.dynamicframe.domain.repository.VideoThumbnailRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaMetadataVideoThumbnailRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoThumbnailRepository {

    override suspend fun extractBlurFrameUri(mediaUri: String): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(mediaUri))
                    val bitmap = retriever.getFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: return@runCatching null
                    cacheBitmap(mediaUri, bitmap)
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

    private fun cacheBitmap(mediaUri: String, bitmap: Bitmap): String {
        val cacheFile = File(
            context.cacheDir,
            "paradise_video_blur_${mediaUri.hashCode()}.jpg"
        )
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return cacheFile.toURI().toString()
    }
}
