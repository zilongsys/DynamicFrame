package com.dynamicframe.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/** Navegación por carpetas locales sin selector SAF (TV box sin gestor de archivos). */
object LocalStorageBrowser {

    data class BrowserEntry(
        val name: String,
        val path: File
    )

    fun defaultRoots(context: Context): List<BrowserEntry> {
        val roots = linkedMapOf<String, BrowserEntry>()

        context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.let { storageRoot ->
            if (storageRoot.exists() && storageRoot.canRead()) {
                roots[storageRoot.absolutePath] = BrowserEntry("Almacenamiento interno", storageRoot)
            }
        }

        Environment.getExternalStorageDirectory()?.takeIf { it.exists() && it.canRead() }?.let { primary ->
            roots[primary.absolutePath] = BrowserEntry("Almacenamiento principal", primary)
        }

        listOf(
            Environment.DIRECTORY_PICTURES to "Imágenes",
            Environment.DIRECTORY_DCIM to "Cámara / DCIM",
            Environment.DIRECTORY_DOWNLOADS to "Descargas",
            Environment.DIRECTORY_MOVIES to "Vídeos",
            Environment.DIRECTORY_MUSIC to "Música"
        ).forEach { (type, label) ->
            Environment.getExternalStoragePublicDirectory(type)
                ?.takeIf { it.exists() && it.canRead() }
                ?.let { dir -> roots[dir.absolutePath] = BrowserEntry(label, dir) }
        }

        File("/storage").takeIf { it.isDirectory }?.listFiles()?.forEach { volume ->
            if (!volume.isDirectory || !volume.canRead()) return@forEach
            if (volume.name in setOf("emulated", "self")) return@forEach
            val label = if (volume.name.matches(Regex("[0-9A-F]{4}-[0-9A-F]{4}"))) {
                "USB / SD (${volume.name})"
            } else {
                volume.name
            }
            roots[volume.absolutePath] = BrowserEntry(label, volume)
        }

        return roots.values.toList()
    }

    fun listSubfolders(dir: File): List<BrowserEntry> =
        dir.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.canRead() && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?.map { BrowserEntry(it.name, it) }
            ?.toList()
            .orEmpty()

    fun toFolderUri(file: File): String = Uri.fromFile(file).toString()

    fun folderDisplayName(uriString: String): String {
        if (uriString.startsWith("file:")) {
            return Uri.parse(uriString).lastPathSegment ?: filePathFromUri(uriString)?.name ?: "Carpeta"
        }
        val uri = Uri.parse(uriString)
        return uri.lastPathSegment?.replace(':', ' ') ?: "Carpeta"
    }

    fun filePathFromUri(uriString: String): File? {
        if (!uriString.startsWith("file:")) return null
        return Uri.parse(uriString).path?.let { File(it) }
    }
}
