package com.dynamicframe.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.presentation.debug.DebugConsoleOverlay
import com.dynamicframe.presentation.debug.DebugViewModel
import com.dynamicframe.presentation.device.ProvideDeviceProfile
import com.dynamicframe.presentation.device.TvDisplayFrame
import com.dynamicframe.presentation.settings.SettingsViewModel
import com.dynamicframe.ui.theme.DynamicFrameTheme

@Composable
fun AppRoot(isTV: Boolean, autoStart: Boolean = false) {
    val activity = LocalContext.current as ComponentActivity
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val debugViewModel: DebugViewModel = hiltViewModel(activity)
    val config by settingsViewModel.config.collectAsStateWithLifecycle()

    val uiScale = if (isTV) config.uiScale.coerceIn(0.75f, 1.25f) else 1f

    DynamicFrameTheme(isTv = isTV) {
        ProvideDeviceProfile(isTv = isTV) {
            TvDisplayFrame(
                isTv = isTV,
                uiScale = uiScale,
                showScreenBorder = isTV && config.showScreenBorder
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DynamicFrameNavHost(
                            isTV = isTV,
                            debugViewModel = debugViewModel,
                            autoStart = autoStart
                        )
                    }
                    DebugConsoleOverlay(viewModel = debugViewModel)
                }
            }
        }
    }
}
