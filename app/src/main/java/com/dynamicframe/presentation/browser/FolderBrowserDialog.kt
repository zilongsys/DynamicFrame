package com.dynamicframe.presentation.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dynamicframe.domain.model.StorageRoot
import com.dynamicframe.domain.model.StorageSubfolder
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.NostalgiaAccent
import com.dynamicframe.ui.theme.NostalgiaSelected
import com.dynamicframe.ui.theme.safeClickable

@Composable
fun FolderBrowserDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit,
    listRoots: () -> List<StorageRoot>,
    listSubfolders: (String) -> List<StorageSubfolder>
) {
    val device = LocalDeviceProfile.current
    val roots = remember(visible) { if (visible) listRoots() else emptyList() }

    var navigationStack by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(visible) {
        if (visible) navigationStack = emptyList()
    }

    if (!visible) return

    BackHandler {
        if (navigationStack.isNotEmpty()) {
            navigationStack = navigationStack.dropLast(1)
        } else {
            onDismiss()
        }
    }

    val currentFolderUri = navigationStack.lastOrNull()
    val entries = remember(currentFolderUri, roots) {
        if (currentFolderUri == null) {
            roots.map { StorageSubfolder(it.label, it.folderUri, it.readable) }
        } else {
            listSubfolders(currentFolderUri)
        }
    }

    val pathLabel = when {
        currentFolderUri != null -> currentFolderUri
        else -> "Elige ubicación — USB suele estar en «Todos los discos» o «USB/SD»"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = !device.isTv,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = if (device.isTv) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            },
            shape = if (device.isTv) MaterialTheme.shapes.extraSmall else MaterialTheme.shapes.large,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (device.isTv) Modifier.focusGroup() else Modifier)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            pathLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                    Row(
                        modifier = Modifier.safeClickable(onClick = onDismiss),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (device.isTv) {
                            Text("Cerrar", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NostalgiaSelected.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (navigationStack.isNotEmpty()) {
                        NavActionChip(
                            label = "Atrás (..)",
                            icon = Icons.Default.ArrowUpward,
                            onClick = { navigationStack = navigationStack.dropLast(1) }
                        )
                    }
                    if (currentFolderUri != null) {
                        NavActionChip(
                            label = "Usar esta carpeta",
                            icon = Icons.Default.CheckCircle,
                            primary = true,
                            onClick = {
                                onSelectFolder(currentFolderUri)
                                onDismiss()
                            }
                        )
                    }
                    if (navigationStack.isNotEmpty()) {
                        NavActionChip(
                            label = "Inicio",
                            icon = Icons.Default.Home,
                            onClick = { navigationStack = emptyList() }
                        )
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                text = if (currentFolderUri == null) {
                                    "No se detectaron ubicaciones. Conecta un USB y vuelve a abrir el explorador."
                                } else {
                                    "Sin subcarpetas visibles. Pulsa «Usar esta carpeta» si tus fotos están aquí."
                                },
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(entries, key = { it.folderUri }) { entry ->
                        FolderRow(
                            name = entry.label,
                            icon = when {
                                !entry.readable -> Icons.Default.Lock
                                currentFolderUri == null && entry.label.contains("USB", ignoreCase = true) -> Icons.Default.Usb
                                else -> Icons.Default.Folder
                            },
                            enabled = entry.readable,
                            onClick = {
                                if (entry.readable) {
                                    navigationStack = navigationStack + entry.folderUri
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (primary) NostalgiaAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .safeClickable(onClick = onClick)
            .background(bg, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (primary) NostalgiaAccent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
            color = if (primary) NostalgiaAccent else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FolderRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.safeClickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
