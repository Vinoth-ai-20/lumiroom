package com.lumiroom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lumiroom.app.navigation.LumiroomNavHost
import com.lumiroom.core.ui.theme.LumiroomTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the entire Lumiroom application.
 *
 * Responsibilities:
 * - Install the splash screen before content is set.
 * - Enable edge-to-edge display.
 * - Host the root [LumiroomNavHost] inside [LumiroomTheme].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LumiroomTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LumiroomNavHost()
                }
            }
        }
    }
}
