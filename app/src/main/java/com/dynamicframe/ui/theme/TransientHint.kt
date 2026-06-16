package com.dynamicframe.ui.theme

import androidx.compose.runtime.*

/** Muestra un indicador solo durante [visibleMs] tras cada llamada a [trigger]. */
@Composable
fun rememberTransientVisibility(visibleMs: Long = 1_000L): Pair<Boolean, () -> Unit> {
    var visible by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    val trigger: () -> Unit = {
        tick++
        visible = true
    }

    LaunchedEffect(tick) {
        if (!visible) return@LaunchedEffect
        kotlinx.coroutines.delay(visibleMs)
        visible = false
    }

    return visible to trigger
}
