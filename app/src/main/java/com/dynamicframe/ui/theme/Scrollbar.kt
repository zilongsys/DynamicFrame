package com.dynamicframe.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dibuja una barra de desplazamiento vertical sobre un contenedor con scroll
 * (verticalScroll). Indica visualmente que hay más contenido del visible.
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color = MemoriaLine,
    minThumb: Dp = 28.dp
): Modifier = drawWithContent {
    drawContent()
    val max = state.maxValue
    if (max <= 0) return@drawWithContent
    val viewport = size.height
    val total = viewport + max
    val thumbHeight = (viewport * viewport / total).coerceAtLeast(minThumb.toPx())
    val track = viewport - thumbHeight
    val offsetY = (state.value.toFloat() / max) * track
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - w, offsetY),
        size = Size(w, thumbHeight),
        cornerRadius = CornerRadius(w / 2, w / 2)
    )
}

/**
 * Barra de desplazamiento vertical para LazyColumn/LazyList. Aproxima la posición
 * y tamaño del thumb a partir del índice y desplazamiento del primer ítem visible.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = MemoriaLine,
    minThumb: Dp = 28.dp
): Modifier = drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems == 0 || visibleItems.isEmpty()) return@drawWithContent
    if (visibleItems.size >= totalItems) return@drawWithContent
    val viewport = size.height
    val thumbHeight = (viewport * visibleItems.size / totalItems)
        .coerceAtLeast(minThumb.toPx())
    val track = viewport - thumbHeight
    val firstVisible = visibleItems.first()
    val avgItem = (visibleItems.sumOf { it.size } / visibleItems.size).coerceAtLeast(1)
    val scrolled = firstVisible.index * avgItem - firstVisible.offset
    val maxScroll = (totalItems * avgItem - viewport).coerceAtLeast(1f)
    val fraction = (scrolled / maxScroll).coerceIn(0f, 1f)
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - w, fraction * track),
        size = Size(w, thumbHeight),
        cornerRadius = CornerRadius(w / 2, w / 2)
    )
}
