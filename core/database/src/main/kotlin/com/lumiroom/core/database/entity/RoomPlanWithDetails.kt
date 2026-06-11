package com.lumiroom.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RoomPlanWithDetails(
    @Embedded val roomPlan: RoomPlanEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val walls: List<WallEntity>,
    
    @Relation(
        entity = FloorPlanItemEntity::class,
        parentColumn = "id",
        entityColumn = "planId"
    )
    val items: List<com.lumiroom.core.database.relation.FloorPlanItemWithFurniture>,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val doors: List<DoorEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val windows: List<WindowEntity>
)
