package com.lumiroom.core.recommendation

import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.room_analysis.RoomStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val styleScorer: StyleCompatibilityScorer,
    private val compatibilityEngine: FurnitureCompatibilityEngine
) {

    fun getGeneralRecommendations(
        catalog: List<FurnitureEntity>,
        targetStyle: RoomStyle,
        existingItems: List<FurnitureEntity>,
        limit: Int = 10
    ): List<FurnitureEntity> {
        // Filter out items we already have
        val existingIds = existingItems.map { it.id }.toSet()
        val candidates = catalog.filter { it.id !in existingIds }

        // Score them by style
        val scoredCandidates = candidates.map { item ->
            Pair(item, styleScorer.getStyleScore(item, targetStyle))
        }

        // Sort by score descending
        val sortedByStyle = scoredCandidates.sortedByDescending { it.second }.map { it.first }
        
        // Take top style matches
        val styleMatches = sortedByStyle.take(limit)
        
        // Also add complements for any existing items
        val complements = mutableListOf<FurnitureEntity>()
        existingItems.forEach { baseItem ->
            complements.addAll(compatibilityEngine.findComplements(baseItem, catalog))
        }

        // Merge and return unique items
        return (styleMatches + complements)
            .distinctBy { it.id }
            .take(limit)
    }
}
