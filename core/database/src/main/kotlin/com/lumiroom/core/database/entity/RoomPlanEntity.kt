package com.lumiroom.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "room_plans")
data class RoomPlanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val roomAnchorId: String? = null,
    val gridSizeCm: Int = 10,
    val cameraPanX: Float = 0f,
    val cameraPanY: Float = 0f,
    val cameraZoom: Float = 1f,
    val planeBoundariesJson: String? = null,
    val anchorsJson: String? = null
)
