package com.dynamicframe.domain.repository

import com.dynamicframe.domain.model.StorageRoot
import com.dynamicframe.domain.model.StorageSubfolder

interface StorageBrowserRepository {
    fun displayName(folderUri: String): String

    fun listDefaultRoots(): List<StorageRoot>

    fun listSubfolders(folderUri: String): List<StorageSubfolder>
}
