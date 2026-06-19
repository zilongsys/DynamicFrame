package com.dynamicframe.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.dynamicframe.BuildConfig

/** Clic seguro; en TV añade foco D-pad y borde al seleccionar. */
@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    showFocusBorder: Boolean = true,
    focusScale: Boolean = true,
    focusShape: Shape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier {
    val configuration = LocalConfiguration.current
    val runtimeTv = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val isTv = runtimeTv || BuildConfig.IS_TV

    return if (isTv) {
        tvClickable(
            enabled = enabled,
            showFocusBorder = showFocusBorder,
            focusScale = focusScale,
            focusShape = focusShape,
            interactionSource = interactionSource,
            onClick = onClick
        )
    } else {
        val mobileSource = interactionSource ?: remember { MutableInteractionSource() }
        clickable(
            enabled = enabled,
            interactionSource = mobileSource,
            indication = null,
            onClick = onClick
        )
    }
}
