package com.dynamicframe.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.dynamicframe.presentation.device.LocalDeviceProfile

/** Botón con foco D-pad para TV (sustituto de OutlinedButton en ajustes). */
@Composable
fun NostalgiaActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    destructive: Boolean = false
) {
    val device = LocalDeviceProfile.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    // Foco = relleno morado completo + contenido blanco (consistente en toda la app).
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (destructive) NostalgiaAccentDeep else NostalgiaLine
    val bg = when {
        focused -> MemoriaPurple
        destructive -> NostalgiaSelected
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        focused -> Color.White
        destructive -> NostalgiaAccentDeep
        else -> NostalgiaInk
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (focused) Modifier else Modifier.border(1.dp, borderColor, shape))
            .safeClickable(
                interactionSource = interactionSource,
                showFocusBorder = false,
                focusScale = false,
                onClick = onClick
            )
            .padding(
                horizontal = 16.dp,
                vertical = if (device.isTv) 14.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (device.isTv) 24.dp else 20.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
