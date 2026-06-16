package com.dynamicframe.domain.model

fun SlideshowConfig.hasCustomMediaFolders(): Boolean =
    photoFolderUris.isNotEmpty() || videoFolderUris.isNotEmpty()

fun SlideshowConfig.allMediaFolderUris(): List<String> =
    (photoFolderUris + videoFolderUris).distinct()

private const val PHOTO_PILL_PREFIX = "photo:"
private const val VIDEO_PILL_PREFIX = "video:"

fun photoFolderPillId(uri: String): String = "$PHOTO_PILL_PREFIX$uri"

fun videoFolderPillId(uri: String): String = "$VIDEO_PILL_PREFIX$uri"

enum class MediaFolderKind { PHOTO, VIDEO }

/** Resuelve ids de píldoras (photo:/video:) a rutas de carpeta para el repositorio. */
fun foldersForAlbumSelection(
    allFolders: List<String>,
    selectedIds: List<String>,
    kind: MediaFolderKind
): List<String> {
    if (selectedIds.isEmpty()) return allFolders

    val prefix = when (kind) {
        MediaFolderKind.PHOTO -> PHOTO_PILL_PREFIX
        MediaFolderKind.VIDEO -> VIDEO_PILL_PREFIX
    }
    val hasTypedSelection = selectedIds.any {
        it.startsWith(PHOTO_PILL_PREFIX) || it.startsWith(VIDEO_PILL_PREFIX)
    }
    if (hasTypedSelection) {
        val forKind = selectedIds
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
        return allFolders.filter { it in forKind }
    }

    val legacy = selectedIds.filter { it.startsWith("content://") || it.startsWith("file:") }
    return if (legacy.isEmpty()) allFolders else allFolders.filter { it in legacy }
}
