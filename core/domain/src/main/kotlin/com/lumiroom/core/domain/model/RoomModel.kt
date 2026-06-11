package com.lumiroom.core.domain.model

data class RoomModel(
    val planId: String,
    val name: String,
    val walls: List<RoomWall>,
    val doors: List<RoomDoor>,
    val windows: List<RoomWindow>,
    val furniture: List<RoomFurniture>,
    val planeBoundaries: List<PlaneBoundary> = emptyList(),
    val anchors: List<AnchorData> = emptyList(),
    val cameraState: CameraState = CameraState(),
    val selectionState: SelectionState = SelectionState(),
    val roomAnchorId: String? = null,
    val gridSizeCm: Int = 10
)

data class PlaneBoundary(
    val id: String,
    val centerPose: PoseData,
    val extentX: Float,
    val extentZ: Float,
    val polygon: List<Point2DData>
)

data class PoseData(
    val x: Float, val y: Float, val z: Float,
    val qx: Float, val qy: Float, val qz: Float, val qw: Float
)

data class AnchorData(
    val id: String,
    val pose: PoseData
)

data class CameraState(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1f
)

data class SelectionState(
    val selectedItemIds: Set<String> = emptySet(),
    val lockedItemIds: Set<String> = emptySet(),
    val hiddenItemIds: Set<String> = emptySet()
)

data class Point2DData(val x: Float, val y: Float)

// Re-using existing structs from SharedRoomState
// I will keep them here for the unified RoomModel

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
    val heightCm: Float = 200f,
    val swingAngle: Float = 90f
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
    val zIndex: Int = 0,
    // Metadata from FurnitureEntity
    val name: String = "",
    val category: String = "",
    val modelPath: String = "",
    val thumbnailPath: String? = null,
    val priceEstimate: Double? = null
)
