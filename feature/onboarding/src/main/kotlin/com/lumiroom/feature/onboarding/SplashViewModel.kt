package com.lumiroom.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lumiroom.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Determines the initial navigation destination after the splash screen.
 *
 * Decision tree:
 *  1. Waits for a minimum brand splash duration.
 *  2. Reads [AppPreferencesDataSource.appPreferences] to check onboarding status.
 *  3. Emits [SplashEvent.NavigateToOnboarding] if first launch.
 *  4. Emits [SplashEvent.NavigateToMain] if returning user.
 *
 * Firebase auth state check is deferred to the Catalog/Main screen to avoid
 * blocking the splash duration unnecessarily.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesDataSource: AppPreferencesDataSource,
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SplashEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<SplashEvent> = _navigationEvent

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 1800L
    }

    init {
        viewModelScope.launch {
            delay(MIN_SPLASH_DURATION_MS)
            val prefs = preferencesDataSource.appPreferences.first()
            val event = if (prefs.isOnboardingComplete) {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    SplashEvent.NavigateToMain
                } else {
                    SplashEvent.NavigateToAuth
                }
            } else {
                SplashEvent.NavigateToOnboarding
            }
            _navigationEvent.emit(event)
        }
    }
}
