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
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            LumiroomTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LumiroomNavHost()
                }
            }
        }
    }
}
