package com.lumiroom.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.lumiroom.core.database.entity.FloorPlanItemEntity
import com.lumiroom.core.database.entity.FurnitureEntity

data class FloorPlanItemWithFurniture(
    @Embedded val item: FloorPlanItemEntity,
    
    @Relation(
        parentColumn = "furnitureId",
        entityColumn = "id"
    )
    val furniture: FurnitureEntity
)
