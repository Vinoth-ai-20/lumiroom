package com.lumiroom.core.network.model

import com.google.firebase.firestore.DocumentId

data class CloudRoomDesign(
    @DocumentId val roomId: String = "",
    val userId: String = "",
    val roomName: String = "",
    val description: String? = null,
    val roomType: String = "",
    val widthM: Float? = null,
    val lengthM: Float? = null,
    val heightM: Float? = null,
    val arEnvironmentId: String? = null,
    val thumbnailUrl: String? = null,
    val isArchived: Boolean = false,
    val styleTag: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val layoutData: List<CloudPlacedItem> = emptyList()
)

data class CloudPlacedItem(
    val id: String = "",
    val furnitureId: String = "",
    val transformMatrix: String = "",
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val rotW: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val selectedColor: String? = null,
    val label: String? = null,
    val createdAt: Long = 0
)
