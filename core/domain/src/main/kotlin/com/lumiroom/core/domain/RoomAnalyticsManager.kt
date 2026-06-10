package com.lumiroom.core.domain

import com.lumiroom.core.database.entity.FurnitureEntity
import javax.inject.Inject
import javax.inject.Singleton

data class RoomAnalytics(
    val totalItems: Int,
    val estimatedTotalCost: Double,
    val categoryBreakdown: Map<String, Int>
)

@Singleton
class RoomAnalyticsManager @Inject constructor() {

    fun calculateAnalytics(furnitureList: List<FurnitureEntity>): RoomAnalytics {
        val count = furnitureList.size
        var cost = 0.0
        val categories = mutableMapOf<String, Int>()

        for (item in furnitureList) {
            cost += item.priceEstimate ?: 0.0
            categories[item.category] = categories.getOrDefault(item.category, 0) + 1
        }

        return RoomAnalytics(
            totalItems = count,
            estimatedTotalCost = cost,
            categoryBreakdown = categories
        )
    }
}
