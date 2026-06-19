package com.dynamicframe.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.dynamicframe.R

/**
 * Icono de aleatorio (shuffle) unificado para toda la app.
 * Se usa en Ajustes, Dashboard y cualquier sección con orden aleatorio.
 */
val ShuffleIcon: ImageVector
    @Composable
    get() = ImageVector.vectorResource(id = R.drawable.ic_shuffle_dynamic)
