package com.xylotune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xylotune.app.ui.MainScreen
import com.xylotune.app.ui.theme.XylotuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent {
            XylotuneTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // Fully immersive: the keys reach every edge of the screen. Only the
                        // display cutout (a notch/punch-hole camera) still needs clearing —
                        // the system bars themselves are hidden, reappearing only for an
                        // edge swipe (see hideSystemBars), so there's nothing else to pad for.
                        .windowInsetsPadding(WindowInsets.displayCutout),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars() // the status/nav bars can reappear across a resume; re-hide each time
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
