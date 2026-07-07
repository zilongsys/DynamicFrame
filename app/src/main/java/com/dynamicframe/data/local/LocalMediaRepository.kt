package com.dynamicframe.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.dynamicframe.domain.model.DeleteMediaFailure
import com.dynamicframe.domain.model.DeleteMediaResult
import com.dynamicframe.domain.model.MediaContentFilter
import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderScanner: DocumentFolderScanner,
    private val deleteConsentStore: PendingDeleteConsentStore,
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
                            coverUri = imageUri.toString(),
                            itemCount = 1
                        ),
                        1
                    )
                } else {
                    albums[bucketId]?.let { (album, count) ->
                        albums[bucketId] = Pair(album.copy(itemCount = count + 1), count + 1)
                    }
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
                            coverUri = videoUri.toString(),
                            itemCount = 1
                        ),
                        1
                    )
                } else {
                    albums[bucketId]?.let { (album, count) ->
                        albums[bucketId] = Pair(album.copy(itemCount = count + 1), count + 1)
                    }
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
                        uri = uri.toString(),
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
                        uri = uri.toString(),
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

    override suspend fun deleteMediaItem(item: MediaItem): DeleteMediaResult = withContext(Dispatchers.IO) {
        if (item.source != MediaSource.LOCAL) {
            return@withContext DeleteMediaResult.Failed(MediaDeleteFailureMapper.notLocalSource())
        }
        deleteItemInternal(item)
    }

    private sealed interface InternalDeleteOutcome {
        data object Deleted : InternalDeleteOutcome
        data class NeedsConsent(val intentSender: android.content.IntentSender) : InternalDeleteOutcome
        data class Failed(val failure: DeleteMediaFailure) : InternalDeleteOutcome
    }

    private fun deleteItemInternal(item: MediaItem): DeleteMediaResult {
        val parsed = Uri.parse(item.uri)
        return when (parsed.scheme) {
            "file" -> deleteFileUri(parsed, item.uri)
            "content" -> when (val outcome = deleteContentUri(parsed, item.uri)) {
                InternalDeleteOutcome.Deleted -> DeleteMediaResult.Deleted
                is InternalDeleteOutcome.NeedsConsent -> DeleteMediaResult.NeedsUserConsent(
                    deleteConsentStore.register(outcome.intentSender),
                )
                is InternalDeleteOutcome.Failed -> DeleteMediaResult.Failed(outcome.failure)
            }
            else -> DeleteMediaResult.Failed(
                MediaDeleteFailureMapper.unsupportedUri().copy(mediaUri = item.uri),
            )
        }
    }

    private fun deleteFileUri(uri: Uri, rawUri: String): DeleteMediaResult {
        val path = uri.path ?: return DeleteMediaResult.Failed(
            MediaDeleteFailureMapper.unsupportedUri().copy(mediaUri = rawUri),
        )
        val file = File(path)
        if (!file.exists()) return DeleteMediaResult.Deleted

        val parent = file.parentFile
        if (parent != null && !parent.canWrite()) {
            return DeleteMediaResult.Failed(MediaDeleteFailureMapper.readOnlyStorage(rawUri))
        }
        if (!file.canWrite()) {
            return DeleteMediaResult.Failed(MediaDeleteFailureMapper.fileNotWritable(rawUri))
        }
        return if (file.delete()) {
            DeleteMediaResult.Deleted
        } else {
            DeleteMediaResult.Failed(MediaDeleteFailureMapper.fileInUse(rawUri))
        }
    }

    private fun deleteContentUri(uri: Uri, rawUri: String): InternalDeleteOutcome {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val doc = DocumentFile.fromSingleUri(context, uri)
            when {
                doc == null || !doc.exists() -> return InternalDeleteOutcome.Deleted
                !doc.canWrite() -> return InternalDeleteOutcome.Failed(
                    MediaDeleteFailureMapper.safNoWriteAccess(rawUri),
                )
                doc.delete() -> return InternalDeleteOutcome.Deleted
                else -> return InternalDeleteOutcome.Failed(
                    MediaDeleteFailureMapper.fileInUse(rawUri),
                )
            }
        }

        try {
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) return InternalDeleteOutcome.Deleted
        } catch (e: android.app.RecoverableSecurityException) {
            return InternalDeleteOutcome.NeedsConsent(e.userAction.actionIntent.intentSender)
        } catch (_: SecurityException) {
            // Probar otras vías.
        }

        val doc = DocumentFile.fromSingleUri(context, uri)
        if (doc != null && doc.exists()) {
            when {
                !doc.canWrite() -> return InternalDeleteOutcome.Failed(
                    MediaDeleteFailureMapper.safNoWriteAccess(rawUri),
                )
                doc.delete() -> return InternalDeleteOutcome.Deleted
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uriExists(uri)) {
            return try {
                val pending = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                InternalDeleteOutcome.NeedsConsent(pending.intentSender)
            } catch (_: Exception) {
                InternalDeleteOutcome.Failed(MediaDeleteFailureMapper.systemDeleteBlocked(rawUri))
            }
        }

        return when {
            !uriExists(uri) -> InternalDeleteOutcome.Deleted
            isMediaStoreUri(uri) -> InternalDeleteOutcome.Failed(
                MediaDeleteFailureMapper.systemDeleteBlocked(rawUri),
            )
            else -> InternalDeleteOutcome.Failed(
                MediaDeleteFailureMapper.permissionDenied(rawUri),
            )
        }
    }

    private fun uriExists(uri: Uri): Boolean =
        runCatching {
            contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
        }.getOrDefault(false)

    private fun isMediaStoreUri(uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        return authority == MediaStore.AUTHORITY ||
            authority.endsWith(".media") ||
            authority.contains("media")
    }
}
