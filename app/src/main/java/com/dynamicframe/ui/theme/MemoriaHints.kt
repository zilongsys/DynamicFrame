package com.dynamicframe.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Barra de ayuda bajo los controles (TV: al enfocar; móvil: al pulsar). */
@Composable
fun MemoriaControlsHintBar(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        color = MemoriaMuted,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

@Composable
fun rememberControlHintState(defaultHint: String = ""): Pair<String, (String) -> Unit> {
    var hint by remember { mutableStateOf(defaultHint) }
    val setHint: (String) -> Unit = { hint = it.ifBlank { defaultHint } }
    return hint to setHint
}

@Composable
fun FocusHintEffect(
    focused: Boolean,
    description: String,
    onHint: (String) -> Unit
) {
    LaunchedEffect(focused, description) {
        if (focused) onHint(description)
    }
}
