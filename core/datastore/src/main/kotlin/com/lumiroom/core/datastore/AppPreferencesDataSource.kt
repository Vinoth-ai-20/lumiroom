package com.lumiroom.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single access point for all DataStore preferences.
 *
 * Reading: collect [appPreferences] to observe changes reactively.
 * Writing: call the individual `set*` suspend functions.
 */
@Singleton
class AppPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Reactive stream of the current [AppPreferences] snapshot.
     * Emits the default [AppPreferences] on IO errors instead of crashing.
     */
    val appPreferences: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AppPreferences(
                isOnboardingComplete = prefs[AppPreferenceKeys.ONBOARDING_COMPLETE] ?: false,
                themeMode            = prefs[AppPreferenceKeys.THEME_MODE]            ?: "SYSTEM",
                arPlaneDisplay       = prefs[AppPreferenceKeys.AR_PLANE_DISPLAY]      ?: true,
                arShadowQuality      = prefs[AppPreferenceKeys.AR_SHADOW_QUALITY]     ?: "MEDIUM",
                arMeasurementUnit    = prefs[AppPreferenceKeys.AR_MEASUREMENT_UNIT]   ?: "METRIC",
                notificationsEnabled = prefs[AppPreferenceKeys.NOTIFICATIONS_ENABLED] ?: true,
                autoSyncEnabled      = prefs[AppPreferenceKeys.AUTO_SYNC_ENABLED]     ?: true,
                lastSelectedCategory = prefs[AppPreferenceKeys.LAST_SELECTED_CATEGORY] ?: "",
                aiSuggestionCount    = prefs[AppPreferenceKeys.AI_SUGGESTION_COUNT]   ?: 0,
                voiceCommandsEnabled = prefs[AppPreferenceKeys.VOICE_COMMANDS_ENABLED] ?: true,
            )
        }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[AppPreferenceKeys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[AppPreferenceKeys.THEME_MODE] = mode }
    }

    suspend fun setArPlaneDisplay(enabled: Boolean) {
        dataStore.edit { it[AppPreferenceKeys.AR_PLANE_DISPLAY] = enabled }
    }

    suspend fun setArShadowQuality(quality: String) {
        dataStore.edit { it[AppPreferenceKeys.AR_SHADOW_QUALITY] = quality }
    }

    suspend fun setArMeasurementUnit(unit: String) {
        dataStore.edit { it[AppPreferenceKeys.AR_MEASUREMENT_UNIT] = unit }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[AppPreferenceKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[AppPreferenceKeys.AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setLastSelectedCategory(category: String) {
        dataStore.edit { it[AppPreferenceKeys.LAST_SELECTED_CATEGORY] = category }
    }

    suspend fun incrementAiSuggestionCount() {
        dataStore.edit { prefs ->
            prefs[AppPreferenceKeys.AI_SUGGESTION_COUNT] =
                (prefs[AppPreferenceKeys.AI_SUGGESTION_COUNT] ?: 0) + 1
        }
    }

    suspend fun setVoiceCommandsEnabled(enabled: Boolean) {
        dataStore.edit { it[AppPreferenceKeys.VOICE_COMMANDS_ENABLED] = enabled }
    }
}
