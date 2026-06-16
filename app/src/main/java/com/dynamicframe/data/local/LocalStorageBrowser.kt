package com.dynamicframe.data.local

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File

/** Navegación por carpetas locales sin selector SAF (TV box sin gestor de archivos). */
object LocalStorageBrowser {

    data class BrowserEntry(
        val name: String,
        val path: File,
        val readable: Boolean = true
    )

    fun defaultRoots(context: Context): List<BrowserEntry> {
        val roots = linkedMapOf<String, BrowserEntry>()

        fun addRoot(label: String, file: File?) {
            if (file == null) return
            if (!file.exists()) return
            roots[file.absolutePath] = BrowserEntry(label, file, file.canRead())
        }

        // Volúmenes del sistema (interno, USB, SD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = context.getSystemService(StorageManager::class.java)
            sm?.storageVolumes?.forEach { volume ->
                val dir = volumeDirectory(volume)
                val label = volumeLabel(volume, dir)
                addRoot(label, dir)
            }
        }

        addRoot("Almacenamiento principal", Environment.getExternalStorageDirectory())

        listOf(
            Environment.DIRECTORY_PICTURES to "Imágenes",
            Environment.DIRECTORY_DCIM to "Cámara / DCIM",
            Environment.DIRECTORY_DOWNLOADS to "Descargas",
            Environment.DIRECTORY_MOVIES to "Vídeos",
            Environment.DIRECTORY_MUSIC to "Música"
        ).forEach { (type, label) ->
            addRoot(label, Environment.getExternalStoragePublicDirectory(type))
        }

        // Puntos de montaje habituales en TV box
        listOf(
            "/storage" to "Todos los discos (/storage)",
            "/mnt/media_rw" to "USB / SD (/mnt/media_rw)",
            "/mnt/usb" to "USB (/mnt/usb)",
            "/mnt/usbhost" to "USB host",
            "/mnt/usb_storage" to "USB almacenamiento",
            "/storage/usbotg" to "USB OTG",
            "/storage/udisk" to "USB (udisk)",
            "/storage/usb" to "USB",
            "/mnt/sdcard" to "SD card"
        ).forEach { (path, label) ->
            addRoot(label, File(path))
        }

        // Hijos directos de /storage (USB con nombre UUID)
        File("/storage").takeIf { it.isDirectory }?.listFiles()?.forEach { volume ->
            if (!volume.isDirectory) return@forEach
            if (volume.name == "self") return@forEach
            val label = when (volume.name) {
                "emulated" -> "Almacenamiento interno (emulated)"
                else -> if (volume.name.matches(Regex("[0-9A-F]{4}-[0-9A-F]{4}", RegexOption.IGNORE_CASE))) {
                    "USB / SD (${volume.name})"
                } else {
                    "Disco: ${volume.name}"
                }
            }
            addRoot(label, volume)
        }

        // Hijos de /mnt/media_rw (muy común en Android TV)
        File("/mnt/media_rw").takeIf { it.isDirectory }?.listFiles()?.forEach { volume ->
            if (!volume.isDirectory) return@forEach
            addRoot("USB: ${volume.name}", volume)
        }

        return roots.values.toList()
    }

    fun listSubfolders(dir: File): List<BrowserEntry> {
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return emptyList()
        return children
            .asSequence()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.canRead() }, { it.name.lowercase() }))
            .map { child ->
                BrowserEntry(
                    name = if (child.canRead()) child.name else "${child.name} (sin acceso)",
                    path = child,
                    readable = child.canRead()
                )
            }
            .toList()
    }

    fun toFolderUri(file: File): String = Uri.fromFile(file).toString()

    fun folderDisplayName(uriString: String): String {
        if (uriString.startsWith("file:")) {
            return filePathFromUri(uriString)?.absolutePath?.substringAfterLast('/')
                ?: "Carpeta"
        }
        val uri = Uri.parse(uriString)
        return uri.lastPathSegment?.replace(':', ' ') ?: "Carpeta"
    }

    fun filePathFromUri(uriString: String): File? {
        if (!uriString.startsWith("file:")) return null
        return Uri.parse(uriString).path?.let { File(it) }
    }

    private fun volumeDirectory(volume: StorageVolume): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return volume.directory
        }
        val path = volumePathLegacy(volume) ?: return null
        return File(path)
    }

    /** getPath() eliminado del SDK de compilación; reflexión solo en API < 30. */
    private fun volumePathLegacy(volume: StorageVolume): String? = runCatching {
        volume.javaClass.getMethod("getPath").invoke(volume) as? String
    }.getOrNull()

    private fun volumeLabel(volume: StorageVolume, dir: File?): String {
        val desc = volume.getDescription(null)?.toString()
        if (!desc.isNullOrBlank()) {
            return if (volume.isRemovable) "USB/SD: $desc" else "Interno: $desc"
        }
        return dir?.name ?: "Volumen"
    }
}
