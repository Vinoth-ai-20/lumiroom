package com.lumiroom.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "walls",
    foreignKeys = [
        ForeignKey(
            entity = RoomPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WallEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val thicknessCm: Float = 10f
)
