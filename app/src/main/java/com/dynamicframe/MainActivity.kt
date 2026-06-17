package com.dynamicframe

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.usecase.PauseAppPlaybackUseCase
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.presentation.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pauseAppPlayback: PauseAppPlaybackUseCase
    @Inject lateinit var debugLogger: AppDebugLogger

    val isTV: Boolean
        get() {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
            if (BuildConfig.IS_TV) return true
            return false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            AppRoot(isTV = isTV)
        }
        debugLogger.i("Lifecycle", "MainActivity onCreate (tv=$isTV)")
    }

    override fun onStop() {
        super.onStop()
        debugLogger.i("Lifecycle", "MainActivity onStop → pauseAll")
        pauseAppPlayback.pauseAll()
    }

    override fun onDestroy() {
        if (isFinishing) {
            debugLogger.i("Lifecycle", "MainActivity onDestroy → disconnectAll")
            pauseAppPlayback.disconnectAll()
        }
        super.onDestroy()
    }
}
