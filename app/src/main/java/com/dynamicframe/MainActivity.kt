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
import com.dynamicframe.data.player.MusicPlayerController
import com.dynamicframe.presentation.AppRoot
import com.dynamicframe.presentation.slideshow.SlideshowEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var slideshowEngine: SlideshowEngine
    @Inject lateinit var musicController: MusicPlayerController

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
    }

    override fun onStop() {
        super.onStop()
        slideshowEngine.pause()
        musicController.pause()
    }

    override fun onDestroy() {
        if (isFinishing) {
            slideshowEngine.pause()
            musicController.disconnect()
        }
        super.onDestroy()
    }
}
