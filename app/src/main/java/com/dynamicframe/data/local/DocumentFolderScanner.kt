package com.dynamicframe.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.dynamicframe.domain.model.MediaContentFilter
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.MusicTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentFolderScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filePathScanner: FilePathFolderScanner
) {

    fun scanMediaFolders(
        folderUris: List<String>,
        filter: MediaContentFilter
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        folderUris.forEach { uriString ->
            if (uriString.startsWith("file:")) {
                items.addAll(filePathScanner.scanMediaFolder(uriString, filter))
                return@forEach
            }
            val treeUri = Uri.parse(uriString)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@forEach
            scanMediaRecursive(root, filter, items, root.name ?: "Carpeta")
        }
        return items.distinctBy { it.uri }
            .sortedByDescending { it.dateAdded }
    }

    fun scanMusicFolder(folderUri: String): List<MusicTrack> {
        if (folderUri.startsWith("file:")) {
            return filePathScanner.scanMusicFolder(folderUri)
        }
        val treeUri = Uri.parse(folderUri)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val tracks = mutableListOf<MusicTrack>()
        scanMusicRecursive(root, tracks)
        return tracks.distinctBy { it.uri }.sortedBy { it.title.lowercase() }
    }

    private fun scanMediaRecursive(
        file: DocumentFile,
        filter: MediaContentFilter,
        out: MutableList<MediaItem>,
        folderName: String
    ) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                scanMediaRecursive(child, filter, out, folderName)
            }
            return
        }
        val mime = file.type ?: return
        val type = when {
            mime.startsWith("image/") -> MediaType.IMAGE
            mime.startsWith("video/") -> MediaType.VIDEO
            else -> return
        }
        if (!filter.allows(type)) return

        val uri = file.uri.toString()
        val name = file.name ?: queryDisplayName(file.uri) ?: "archivo"
        out.add(
            MediaItem(
                id = "folder_$uri",
                uri = uri,
                type = type,
                source = MediaSource.LOCAL,
                name = name,
                dateAdded = file.lastModified() / 1000L,
                albumId = folderName,
                albumName = folderName
            )
        )
    }

    private fun scanMusicRecursive(file: DocumentFile, out: MutableList<MusicTrack>) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { scanMusicRecursive(it, out) }
            return
        }
        val mime = file.type ?: return
        if (!mime.startsWith("audio/")) return

        val uri = file.uri.toString()
        val name = file.name ?: queryDisplayName(file.uri) ?: "Sin título"
        val title = name.substringBeforeLast('.')
        out.add(
            MusicTrack(
                id = "folder_music_$uri",
                uri = uri,
                title = title,
                artist = "Carpeta local",
                duration = 0L
            )
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        return null
    }
}
