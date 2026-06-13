package com.lumiroom.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.datastore.AppPreferences
import com.lumiroom.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.lumiroom.core.domain.sync.SyncManager

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: AppPreferencesDataSource,
    private val syncManager: SyncManager
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesDataSource.appPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences(),
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesDataSource.setThemeMode(mode) }
    }

    fun setArPlaneDisplay(enabled: Boolean) {
        viewModelScope.launch { preferencesDataSource.setArPlaneDisplay(enabled) }
    }

    fun setVoiceCommandsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataSource.setVoiceCommandsEnabled(enabled) }
    }

    fun setArMeasurementUnit(unit: String) {
        viewModelScope.launch { preferencesDataSource.setArMeasurementUnit(unit) }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataSource.setAutoSyncEnabled(enabled) }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.scheduleRoomSync()
            syncManager.syncDownRooms()
        }
    }
}
