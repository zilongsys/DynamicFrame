package com.dynamicframe.presentation.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dynamicframe.domain.model.DeleteMediaFailure
import com.dynamicframe.domain.model.DeleteMediaFailureAction
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.NostalgiaAccentDeep
import com.dynamicframe.ui.theme.requestFocusWhenReady
import com.dynamicframe.ui.theme.safeClickable
import com.dynamicframe.ui.theme.tvFocusRequester

@Composable
fun DeleteMediaFailureDialog(
    failure: DeleteMediaFailure?,
    onDismiss: () -> Unit,
    onOpenContentSettings: () -> Unit = {},
) {
    if (failure == null) return
    val device = LocalDeviceProfile.current
    val context = LocalContext.current
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(failure) {
        if (device.isTv) closeFocus.requestFocusWhenReady()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .then(if (device.isTv) Modifier.focusGroup() else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = NostalgiaAccentDeep,
                    modifier = Modifier.size(if (device.isTv) 40.dp else 32.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    failure.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    failure.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Qué puedes hacer",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            failure.solution,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                val actionLabel = when (failure.action) {
                    DeleteMediaFailureAction.OPEN_CONTENT_SETTINGS -> "Ir a Ajustes → Contenido"
                    DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY -> "Abrir con otra app"
                    DeleteMediaFailureAction.NONE -> null
                }

                if (actionLabel != null) {
                    DialogActionButton(
                        label = actionLabel,
                        primary = true,
                        onClick = {
                            when (failure.action) {
                                DeleteMediaFailureAction.OPEN_CONTENT_SETTINGS -> onOpenContentSettings()
                                DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY -> {
                                    failure.mediaUri?.let { uriString ->
                                        openMediaExternally(context, uriString)
                                    }
                                }
                                DeleteMediaFailureAction.NONE -> Unit
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }

                DialogActionButton(
                    label = "Entendido",
                    primary = actionLabel == null,
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (device.isTv) Modifier.tvFocusRequester(closeFocus) else Modifier),
                )
            }
        }
    }
}

private fun openMediaExternally(context: android.content.Context, uriString: String) {
    runCatching {
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, guessMimeType(uriString))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir con"))
    }
}

private fun guessMimeType(uriString: String): String = when {
    uriString.contains("video", ignoreCase = true) -> "video/*"
    else -> "image/*"
}

@Composable
private fun DialogActionButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (primary) NostalgiaAccentDeep else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .safeClickable(onClick = onClick)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(color = bg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 14.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
