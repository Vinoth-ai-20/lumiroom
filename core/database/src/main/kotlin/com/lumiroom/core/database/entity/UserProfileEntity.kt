package com.lumiroom.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Locally cached user profile. The [id] matches the Firebase UID. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,                          // Firebase UID

    val email: String,

    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "photo_url")    val photoUrl: String?,

    /** AI style quiz result: e.g. "Scandinavian", "Industrial", "Maximalist" */
    @ColumnInfo(name = "style_preference") val stylePreference: String?,

    @ColumnInfo(name = "preferred_currency") val preferredCurrency: String = "USD",
    val units: String = "metric",            // "metric" | "imperial"

    @ColumnInfo(name = "is_premium") val isPremium: Boolean = false,

    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long?,
)
