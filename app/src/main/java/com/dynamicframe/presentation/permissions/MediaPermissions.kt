package com.dynamicframe.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentLinkedQueue

enum class MediaPermissionKind { PHOTOS_VIDEOS, MUSIC, ALL }

class MediaPermissionState internal constructor(
    private val request: (Array<String>) -> Unit
) {
    fun requestFor(kind: MediaPermissionKind, onGranted: () -> Unit = {}) {
        val perms = permissionsFor(kind)
        if (perms.isEmpty()) {
            onGranted()
            return
        }
        pendingActions.add(onGranted)
        request(perms)
    }

    companion object {
        private val pendingActions = ConcurrentLinkedQueue<() -> Unit>()

        internal fun flushGranted() {
            while (true) {
                val action = pendingActions.poll() ?: break
                action()
            }
        }

        internal fun clearPending() {
            pendingActions.clear()
        }
    }
}

@Composable
fun rememberMediaPermissions(): MediaPermissionState {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            MediaPermissionState.flushGranted()
        } else {
            MediaPermissionState.clearPending()
        }
    }

    return remember {
        MediaPermissionState { perms ->
            val missing = perms.filter {
                ContextCompat.checkSelfPermission(context, it) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                MediaPermissionState.flushGranted()
            } else {
                launcher.launch(missing.toTypedArray())
            }
        }
    }
}

private fun permissionsFor(kind: MediaPermissionKind): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when (kind) {
            MediaPermissionKind.PHOTOS_VIDEOS -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            MediaPermissionKind.MUSIC -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            MediaPermissionKind.ALL -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        }
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
