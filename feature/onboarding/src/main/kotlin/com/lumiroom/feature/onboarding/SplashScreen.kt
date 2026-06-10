package com.lumiroom.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.theme.LumiroomBackground
import com.lumiroom.core.ui.theme.LumiroomPrimary
import kotlinx.coroutines.delay

/**
 * Splash screen that auto-navigates after checking onboarding/auth state.
 *
 * Navigation is delegated to [SplashViewModel] which emits a single
 * [SplashEvent] driving either [onNavigateToOnboarding] or [onNavigateToMain].
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val event by viewModel.navigationEvent.collectAsState(initial = null)

    LaunchedEffect(event) {
        when (event) {
            SplashEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            SplashEvent.NavigateToAuth       -> onNavigateToAuth()
            SplashEvent.NavigateToMain       -> onNavigateToMain()
            null                             -> Unit
        }
    }

    // Animated gradient background with centered logo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LumiroomPrimary.copy(alpha = 0.3f),
                        LumiroomBackground,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.lumiroom.core.ui.R.drawable.lumiroom_logo),
            contentDescription = "Lumiroom Logo",
            modifier = Modifier.fillMaxSize(0.6f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

sealed class SplashEvent {
    object NavigateToOnboarding : SplashEvent()
    object NavigateToAuth : SplashEvent()
    object NavigateToMain : SplashEvent()
}
