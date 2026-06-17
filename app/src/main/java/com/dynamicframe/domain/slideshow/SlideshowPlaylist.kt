package com.dynamicframe.domain.slideshow

import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.SlideshowConfig
import com.dynamicframe.domain.model.TransitionType

fun transitionMillis(type: TransitionType, configuredMs: Int): Int {
    val base = configuredMs.coerceIn(600, 3200)
    return when (type) {
        TransitionType.NONE -> 0
        TransitionType.DISSOLVE, TransitionType.CROSSFADE, TransitionType.KEN_BURNS ->
            base.coerceIn(900, 3200)
        TransitionType.BLUR_FADE -> (base * 1.1f).toInt()
        else -> base
    }
}

/** Mezcla fotos y vídeos por separado, conservando el orden relativo del origen. */
fun buildPlaylist(items: List<MediaItem>, config: SlideshowConfig): List<MediaItem> {
    if (items.isEmpty()) return items
    val photos = items.filter { it.type == MediaType.IMAGE }
    val videos = items.filter { it.type == MediaType.VIDEO }
    val photoOrder = if (config.photoShuffle) photos.shuffled() else photos
    val videoOrder = if (config.videoShuffle) videos.shuffled() else videos
    val photoIter = photoOrder.iterator()
    val videoIter = videoOrder.iterator()
    return items.mapNotNull { item ->
        when (item.type) {
            MediaType.IMAGE -> if (photoIter.hasNext()) photoIter.next() else null
            MediaType.VIDEO -> if (videoIter.hasNext()) videoIter.next() else null
            else -> item
        }
    }
}
