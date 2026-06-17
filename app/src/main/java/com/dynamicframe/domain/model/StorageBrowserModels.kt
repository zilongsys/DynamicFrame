package com.dynamicframe.domain.model

data class StorageRoot(
    val label: String,
    val folderUri: String,
    val readable: Boolean
)

data class StorageSubfolder(
    val label: String,
    val folderUri: String,
    val readable: Boolean
)
