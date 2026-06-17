package com.dynamicframe.presentation.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.PaperInk
import com.dynamicframe.ui.theme.PaperMuted

@Composable
fun MediaPermissionDeniedBanner(
    message: String,
    modifier: Modifier = Modifier
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Permiso de almacenamiento denegado", color = PaperInk, fontSize = 14.sp)
            Text(message, color = PaperMuted, fontSize = 13.sp)
        }
    }
}
