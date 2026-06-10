package com.lumiroom.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A persisted room design (a named collection of [PlacedItemEntity] items).
 *
 * [arEnvironmentId] stores an ARCore Cloud Anchor ID for spatial persistence
 * if Cloud Anchors are available; null otherwise.
 */
@Entity(
    tableName = "room_design",
    indices = [
        Index("user_id"),
        Index("room_type"),
        Index("sync_status"),
    ],
)
data class RoomDesignEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val name: String,
    val description: String?,

    @ColumnInfo(name = "room_type")
    val roomType: String,          // Living Room, Bedroom, Kitchen, etc.

    @ColumnInfo(name = "width_m")  val widthM: Float?,
    @ColumnInfo(name = "length_m") val lengthM: Float?,
    @ColumnInfo(name = "height_m") val heightM: Float?,

    @ColumnInfo(name = "ar_environment_id")
    val arEnvironmentId: String?,  // ARCore Cloud Anchor ID

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,

    @ColumnInfo(name = "firestore_doc_id")
    val firestoreDocId: String?,

    /** Sync state machine: "local" | "queued" | "syncing" | "synced" | "failed" */
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "local",

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "style_tag")
    val styleTag: String?,         // AI-assigned style label

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
