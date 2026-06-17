package com.dynamicframe.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.pm.PackageManager

enum class MediaPermissionKind { PHOTOS_VIDEOS, MUSIC, ALL }

class MediaPermissionState internal constructor(
    private val requestLauncher: (Array<String>) -> Unit,
    private val missingPermissions: (Array<String>) -> Array<String>,
    private val defaultOnDenied: () -> Unit
) {
    private var pendingGranted: (() -> Unit)? = null
    private var pendingDenied: (() -> Unit)? = null

    fun requestFor(
        kind: MediaPermissionKind,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        val perms = permissionsFor(kind)
        if (perms.isEmpty()) {
            onGranted()
            return
        }
        val missing = missingPermissions(perms)
        if (missing.isEmpty()) {
            onGranted()
            return
        }
        pendingGranted = onGranted
        pendingDenied = onDenied
        requestLauncher(missing)
    }

    internal fun onResult(allGranted: Boolean) {
        if (allGranted) {
            pendingGranted?.invoke()
        } else {
            pendingDenied?.invoke() ?: defaultOnDenied()
        }
        pendingGranted = null
        pendingDenied = null
    }
}

@Composable
fun rememberMediaPermissions(
    onPermissionDenied: () -> Unit = {}
): MediaPermissionState {
    val context = LocalContext.current
    val currentContext = rememberUpdatedState(context)
    val deniedCallback = rememberUpdatedState(onPermissionDenied)
    val stateHolder = remember { mutableStateOf<MediaPermissionState?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        stateHolder.value?.onResult(results.values.all { it })
    }

    val state = remember {
        MediaPermissionState(
            requestLauncher = { perms -> launcher.launch(perms) },
            missingPermissions = { perms ->
                perms.filter {
                    ContextCompat.checkSelfPermission(currentContext.value, it) !=
                        PackageManager.PERMISSION_GRANTED
                }.toTypedArray()
            },
            defaultOnDenied = { deniedCallback.value() }
        ).also { stateHolder.value = it }
    }

    return state
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

fun missingMediaPermissions(context: Context, kind: MediaPermissionKind): Array<String> {
    return permissionsFor(kind).filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()
}

fun hasMissingMediaPermissions(context: Context): Boolean =
    missingMediaPermissions(context, MediaPermissionKind.PHOTOS_VIDEOS).isNotEmpty()

fun hasMissingMusicPermissions(context: Context): Boolean =
    missingMediaPermissions(context, MediaPermissionKind.MUSIC).isNotEmpty()
