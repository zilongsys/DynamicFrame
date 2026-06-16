package com.dynamicframe.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.presentation.device.LocalDeviceProfile

/** Botón primario (morado) con foco D-pad en TV. */
@Composable
fun MemoriaPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    if (device.isTv) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MemoriaPurple)
                .safeClickable(
                    onClick = onClick,
                    showFocusBorder = false,
                    focusShape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, text, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MemoriaPurple)
        ) {
            Icon(icon, text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Botón contorneado con foco D-pad en TV. */
@Composable
fun MemoriaOutlinedButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    if (device.isTv) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MemoriaLine, RoundedCornerShape(10.dp))
                .background(MemoriaSurface)
                .safeClickable(
                    onClick = onClick,
                    showFocusBorder = false,
                    focusShape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, text, tint = MemoriaInk, modifier = Modifier.size(18.dp))
            Text(text, color = MemoriaInk, fontSize = 14.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MemoriaLine)
        ) {
            Icon(icon, text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text)
        }
    }
}

/** Interruptor accesible con mando: fila enfocable en TV. */
@Composable
fun TvSwitchRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (device.isTv) Modifier.safeClickable { onCheckedChange(!checked) }
                else Modifier
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, label, tint = MemoriaMuted, modifier = Modifier.size(18.dp))
        Text(label, color = MemoriaInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (device.isTv) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (checked) "Activado" else "Desactivado",
                tint = if (checked) MemoriaPurple else MemoriaMuted,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MemoriaPurple
                )
            )
        }
    }
}

/** Volumen / slider: en TV usa botones − y +. */
@Composable
fun TvVolumeStepper(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    step: Float = 0.1f,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, label, tint = MemoriaMuted, modifier = Modifier.size(18.dp))
            Text(label, color = MemoriaMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (device.isTv) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvStepperChip(
                    icon = Icons.Default.Remove,
                    desc = "Bajar",
                    onClick = { onValueChange((value - step).coerceIn(0f, 1f)) }
                )
                Text(
                    "${(value * 100).toInt()}%",
                    color = MemoriaInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 48.dp)
                )
                TvStepperChip(
                    icon = Icons.Default.Add,
                    desc = "Subir",
                    onClick = { onValueChange((value + step).coerceIn(0f, 1f)) }
                )
            }
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = MemoriaPurple,
                    activeTrackColor = MemoriaPurple
                )
            )
        }
    }
}

@Composable
fun TvStepperChip(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, MemoriaLine, CircleShape)
            .background(MemoriaSurface)
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = MemoriaInk, modifier = Modifier.size(22.dp))
    }
}

/** Botón circular grande para barra superior (álbum activo). Foco = violeta completo. */
@Composable
fun MemoriaPlaybackCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val iconSize = (size.value * 0.44f).dp

    val bg = if (focused) MemoriaPurple else MemoriaSurface
    val borderColor = if (focused) MemoriaPurple else MemoriaLine
    val iconTint = if (focused) Color.White else MemoriaInk

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interactionSource
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}
