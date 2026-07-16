package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.dynamicframe.domain.model.ClockPosition

fun ClockPosition.toAlignment(): Alignment = when (this) {
    ClockPosition.TOP_LEFT -> Alignment.TopStart
    ClockPosition.TOP_RIGHT -> Alignment.TopEnd
    ClockPosition.BOTTOM_LEFT -> Alignment.BottomStart
    ClockPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
    ClockPosition.CENTER -> Alignment.Center
}

fun ClockPosition.toOverlayPadding(
    edge: Int = 16,
    paradiseEdgeH: Int = 44,
    paradiseEdgeV: Int = 52,
    paradise: Boolean = false,
): PaddingValues {
    val h = if (paradise) paradiseEdgeH.dp else edge.dp
    val v = if (paradise) paradiseEdgeV.dp else edge.dp
    return when (this) {
        ClockPosition.TOP_LEFT -> PaddingValues(start = h, top = v)
        ClockPosition.TOP_RIGHT -> PaddingValues(end = h, top = v)
        ClockPosition.BOTTOM_LEFT -> PaddingValues(start = h, bottom = v)
        ClockPosition.BOTTOM_RIGHT -> PaddingValues(end = h, bottom = v)
        ClockPosition.CENTER -> PaddingValues(0.dp)
    }
}
