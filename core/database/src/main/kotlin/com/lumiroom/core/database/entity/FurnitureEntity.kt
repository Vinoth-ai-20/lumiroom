package com.lumiroom.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "furniture",
    indices = [
        Index("category"),
        Index("style"),
        Index("is_favorite"),
        Index("is_downloaded"),
    ],
)
data class FurnitureEntity(
    @PrimaryKey
    val id: String,

    val name: String,
    val category: String,
    val description: String,

    val width: Float,
    val depth: Float,
    val height: Float,

    @ColumnInfo(name = "price_estimate") val priceEstimate: Double?,

    @ColumnInfo(name = "model_path") val modelPath: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String?,

    val style: String?,

    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
