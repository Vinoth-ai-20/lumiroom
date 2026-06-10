package com.lumiroom.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.entity.RoomDesignEntity

/**
 * Room relationship: a [RoomDesignEntity] with all its [PlacedItemEntity] children.
 *
 * Queried with a single JOIN via Room's [@Relation] annotation — no manual join needed.
 *
 * Usage in DAO:
 * ```kotlin
 * @Transaction
 * @Query("SELECT * FROM room_design WHERE id = :roomId")
 * fun getRoomWithItems(roomId: String): Flow<RoomDesignWithItems?>
 * ```
 */
data class RoomDesignWithItems(
    @Embedded
    val roomDesign: RoomDesignEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "room_design_id",
    )
    val placedItems: List<PlacedItemEntity>,
)

/**
 * A [PlacedItemEntity] with the associated [FurnitureEntity] resolved.
 * Used when rendering the AR scene to avoid repeated lookups.
 */
data class PlacedItemWithFurniture(
    @Embedded
    val placedItem: PlacedItemEntity,

    @Relation(
        parentColumn = "furniture_id",
        entityColumn = "id",
    )
    val furniture: FurnitureEntity,
)
