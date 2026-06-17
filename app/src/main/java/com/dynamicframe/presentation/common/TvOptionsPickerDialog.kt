package com.dynamicframe.presentation.common

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.requestFocusWhenReady
import com.dynamicframe.ui.theme.safeClickable
import com.dynamicframe.ui.theme.tvFocusRequester
import kotlinx.coroutines.delay

@Composable
fun TvOptionsPickerDialog(
    visible: Boolean,
    title: String,
    options: List<String>,
    currentValue: String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val device = LocalDeviceProfile.current
    val selectedIndex = options.indexOf(currentValue).let { if (it < 0) 0 else it }
    val initialFocus = remember { FocusRequester() }
    var acceptSelection by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            acceptSelection = false
            delay(350)
            acceptSelection = true
            if (device.isTv) {
                initialFocus.requestFocusWhenReady()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .then(if (device.isTv) Modifier.focusGroup() else Modifier)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(options) { index, option ->
                        val isSelected = option == currentValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (index == selectedIndex && device.isTv) {
                                        Modifier.tvFocusRequester(initialFocus)
                                    } else {
                                        Modifier
                                    }
                                )
                                .safeClickable {
                                    if (!acceptSelection) return@safeClickable
                                    onSelect(index)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                option,
                                fontSize = if (device.isTv) 16.sp else 15.sp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (index < options.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
