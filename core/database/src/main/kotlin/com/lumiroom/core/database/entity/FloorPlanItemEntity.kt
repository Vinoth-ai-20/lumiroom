package com.lumiroom.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "floor_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = RoomPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FurnitureEntity::class,
            parentColumns = ["id"],
            childColumns = ["furnitureId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("planId"), Index("furnitureId")]
)
data class FloorPlanItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val furnitureId: String,
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    val rotation: Float, // This acts as Y-axis rotation in AR (Yaw)
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true
)
