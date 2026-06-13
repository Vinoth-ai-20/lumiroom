package com.lumiroom.core.database.model

import com.lumiroom.core.database.entity.FurnitureEntity

fun FurnitureEntity.toDomain(): Furniture {
    return Furniture(
        id = id,
        name = name,
        category = category,
        roomType = roomType,
        description = description,
        width = width,
        depth = depth,
        height = height,
        priceEstimate = priceEstimate,
        modelPath = modelPath,
        thumbnailPath = thumbnailPath,
        style = style,
        isDownloaded = isDownloaded,
        isFavorite = isFavorite,
    )
}

fun Furniture.toEntity(): FurnitureEntity {
    return FurnitureEntity(
        id = id,
        name = name,
        category = category,
        roomType = roomType,
        description = description,
        width = width,
        depth = depth,
        height = height,
        priceEstimate = priceEstimate,
        modelPath = modelPath,
        thumbnailPath = thumbnailPath,
        style = style,
        isDownloaded = isDownloaded,
        isFavorite = isFavorite,
    )
}
