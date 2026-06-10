package com.lumiroom.core.room_analysis

import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.relation.PlacedItemWithFurniture
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomScoreEngineTest {

    private lateinit engine: RoomScoreEngine

    @Before
    fun setup() {
        engine = RoomScoreEngine()
    }

    @Test
    fun `calculateScore returns empty result for empty list`() {
        val result = engine.calculateScore(emptyList())
        assertEquals(0, result.score)
        assertEquals(RoomHealthCategory.NEEDS_WORK, result.category)
    }

    @Test
    fun `calculateScore returns valid score for single item`() {
        val furniture = FurnitureEntity("f1", "Table", "table", "desc", 1f, 1f, 1f, 100.0, "", null, null)
        val placedItem = PlacedItemEntity("p1", "r1", "f1", "matrix", 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, null, null)
        val itemWithFurniture = PlacedItemWithFurniture(placedItem, furniture)

        val result = engine.calculateScore(listOf(itemWithFurniture))
        
        // With 1 item, bounds are 0, so spread is 0, score might be low but calculable
        // Layout efficiency should be 50, balance 100, utilization 0 -> average ~50
        assert(result.score > 0)
    }

    @Test
    fun `calculateScore calculates layout efficiency penalty for overlapping items`() {
        val furniture1 = FurnitureEntity("f1", "Table", "table", "desc", 2f, 2f, 1f, 100.0, "", null, null)
        val furniture2 = FurnitureEntity("f2", "Chair", "chair", "desc", 1f, 1f, 1f, 50.0, "", null, null)
        
        // Both items exactly at the origin (overlapping)
        val placedItem1 = PlacedItemEntity("p1", "r1", "f1", "matrix", 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, null, null)
        val placedItem2 = PlacedItemEntity("p2", "r1", "f2", "matrix", 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, null, null)

        val items = listOf(
            PlacedItemWithFurniture(placedItem1, furniture1),
            PlacedItemWithFurniture(placedItem2, furniture2)
        )

        val result = engine.calculateScore(items)
        
        // Since they overlap, layoutEfficiency should be heavily penalized (0)
        assertEquals(0, result.layoutEfficiency)
    }
}
