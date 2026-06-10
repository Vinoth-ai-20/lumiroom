package com.lumiroom.core.recommendation

import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.room_analysis.RoomStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleCompatibilityScorer @Inject constructor() {
    
    fun getStyleScore(item: FurnitureEntity, targetStyle: RoomStyle): Float {
        // Mock simple scoring based on style keywords in the description or category
        val styleTag = targetStyle.name.lowercase()
        val desc = item.description.lowercase()
        val cat = item.category.lowercase()
        val name = item.name.lowercase()

        var score = 0.5f // Baseline

        if (desc.contains(styleTag) || cat.contains(styleTag) || name.contains(styleTag)) {
            score += 0.4f
        }

        // Penalty for conflicting styles
        if (targetStyle == RoomStyle.MINIMALIST && desc.contains("traditional")) score -= 0.3f
        if (targetStyle == RoomStyle.TRADITIONAL && desc.contains("modern")) score -= 0.3f

        return score.coerceIn(0.0f, 1.0f)
    }
}
