package com.dynamicframe.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamicframe.presentation.device.ProvideDeviceProfile
import com.dynamicframe.presentation.device.TvDisplayFrame
import com.dynamicframe.presentation.settings.SettingsViewModel
import com.dynamicframe.ui.theme.DynamicFrameTheme

@Composable
fun AppRoot(isTV: Boolean) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val config by settingsViewModel.config.collectAsStateWithLifecycle()

    val uiScale = if (isTV) config.uiScale.coerceIn(0.75f, 1.25f) else 1f

    DynamicFrameTheme(isTv = isTV) {
        ProvideDeviceProfile(isTv = isTV) {
            TvDisplayFrame(
                isTv = isTV,
                uiScale = uiScale,
                showScreenBorder = isTV && config.showScreenBorder
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DynamicFrameNavHost(isTV = isTV)
                }
            }
        }
    }
}
