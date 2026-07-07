package com.dynamicframe.domain.model

/** Colores dominantes extraídos de una imagen (ARGB). */
data class MediaDynamicPalette(
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
)
