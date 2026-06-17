package com.dynamicframe.data.local

import android.net.Uri
import com.dynamicframe.domain.model.MediaContentFilter
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicTrack
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePathFolderScanner @Inject constructor() {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp")
    private val audioExtensions = setOf("mp3", "flac", "wav", "aac", "ogg", "m4a", "opus", "wma")

    fun scanMediaFolder(folderUri: String, filter: MediaContentFilter): List<MediaItem> {
        val root = LocalStorageBrowser.filePathFromUri(folderUri) ?: return emptyList()
        if (!root.isDirectory || !root.canRead()) return emptyList()

        val folderName = root.name
        val items = mutableListOf<MediaItem>()

        root.walkTopDown()
            .onFail { _, _ -> }
            .maxDepth(25)
            .filter { it.isFile && it.canRead() }
            .forEach { file ->
                val type = mediaTypeFor(file) ?: return@forEach
                if (!filter.allows(type)) return@forEach
                items.add(
                    MediaItem(
                        id = "path_${file.absolutePath}",
                        uri = Uri.fromFile(file).toString(),
                        type = type,
                        source = MediaSource.LOCAL,
                        name = file.name,
                        dateAdded = file.lastModified() / 1000L,
                        albumId = folderName,
                        albumName = folderName
                    )
                )
            }

        return items.distinctBy { it.uri }.sortedByDescending { it.dateAdded }
    }

    fun scanMusicFolder(folderUri: String): List<MusicTrack> {
        val root = LocalStorageBrowser.filePathFromUri(folderUri) ?: return emptyList()
        if (!root.isDirectory || !root.canRead()) return emptyList()

        val tracks = mutableListOf<MusicTrack>()
        root.walkTopDown()
            .onFail { _, _ -> }
            .maxDepth(25)
            .filter { it.isFile && it.canRead() }
            .forEach { file ->
                if (file.extension.lowercase() !in audioExtensions) return@forEach
                val title = file.nameWithoutExtension
                tracks.add(
                    MusicTrack(
                        id = "path_music_${file.absolutePath}",
                        uri = Uri.fromFile(file).toString(),
                        title = title,
                        artist = "Carpeta local",
                        duration = 0L
                    )
                )
            }

        return tracks.distinctBy { it.uri }.sortedBy { it.title.lowercase() }
    }

    private fun mediaTypeFor(file: File): MediaType? = when (file.extension.lowercase()) {
        in imageExtensions -> MediaType.IMAGE
        in videoExtensions -> MediaType.VIDEO
        else -> null
    }
}
