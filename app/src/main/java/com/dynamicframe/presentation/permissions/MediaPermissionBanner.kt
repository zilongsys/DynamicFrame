package com.dynamicframe.presentation.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.ui.theme.MemoriaInk
import com.dynamicframe.ui.theme.MemoriaMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.safeClickable

@Composable
fun MediaPermissionDeniedBanner(
    message: String,
    modifier: Modifier = Modifier,
    onGrantAccess: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MemoriaPurple.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MemoriaPurple
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("Permiso de almacenamiento denegado", color = MemoriaInk, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Text(message, color = MemoriaMuted, fontSize = 13.sp)
        }
        if (onGrantAccess != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MemoriaPurple)
                    .safeClickable(onClick = onGrantAccess)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Conceder acceso", color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
