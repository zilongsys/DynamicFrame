package com.dynamicframe.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// MemoriaPurple definido en MemoriaTheme.kt (mismo paquete)

private val TvFocusColor = MemoriaPurple

/** Foco + D-pad/OK para Android TV y TV box. */
@Composable
fun Modifier.tvClickable(
    enabled: Boolean = true,
    showFocusBorder: Boolean = true,
    focusShape: Shape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()

    return this
        .semantics { role = Role.Button }
        .focusable(enabled = enabled, interactionSource = source)
        .onKeyEvent { event ->
            if (!enabled) return@onKeyEvent false
            val isSelectKey = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter ||
                event.key == Key.Spacebar ||
                event.key == Key.ButtonA
            if (isSelectKey && event.type == KeyEventType.KeyDown) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
        .then(
            when {
                !showFocusBorder && focused -> Modifier.scale(1.06f)
                showFocusBorder && focused -> Modifier.border(2.dp, TvFocusColor, focusShape)
                else -> Modifier
            }
        )
}

fun Modifier.tvFocusRequester(requester: FocusRequester): Modifier =
    focusRequester(requester)

suspend fun FocusRequester.requestFocusWhenReady() {
    delay(100)
    runCatching { requestFocus() }
}

/** En TV, cualquier dirección del D-pad revela controles (estilo Netflix). */
fun Modifier.tvRevealOnDpad(
    enabled: Boolean,
    onReveal: () -> Unit
): Modifier {
    if (!enabled) return this
    return onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.key) {
            Key.DirectionDown,
            Key.DirectionUp,
            Key.DirectionLeft,
            Key.DirectionRight,
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter,
            Key.ButtonA -> {
                onReveal()
                true
            }
            else -> false
        }
    }
}
