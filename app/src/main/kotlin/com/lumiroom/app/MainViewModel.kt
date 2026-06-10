package com.lumiroom.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesDataSource: AppPreferencesDataSource
) : ViewModel() {

    val themeMode: StateFlow<String> = preferencesDataSource.appPreferences
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "SYSTEM"
        )
}
