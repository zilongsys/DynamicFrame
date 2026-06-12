package com.dynamicframe.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dynamicframe.domain.model.MediaContentFilter
import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderScanner: DocumentFolderScanner
) : MediaRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    // ── Albums ──────────────────────────────────────────────────────────────────

    override suspend fun getAlbums(source: MediaSource): Result<List<MediaAlbum>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val albums = mutableMapOf<String, MediaAlbum>()

                // Álbumes de imágenes
                queryImageAlbums().forEach { albums[it.id] = it }

                // Álbumes de videos
                queryVideoAlbums().forEach { album ->
                    val existing = albums[album.id]
                    if (existing != null) {
                        albums[album.id] = existing.copy(itemCount = existing.itemCount + album.itemCount)
                    } else {
                        albums[album.id] = album
                    }
                }

                albums.values.sortedBy { it.name }
            }
        }

    private fun queryImageAlbums(): List<MediaAlbum> {
        val albums = mutableMapOf<String, Pair<MediaAlbum, Int>>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol) ?: "Sin nombre"
                val imageId = cursor.getLong(idCol)
                val imageUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
                )

                if (!albums.containsKey(bucketId)) {
                    albums[bucketId] = Pair(
                        MediaAlbum(
                            id = bucketId,
                            name = bucketName,
                            source = MediaSource.LOCAL,
                            coverUri = imageUri,
                            itemCount = 1
                        ),
                        1
                    )
                } else {
                    val (album, count) = albums[bucketId]!!
                    albums[bucketId] = Pair(album.copy(itemCount = count + 1), count + 1)
                }
            }
        }

        return albums.values.map { (album, count) -> album.copy(itemCount = count) }
    }

    private fun queryVideoAlbums(): List<MediaAlbum> {
        val albums = mutableMapOf<String, Pair<MediaAlbum, Int>>()

        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media._ID
        )

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol) ?: "Sin nombre"
                val videoId = cursor.getLong(idCol)
                val videoUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId
                )

                if (!albums.containsKey(bucketId)) {
                    albums[bucketId] = Pair(
                        MediaAlbum(
                            id = bucketId,
                            name = bucketName,
                            source = MediaSource.LOCAL,
                            coverUri = videoUri,
                            itemCount = 1
                        ),
                        1
                    )
                } else {
                    val (album, count) = albums[bucketId]!!
                    albums[bucketId] = Pair(album.copy(itemCount = count + 1), count + 1)
                }
            }
        }

        return albums.values.map { (album, count) -> album.copy(itemCount = count) }
    }

    // ── Media Items ─────────────────────────────────────────────────────────────

    override suspend fun getMediaItems(
        albumId: String,
        source: MediaSource
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val images = queryImages(albumId)
            val videos = queryVideos(albumId)
            (images + videos).sortedByDescending { it.dateAdded }
        }
    }

    override suspend fun getAllMediaItems(
        sources: List<MediaSource>
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val images = queryImages(null)
            val videos = queryVideos(null)
            (images + videos).sortedByDescending { it.dateAdded }
        }
    }

    override suspend fun getMediaFromFolders(
        folderUris: List<String>,
        filter: MediaContentFilter
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            folderScanner.scanMediaFolders(folderUris, filter)
        }
    }

    private fun queryImages(bucketId: String?): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val selection = bucketId?.let { "${MediaStore.Images.Media.BUCKET_ID} = ?" }
        val selectionArgs = bucketId?.let { arrayOf(it) }

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                items.add(
                    MediaItem(
                        id = "img_$id",
                        uri = uri,
                        type = MediaType.IMAGE,
                        source = MediaSource.LOCAL,
                        name = cursor.getString(nameCol) ?: "",
                        dateAdded = cursor.getLong(dateCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        albumId = cursor.getString(bucketIdCol) ?: "",
                        albumName = cursor.getString(bucketNameCol) ?: ""
                    )
                )
            }
        }

        return items
    }

    private fun queryVideos(bucketId: String?): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val selection = bucketId?.let { "${MediaStore.Video.Media.BUCKET_ID} = ?" }
        val selectionArgs = bucketId?.let { arrayOf(it) }

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                items.add(
                    MediaItem(
                        id = "vid_$id",
                        uri = uri,
                        type = MediaType.VIDEO,
                        source = MediaSource.LOCAL,
                        name = cursor.getString(nameCol) ?: "",
                        dateAdded = cursor.getLong(dateCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        duration = cursor.getLong(durationCol),
                        albumId = cursor.getString(bucketIdCol) ?: "",
                        albumName = cursor.getString(bucketNameCol) ?: ""
                    )
                )
            }
        }

        return items
    }

    override fun observeLocalMedia(): Flow<List<MediaItem>> = flow {
        emit(getAllMediaItems(listOf(MediaSource.LOCAL)).getOrDefault(emptyList()))
    }.flowOn(Dispatchers.IO)
}
