package com.lumiroom.core.database.model

data class Furniture(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val width: Float,
    val depth: Float,
    val height: Float,
    val priceEstimate: Double?,
    val modelPath: String,
    val thumbnailPath: String?,
    val style: String?,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
)
