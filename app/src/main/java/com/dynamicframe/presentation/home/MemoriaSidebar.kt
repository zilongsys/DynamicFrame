package com.dynamicframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.MemoriaInk
import com.dynamicframe.ui.theme.MemoriaLine
import com.dynamicframe.ui.theme.MemoriaMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.MemoriaPurpleSoft
import com.dynamicframe.ui.theme.AppVersionLabel
import com.dynamicframe.ui.theme.requestFocusWhenReady
import com.dynamicframe.ui.theme.safeClickable
import com.dynamicframe.ui.theme.tvFocusRequester
import com.dynamicframe.ui.theme.verticalScrollbar
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

@Composable
fun MemoriaSidebar(
    selected: MemoriaDestination,
    photoCount: Int,
    onSelect: (MemoriaDestination) -> Unit,
    selectedItemFocus: FocusRequester? = null,
    contentFocus: FocusRequester? = null
) {
    val device = LocalDeviceProfile.current
    val firstFocus = remember { FocusRequester() }
    val localSelectedFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    // El requester del item seleccionado se comparte con el contenido para que
    // al pulsar IZQUIERDA desde la lista el foco vuelva siempre aquí (estilo Netflix).
    val selectedFocus = selectedItemFocus ?: localSelectedFocus
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(device.isTv) {
        if (device.isTv) {
            delay(300)
            firstFocus.requestFocusWhenReady()
            initialized = true
        }
    }

    // Restaura el foco al item seleccionado cuando se vuelve al sidebar (estilo Netflix)
    LaunchedEffect(selected) {
        if (device.isTv && initialized) {
            delay(80)
            runCatching { selectedFocus.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Color.White)
            .border(width = 1.dp, color = MemoriaLine)
            .verticalScrollbar(scrollState, color = MemoriaLine)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(scrollState)
            .then(
                if (device.isTv) Modifier
                    // Flecha DERECHA desde el menú → primer elemento focable del contenido.
                    .then(
                        if (contentFocus != null) Modifier.focusProperties { right = contentFocus }
                        else Modifier
                    )
                    .focusGroup()
                else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.focusProperties { canFocus = false }
        ) {
            Text(
                "MEMORIA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MemoriaInk,
                letterSpacing = (-0.5).sp
            )
            AppVersionLabel(
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                fontSize = 11.sp
            )
        }
        Text(
            "cuadro interactivo",
            fontSize = 12.sp,
            color = MemoriaMuted,
            modifier = Modifier.focusProperties { canFocus = false }
        )
        Spacer(Modifier.height(28.dp))

        RoadmapGroup.entries.forEach { group ->
            Text(
                group.label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MemoriaMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            val entries = memoriaSidebarEntries.filter { it.group == group }
            entries.forEachIndexed { index, entry ->
                val isFirst = index == 0 && group == RoadmapGroup.BIBLIOTECA
                val isSelected = destinationEquals(selected, entry.destination)
                val badge = if (entry.destination is MemoriaDestination.AlbumActive && photoCount > 0) {
                    photoCount.toString()
                } else entry.badge

                MemoriaSidebarItem(
                    label = entry.label,
                    icon = entry.icon,
                    selected = isSelected,
                    badge = badge,
                    onClick = { onSelect(entry.destination) },
                    modifier = when {
                        isSelected && device.isTv -> Modifier.tvFocusRequester(selectedFocus)
                        isFirst && device.isTv -> Modifier.tvFocusRequester(firstFocus)
                        else -> Modifier
                    }
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))
        AppVersionLabel(showBuildCode = true, fontSize = 10.sp)
    }
}

@Composable
private fun MemoriaSidebarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    // Foco = relleno morado completo + contenido blanco; seleccionado (sin foco) = morado suave.
    val bg = when {
        focused -> MemoriaPurple
        selected -> MemoriaPurpleSoft
        else -> Color.Transparent
    }
    val contentColor = when {
        focused -> Color.White
        selected -> MemoriaInk
        else -> MemoriaMuted
    }
    val iconTint = when {
        focused -> Color.White
        selected -> MemoriaPurple
        else -> MemoriaMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusScale = false,
                interactionSource = source
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selected && !focused) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(MemoriaPurple, RoundedCornerShape(2.dp))
            )
        }
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        badge?.let {
            Surface(
                color = if (focused) Color.White.copy(alpha = 0.22f) else MemoriaPurpleSoft,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp, color = if (focused) Color.White else MemoriaPurple)
            }
        }
    }
}

private fun destinationEquals(a: MemoriaDestination, b: MemoriaDestination): Boolean = when {
    a is MemoriaDestination.Roadmap && b is MemoriaDestination.Roadmap -> a.feature == b.feature
    else -> a::class == b::class
}

@Composable
fun MemoriaPhoneBottomNav(
    selected: MemoriaDestination,
    onSelect: (MemoriaDestination) -> Unit
) {
    val tabs = listOf(
        MemoriaDestination.AlbumActive to "Inicio",
        MemoriaDestination.Albums to "Álbumes",
        MemoriaDestination.Music to "Música",
        MemoriaDestination.Settings to "Ajustes"
    )
    NavigationBar(
        containerColor = com.dynamicframe.ui.theme.MemoriaSurface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { (dest, label) ->
            val icon = when (dest) {
                MemoriaDestination.AlbumActive -> Icons.Default.PhotoAlbum
                MemoriaDestination.Albums -> Icons.Default.PhotoLibrary
                MemoriaDestination.Music -> Icons.Default.MusicNote
                else -> Icons.Default.Settings
            }
            val isSelected = destinationEquals(selected, dest)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(dest) },
                icon = { Icon(icon, label, tint = if (isSelected) MemoriaPurple else MemoriaMuted) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MemoriaPurple,
                    selectedTextColor = MemoriaPurple,
                    indicatorColor = MemoriaPurpleSoft
                )
            )
        }
    }
}
