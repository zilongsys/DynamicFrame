package com.dynamicframe.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dynamicframe.domain.model.hasCustomMediaFolders
import com.dynamicframe.presentation.debug.DebugViewModel
import com.dynamicframe.presentation.home.HomeScreen
import com.dynamicframe.presentation.permissions.MediaPermissionKind
import com.dynamicframe.presentation.permissions.MediaPermissionState
import com.dynamicframe.presentation.permissions.hasMissingMediaPermissions
import com.dynamicframe.presentation.permissions.rememberMediaPermissions
import com.dynamicframe.presentation.settings.SettingsScreen
import com.dynamicframe.presentation.settings.SettingsViewModel
import com.dynamicframe.presentation.slideshow.SlideshowScreen
import com.dynamicframe.presentation.slideshow.SlideshowViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Slideshow : Screen("slideshow")
    object Settings : Screen("settings")
}

@Composable
fun DynamicFrameNavHost(
    isTV: Boolean,
    debugViewModel: DebugViewModel,
    autoStart: Boolean = false,
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalContext.current as ComponentActivity
    val slideshowViewModel: SlideshowViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val settingsConfig by settingsViewModel.config.collectAsStateWithLifecycle()

    var mediaPermissionDenied by remember { mutableStateOf(false) }
    val permissions = rememberMediaPermissions { mediaPermissionDenied = true }

    LaunchedEffect(settingsConfig.photoFolderUris, settingsConfig.videoFolderUris) {
        val usesMediaStore = !settingsConfig.hasCustomMediaFolders()
        mediaPermissionDenied = usesMediaStore && hasMissingMediaPermissions(activity)
    }

    LaunchedEffect(mediaPermissionDenied) {
        if (mediaPermissionDenied) {
            debugViewModel.logWarn("Permisos", "Falta acceso a fotos/vídeos (MediaStore)")
        }
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            debugViewModel.logInfo("Nav", "→ ${destination.route ?: "?"}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    var fullscreenNavLocked by remember { mutableStateOf(false) }
    LaunchedEffect(fullscreenNavLocked) {
        if (fullscreenNavLocked) {
            delay(450)
            fullscreenNavLocked = false
        }
    }

    fun openFullscreen() {
        if (fullscreenNavLocked) return
        if (navController.currentDestination?.route == Screen.Slideshow.route) return
        fullscreenNavLocked = true
        navController.navigate(Screen.Slideshow.route) {
            launchSingleTop = true
        }
    }

    // Auto-arranque tras reinicio (Android TV): abre el slideshow a pantalla completa.
    LaunchedEffect(autoStart) {
        if (autoStart) {
            delay(800)
            slideshowViewModel.startSlideshow(freshSession = true)
            openFullscreen()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                isTV = isTV,
                slideshowViewModel = slideshowViewModel,
                settingsViewModel = settingsViewModel,
                mediaPermissionDenied = mediaPermissionDenied,
                permissions = permissions,
                onOpenFullscreen = { openFullscreen() }
            )
        }

        composable(Screen.Slideshow.route) {
            SlideshowScreen(
                viewModel = slideshowViewModel,
                isTV = isTV,
                showPermissionDenied = mediaPermissionDenied,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                showPermissionDenied = mediaPermissionDenied,
                requestMediaAccess = { onGranted ->
                    permissions.requestFor(MediaPermissionKind.PHOTOS_VIDEOS, onGranted)
                },
                requestMusicAccess = { onGranted ->
                    permissions.requestFor(MediaPermissionKind.MUSIC, onGranted)
                },
                onMediaChanged = { slideshowViewModel.reloadMedia() }
            )
        }
    }
}
