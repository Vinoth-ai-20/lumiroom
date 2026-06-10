package com.lumiroom.core.recommendation

import com.lumiroom.core.database.entity.FurnitureEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FurnitureCompatibilityEngine @Inject constructor() {

    fun findComplements(baseItem: FurnitureEntity, catalog: List<FurnitureEntity>): List<FurnitureEntity> {
        val complements = mutableListOf<FurnitureEntity>()
        val baseCategory = baseItem.category.lowercase()
        
        // Highly simplified complement logic
        val targetCategories = when (baseCategory) {
            "sofa" -> listOf("coffee table", "rug", "lamp", "side table")
            "bed" -> listOf("nightstand", "wardrobe", "rug", "lamp")
            "table" -> listOf("chair", "rug", "lighting")
            "desk" -> listOf("office chair", "lamp")
            else -> emptyList()
        }

        for (item in catalog) {
            if (item.id == baseItem.id) continue
            val itemCat = item.category.lowercase()
            
            // Check if it's a target category
            val isTarget = targetCategories.any { itemCat.contains(it) }

            if (isTarget) {
                complements.add(item)
            }
        }

        return complements.distinctBy { it.id }.take(5)
    }
}
