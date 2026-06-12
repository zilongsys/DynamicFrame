package com.dynamicframe.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val borderColor = if (destructive) NostalgiaAccentDeep else NostalgiaLine
    val bg = if (destructive) NostalgiaSelected else MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .safeClickable(onClick = onClick)
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
                tint = if (destructive) NostalgiaAccentDeep else NostalgiaInk,
                modifier = Modifier.size(if (device.isTv) 24.dp else 20.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = if (destructive) NostalgiaAccentDeep else NostalgiaInk,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
