package com.dynamicframe.data.local

import android.content.Context
import com.dynamicframe.domain.model.StorageRoot
import com.dynamicframe.domain.model.StorageSubfolder
import com.dynamicframe.domain.repository.StorageBrowserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStorageBrowserRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageBrowserRepository {

    override fun displayName(folderUri: String): String =
        LocalStorageBrowser.folderDisplayName(folderUri)

    override fun listDefaultRoots(): List<StorageRoot> =
        LocalStorageBrowser.defaultRoots(context).map { entry ->
            StorageRoot(
                label = entry.name,
                folderUri = LocalStorageBrowser.toFolderUri(entry.path),
                readable = entry.readable
            )
        }

    override fun listSubfolders(folderUri: String): List<StorageSubfolder> {
        val dir = LocalStorageBrowser.filePathFromUri(folderUri) ?: return emptyList()
        return LocalStorageBrowser.listSubfolders(dir).map { entry ->
            StorageSubfolder(
                label = entry.name,
                folderUri = LocalStorageBrowser.toFolderUri(entry.path),
                readable = entry.readable
            )
        }
    }

    fun fileFromUri(folderUri: String): File? = LocalStorageBrowser.filePathFromUri(folderUri)
}
