package com.lumiroom.core.domain.model

data class SharedRoomState(
    val planId: String,
    val name: String,
    val walls: List<RoomWall>,
    val doors: List<RoomDoor>,
    val windows: List<RoomWindow>,
    val furniture: List<RoomFurniture>,
    val roomAnchorId: String? = null,
    val gridSizeCm: Int = 10
)

data class RoomWall(
    val id: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val thicknessCm: Float = 10f,
    val heightCm: Float = 250f
)

data class RoomDoor(
    val id: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val thicknessCm: Float = 10f,
    val heightCm: Float = 200f
)

data class RoomWindow(
    val id: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val thicknessCm: Float = 10f,
    val heightCm: Float = 120f,
    val elevationCm: Float = 80f
)

data class RoomFurniture(
    val id: String,
    val furnitureId: String,
    val positionX: Float,
    val positionY: Float,
    val positionZ: Float = 0f,
    val rotation: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float = 1f,
    val widthCm: Float = 100f,
    val depthCm: Float = 100f,
    val heightCm: Float = 100f,
    val colorHex: Long = 0xFF8B4513,
    val zIndex: Int = 0
)
