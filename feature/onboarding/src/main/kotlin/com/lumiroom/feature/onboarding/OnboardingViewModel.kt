package com.lumiroom.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesDataSource: AppPreferencesDataSource,
) : ViewModel() {

    fun onOnboardingComplete() {
        viewModelScope.launch {
            preferencesDataSource.setOnboardingComplete(true)
        }
    }
}
