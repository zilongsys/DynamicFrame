package com.dynamicframe.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dynamicframe.presentation.home.HomeScreen
import com.dynamicframe.presentation.permissions.MediaPermissionKind
import com.dynamicframe.presentation.permissions.rememberMediaPermissions
import com.dynamicframe.presentation.settings.SettingsScreen
import com.dynamicframe.presentation.slideshow.SlideshowScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Slideshow : Screen("slideshow")
    object Settings : Screen("settings")
}

@Composable
fun DynamicFrameNavHost(
    isTV: Boolean,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                isTV = isTV,
                onOpenFullscreen = { navController.navigate(Screen.Slideshow.route) }
            )
        }

        composable(Screen.Slideshow.route) {
            SlideshowScreen(
                isTV = isTV,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val permissions = rememberMediaPermissions()
            SettingsScreen(
                onBack = { navController.popBackStack() },
                requestMediaAccess = { onGranted ->
                    permissions.requestFor(MediaPermissionKind.PHOTOS_VIDEOS, onGranted)
                },
                requestMusicAccess = { onGranted ->
                    permissions.requestFor(MediaPermissionKind.MUSIC, onGranted)
                }
            )
        }
    }
}
