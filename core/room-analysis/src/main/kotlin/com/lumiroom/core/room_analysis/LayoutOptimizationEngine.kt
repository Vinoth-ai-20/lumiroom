package com.lumiroom.core.room_analysis

import com.lumiroom.core.database.relation.PlacedItemWithFurniture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayoutOptimizationEngine @Inject constructor() {

    fun analyzeLayout(items: List<PlacedItemWithFurniture>): List<String> {
        val suggestions = mutableListOf<String>()

        if (items.isEmpty()) {
            suggestions.add("Add a central piece of furniture like a sofa or bed to anchor the room.")
            return suggestions
        }

        val categories = items.map { it.furniture.category.lowercase() }
        
        if ("sofa" in categories && "table" !in categories && "coffee table" !in categories) {
            suggestions.add("Add a coffee table or side table near your sofa for convenience.")
        }
        
        if ("bed" in categories && "table" !in categories && "nightstand" !in categories) {
            suggestions.add("Consider placing a nightstand next to the bed.")
        }
        
        if ("lamp" !in categories && "lighting" !in categories) {
            suggestions.add("Improve ambiance by adding a floor lamp or table lamp.")
        }
        
        if (items.size > 8) {
            suggestions.add("Improve walking space: The room might be a bit cluttered. Consider removing an item.")
        }

        // Distance checks (naive)
        if (items.size > 1) {
            var tooCloseCount = 0
            for (i in 0 until items.size - 1) {
                for (j in i + 1 until items.size) {
                    val p1 = items[i].placedItem
                    val p2 = items[j].placedItem
                    val dx = p1.posX - p2.posX
                    val dz = p1.posZ - p2.posZ
                    val distSq = dx*dx + dz*dz
                    if (distSq < 0.25f) { // Less than 0.5m apart
                        tooCloseCount++
                    }
                }
            }
            if (tooCloseCount > 0) {
                suggestions.add("Balance furniture spacing: Some items are placed very close to each other.")
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Your layout looks great and well-balanced!")
        }

        return suggestions
    }
}
