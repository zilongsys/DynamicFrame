package com.dynamicframe.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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

private val TvFocusColor = Color(0xFFC75B7A)

/** Foco + D-pad/OK para Android TV y TV box. */
@Composable
fun Modifier.tvClickable(
    enabled: Boolean = true,
    showFocusBorder: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    return this
        .semantics { role = Role.Button }
        .focusable(enabled = enabled, interactionSource = interactionSource)
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
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        .then(
            if (showFocusBorder && focused) {
                Modifier.border(3.dp, TvFocusColor, RoundedCornerShape(12.dp))
            } else {
                Modifier
            }
        )
}

fun Modifier.tvFocusRequester(requester: FocusRequester): Modifier =
    focusRequester(requester)

suspend fun FocusRequester.requestFocusWhenReady() {
    delay(100)
    runCatching { requestFocus() }
}
