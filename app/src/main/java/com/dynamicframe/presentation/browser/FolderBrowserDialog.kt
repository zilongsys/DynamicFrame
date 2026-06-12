package com.dynamicframe.presentation.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dynamicframe.data.local.LocalStorageBrowser
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.safeClickable
import java.io.File

@Composable
fun FolderBrowserDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val device = LocalDeviceProfile.current
    var currentDir by remember { mutableStateOf<File?>(null) }
    val roots = remember { LocalStorageBrowser.defaultRoots(context) }

    val entries = remember(currentDir) {
        if (currentDir == null) roots
        else LocalStorageBrowser.listSubfolders(currentDir!!)
    }

    val pathLabel = currentDir?.absolutePath ?: "Ubicaciones"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !device.isTv)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (device.isTv) 0.92f else 0.75f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                            maxLines = 2
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                if (currentDir != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .safeClickable {
                                onSelectFolder(LocalStorageBrowser.toFolderUri(currentDir!!))
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Usar esta carpeta",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (currentDir != null) {
                        item {
                            FolderRow(
                                name = "Subir (..)",
                                icon = Icons.Default.ArrowUpward,
                                onClick = {
                                    currentDir = currentDir?.parentFile?.takeIf { it.canRead() }
                                }
                            )
                        }
                    }

                    items(entries, key = { it.path.absolutePath }) { entry ->
                        FolderRow(
                            name = entry.name,
                            icon = Icons.Default.Folder,
                            onClick = { currentDir = entry.path }
                        )
                    }

                    if (entries.isEmpty() && currentDir != null) {
                        item {
                            Text(
                                "No hay subcarpetas aquí. Pulsa «Usar esta carpeta».",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
