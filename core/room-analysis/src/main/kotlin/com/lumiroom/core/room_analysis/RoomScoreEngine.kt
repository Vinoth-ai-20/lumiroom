package com.lumiroom.core.room_analysis

import com.lumiroom.core.database.relation.PlacedItemWithFurniture
import javax.inject.Inject
import javax.inject.Singleton

enum class RoomHealthCategory {
    POOR, FAIR, GOOD, EXCELLENT
}

data class RoomHealthResult(
    val score: Int, // 0 - 100
    val category: RoomHealthCategory,
    val layoutEfficiency: Int,
    val spaceUtilization: Int,
    val furnitureBalance: Int
)

@Singleton
class RoomScoreEngine @Inject constructor() {

    fun calculateScore(items: List<PlacedItemWithFurniture>): RoomHealthResult {
        if (items.isEmpty()) {
            return RoomHealthResult(0, RoomHealthCategory.POOR, 0, 0, 0)
        }

        // Extremely simplified heuristics for score generation based on local data
        // 1. Layout Efficiency: Ideal amount of furniture is ~3-8 items for a typical room snippet.
        val itemSize = items.size
        val layoutEfficiency = when {
            itemSize < 2 -> 50
            itemSize in 2..6 -> 90
            itemSize in 7..10 -> 80
            else -> 60
        }

        // 2. Space Utilization: Check bounding box spreads (mocked for now since we don't have walls)
        // If items are too tightly packed (all near origin), penalty.
        var spaceUtil = 75
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        items.forEach { 
            if (it.placedItem.posX < minX) minX = it.placedItem.posX
            if (it.placedItem.posX > maxX) maxX = it.placedItem.posX
            if (it.placedItem.posZ < minZ) minZ = it.placedItem.posZ
            if (it.placedItem.posZ > maxZ) maxZ = it.placedItem.posZ
        }
        
        val areaSpread = (maxX - minX) * (maxZ - minZ)
        if (itemSize > 1) {
            if (areaSpread < 1.0f) spaceUtil = 40 // Too cramped
            else if (areaSpread > 10.0f) spaceUtil = 60 // Too spread out
            else spaceUtil = 90
        }

        // 3. Furniture Balance: Ratio of big items (sofa, bed) to small items (decor, lamp)
        val bigItems = items.count { it.furniture.category.lowercase() in listOf("sofa", "bed", "cabinet", "table") }
        val smallItems = items.size - bigItems
        val balance = if (bigItems > 0 && smallItems > 0) {
            val ratio = smallItems.toFloat() / bigItems.toFloat()
            if (ratio in 0.5f..2.0f) 95 else 70
        } else {
            50
        }

        val totalScore = ((layoutEfficiency + spaceUtil + balance) / 3f).toInt().coerceIn(0, 100)

        val cat = when {
            totalScore >= 85 -> RoomHealthCategory.EXCELLENT
            totalScore >= 70 -> RoomHealthCategory.GOOD
            totalScore >= 50 -> RoomHealthCategory.FAIR
            else -> RoomHealthCategory.POOR
        }

        return RoomHealthResult(totalScore, cat, layoutEfficiency, spaceUtil, balance)
    }
}
