package com.dynamicframe.presentation.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.domain.debug.DebugLevel
import com.dynamicframe.domain.debug.DebugLogEntry
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.safeClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugConsoleOverlay(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val enabled by viewModel.debugEnabled.collectAsStateWithLifecycle()
    if (!enabled) return

    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val device = LocalDeviceProfile.current
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (!expanded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(device.contentPaddingH)
                    .safeClickable { expanded = true },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF6B21A8),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White)
                    Text(
                        "DBG ${logs.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (expanded) {
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF121212)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Consola de depuración",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    copyLogsToClipboard(context, viewModel.exportLogsText())
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Log copiado al portapapeles")
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, "Copiar", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.clearLogs() }) {
                                    Icon(Icons.Default.Delete, "Limpiar", tint = Color.White)
                                }
                                IconButton(onClick = { expanded = false }) {
                                    Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                                }
                            }
                        }

                        Text(
                            "Copia este log y pégalo al reportar un error.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        val listState = rememberLazyListState()
                        LaunchedEffect(logs.size) {
                            if (logs.isNotEmpty()) listState.scrollToItem(0)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        "Sin entradas aún. Usa la app y verás acciones aquí.",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                items(logs, key = { it.id }) { entry ->
                                    DebugLogRow(entry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLogRow(entry: DebugLogEntry) {
    val time = remember(entry.timestampMs) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestampMs))
    }
    val levelColor = when (entry.level) {
        DebugLevel.ERROR -> Color(0xFFEF5350)
        DebugLevel.WARN -> Color(0xFFFFB74D)
        DebugLevel.INFO -> Color(0xFF81C784)
        DebugLevel.DEBUG -> Color(0xFF64B5F6)
        DebugLevel.VERBOSE -> Color(0xFF90A4AE)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(time, color = Color.White.copy(0.45f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(entry.level.name, color = levelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(entry.tag, color = Color(0xFFCE93D8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(entry.message, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        entry.detail?.let {
            Text(it, color = Color.White.copy(0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun copyLogsToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("DynamicFrame debug log", text))
}
