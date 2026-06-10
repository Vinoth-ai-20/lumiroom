package com.lumiroom.core.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * All DataStore preference key definitions in one place.
 *
 * Use these keys via [AppPreferencesDataSource] — never access DataStore directly
 * outside of that class.
 */
object AppPreferenceKeys {

    // ── Onboarding ────────────────────────────────────────────────────────────
    val ONBOARDING_COMPLETE     = booleanPreferencesKey("onboarding_complete")

    // ── Theme ─────────────────────────────────────────────────────────────────
    /** Values: "LIGHT" | "DARK" | "SYSTEM" */
    val THEME_MODE              = stringPreferencesKey("theme_mode")

    // ── AR Settings ───────────────────────────────────────────────────────────
    val AR_PLANE_DISPLAY        = booleanPreferencesKey("ar_plane_display")
    /** Values: "LOW" | "MEDIUM" | "HIGH" */
    val AR_SHADOW_QUALITY       = stringPreferencesKey("ar_shadow_quality")
    /** Values: "METRIC" | "IMPERIAL" */
    val AR_MEASUREMENT_UNIT     = stringPreferencesKey("ar_measurement_unit")

    // ── Notifications & Sync ──────────────────────────────────────────────────
    val NOTIFICATIONS_ENABLED   = booleanPreferencesKey("notifications_enabled")
    val AUTO_SYNC_ENABLED       = booleanPreferencesKey("auto_sync_enabled")

    // ── Catalog State ─────────────────────────────────────────────────────────
    val LAST_SELECTED_CATEGORY  = stringPreferencesKey("last_selected_category")

    // ── AI & Voice ────────────────────────────────────────────────────────────
    val AI_SUGGESTION_COUNT     = intPreferencesKey("ai_suggestion_count")
    val VOICE_COMMANDS_ENABLED  = booleanPreferencesKey("voice_commands_enabled")
}

/**
 * Typed snapshot of all application preferences.
 * Emitted by [AppPreferencesDataSource.appPreferences].
 */
data class AppPreferences(
    val isOnboardingComplete:    Boolean = false,
    val themeMode:               String  = "SYSTEM",
    val arPlaneDisplay:          Boolean = true,
    val arShadowQuality:         String  = "MEDIUM",
    val arMeasurementUnit:       String  = "METRIC",
    val notificationsEnabled:    Boolean = true,
    val autoSyncEnabled:         Boolean = true,
    val lastSelectedCategory:    String  = "",
    val aiSuggestionCount:       Int     = 0,
    val voiceCommandsEnabled:    Boolean = true,
)
