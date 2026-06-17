package com.dynamicframe.domain.repository

interface ImageCacheRepository {
    fun evict(uri: String)
    fun preload(uri: String, width: Int, height: Int)
}
