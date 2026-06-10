package com.lumiroom.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A persisted AI assistant conversation session.
 *
 * [messages] is a JSON array of chat message objects serialized via
 * [com.lumiroom.core.database.converter.JsonListConverter].
 *
 * Schema example for a single message element:
 * ```json
 * { "role": "user" | "model", "content": "...", "timestamp": 1234567890 }
 * ```
 */
@Entity(
    tableName = "ai_session",
    foreignKeys = [
        ForeignKey(
            entity = RoomDesignEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_design_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("user_id"),
        Index("room_design_id"),
    ],
)
data class AiSessionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")       val userId: String,
    @ColumnInfo(name = "room_design_id") val roomDesignId: String?,

    /** JSON array of { role, content, timestamp } objects */
    val messages: String = "[]",

    @ColumnInfo(name = "model_version") val modelVersion: String?,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
