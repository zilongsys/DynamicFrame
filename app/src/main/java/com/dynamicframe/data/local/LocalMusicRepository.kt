package com.dynamicframe.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.dynamicframe.domain.model.MediaAlbum
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MusicTrack
import com.dynamicframe.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderScanner: DocumentFolderScanner
) : MusicRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun getLocalTracks(): Result<List<MusicTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tracks = mutableListOf<MusicTrack>()

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.IS_MUSIC
                )

                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val trackUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

                        tracks.add(
                            MusicTrack(
                                id = "music_$id",
                                uri = trackUri.toString(),
                                title = cursor.getString(titleCol) ?: "Sin título",
                                artist = cursor.getString(artistCol) ?: "Desconocido",
                                album = cursor.getString(albumCol) ?: "",
                                duration = cursor.getLong(durationCol),
                                albumArtUri = albumArtUri.toString()
                            )
                        )
                    }
                }

                tracks
            }
        }

    override suspend fun getTracksFromFolder(folderUri: String): Result<List<MusicTrack>> =
        getTracksFromFolders(listOf(folderUri))

    override suspend fun getTracksFromFolders(folderUris: List<String>): Result<List<MusicTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tracks = linkedMapOf<String, MusicTrack>()
                folderUris.distinct().forEach { uri ->
                    folderScanner.scanMusicFolder(uri).forEach { track ->
                        tracks[track.id] = track
                    }
                }
                tracks.values.sortedBy { it.title.lowercase() }
            }
        }

    override suspend fun getLocalAlbums(): Result<List<MediaAlbum>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val albums = mutableMapOf<Long, MediaAlbum>()

                val projection = arrayOf(
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS
                )

                contentResolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    projection,
                    null, null,
                    "${MediaStore.Audio.Albums.ALBUM} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                    val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val albumArtUri = Uri.parse("content://media/external/audio/albumart/$id")

                        albums[id] = MediaAlbum(
                            id = id.toString(),
                            name = cursor.getString(albumCol) ?: "Sin nombre",
                            source = MediaSource.LOCAL,
                            coverUri = albumArtUri.toString(),
                            itemCount = cursor.getInt(countCol)
                        )
                    }
                }

                albums.values.toList()
            }
        }

    override fun observeLocalTracks(): Flow<List<MusicTrack>> = flow {
        emit(getLocalTracks().getOrDefault(emptyList()))
    }.flowOn(Dispatchers.IO)
}
