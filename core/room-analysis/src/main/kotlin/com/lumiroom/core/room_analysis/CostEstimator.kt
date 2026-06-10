package com.lumiroom.core.room_analysis

import com.lumiroom.core.database.entity.FurnitureEntity
import javax.inject.Inject
import javax.inject.Singleton

data class CostEstimate(
    val totalEstimatedCost: Double,
    val categoryBreakdown: Map<String, Double>
)

@Singleton
class CostEstimator @Inject constructor() {
    
    fun estimateCost(furnitureList: List<FurnitureEntity>): CostEstimate {
        var total = 0.0
        val breakdown = mutableMapOf<String, Double>()

        for (item in furnitureList) {
            val price = item.priceEstimate ?: 0.0
            total += price
            breakdown[item.category] = breakdown.getOrDefault(item.category, 0.0) + price
        }

        return CostEstimate(total, breakdown)
    }
}
