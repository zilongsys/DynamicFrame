package com.dynamicframe.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.ui.theme.FocusHintEffect
import com.dynamicframe.ui.theme.MemoriaInk
import com.dynamicframe.ui.theme.MemoriaLine
import com.dynamicframe.ui.theme.MemoriaMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.MemoriaPurpleSoft
import com.dynamicframe.ui.theme.MemoriaSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TvPickerChipStyle { Full, Compact }

@Composable
fun TvPickerChip(
    title: String,
    icon: ImageVector,
    displayValue: String,
    options: List<String>,
    currentValue: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: TvPickerChipStyle = TvPickerChipStyle.Full,
    description: String = title,
    onFocusHint: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = description, onHint = onFocusHint ?: {})
    var showListDialog by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    var keyHeld by remember { mutableStateOf(false) }
    var repeatDownCount by remember { mutableIntStateOf(0) }
    var suppressShortPress by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(8.dp)
    val chipHeight = if (style == TvPickerChipStyle.Compact) 52.dp else 56.dp

    fun cycleSelection() {
        val idx = options.indexOf(currentValue).let { current ->
            if (current < 0) 0 else (current + 1) % options.size
        }
        onSelect(idx)
    }

    fun isSelectKey(key: Key): Boolean =
        key == Key.DirectionCenter ||
            key == Key.Enter ||
            key == Key.NumPadEnter ||
            key == Key.Spacebar ||
            key == Key.ButtonA

    fun cancelHoldJob() {
        holdJob?.cancel()
        holdJob = null
    }

    fun resetGesture() {
        keyHeld = false
        repeatDownCount = 0
        longPressTriggered = false
        cancelHoldJob()
    }

    fun suppressChipPressAfterDialog() {
        suppressShortPress = true
        scope.launch {
            delay(450)
            suppressShortPress = false
        }
    }

    fun dismissDialog() {
        showListDialog = false
        suppressChipPressAfterDialog()
        resetGesture()
    }

    fun openListDialog() {
        showListDialog = true
        longPressTriggered = true
        suppressChipPressAfterDialog()
        cancelHoldJob()
    }

    fun startHoldDetection() {
        cancelHoldJob()
        holdJob = scope.launch {
            delay(500)
            if (keyHeld && !showListDialog) {
                openListDialog()
            }
        }
    }

    TvOptionsPickerDialog(
        visible = showListDialog,
        title = title,
        options = options,
        currentValue = currentValue,
        onSelect = onSelect,
        onDismiss = { dismissDialog() }
    )

    // Foco = relleno morado completo + contenido blanco (consistente en toda la app).
    val bg = if (focused) MemoriaPurple else MemoriaSurface

    Box(
        modifier = modifier
            .height(chipHeight)
            .clip(shape)
            .background(bg, shape)
            .then(if (focused) Modifier else Modifier.border(1.dp, MemoriaLine, shape))
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (!isSelectKey(event.key)) return@onKeyEvent false
                if (showListDialog || suppressShortPress) return@onKeyEvent true
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (!keyHeld) {
                            keyHeld = true
                            repeatDownCount = 0
                            startHoldDetection()
                        } else {
                            repeatDownCount++
                            if (repeatDownCount >= 4 && !longPressTriggered) {
                                openListDialog()
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        cancelHoldJob()
                        if (showListDialog) {
                            // El KeyUp del mantener OK no debe ciclar ni resetear mientras el diálogo está abierto.
                            true
                        } else if (!longPressTriggered && !suppressShortPress) {
                            cycleSelection()
                            resetGesture()
                            true
                        } else {
                            resetGesture()
                            true
                        }
                    }
                    else -> false
                }
            }
            .padding(horizontal = if (style == TvPickerChipStyle.Compact) 4.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (style == TvPickerChipStyle.Compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = if (focused) Color.White else MemoriaMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    displayValue,
                    color = if (focused) Color.White else MemoriaInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = if (focused) Color.White
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(text = title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (focused) Color.White else MemoriaInk)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayValue,
                        color = if (focused) Color.White else MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                        tint = if (focused) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
