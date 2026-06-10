package com.lumiroom.core.room_analysis

import javax.inject.Inject
import javax.inject.Singleton

data class BudgetPlan(
    val totalBudget: Double,
    val currentCost: Double,
    val utilizationPercentage: Float,
    val remainingBudget: Double
)

@Singleton
class BudgetPlanner @Inject constructor(
    private val costEstimator: CostEstimator
) {
    
    fun calculateBudgetPlan(
        furnitureList: List<com.lumiroom.core.database.entity.FurnitureEntity>,
        targetBudget: Double
    ): BudgetPlan {
        val cost = costEstimator.estimateCost(furnitureList).totalEstimatedCost
        val util = if (targetBudget > 0) (cost / targetBudget).toFloat() else 0f
        
        return BudgetPlan(
            totalBudget = targetBudget,
            currentCost = cost,
            utilizationPercentage = util.coerceIn(0f, 1f),
            remainingBudget = maxOf(0.0, targetBudget - cost)
        )
    }
}
