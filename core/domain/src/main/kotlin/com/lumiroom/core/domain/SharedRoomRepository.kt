package com.lumiroom.core.domain

import com.lumiroom.core.database.dao.RoomPlanDao
import com.lumiroom.core.database.entity.*
import com.lumiroom.core.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedRoomRepository @Inject constructor(
    private val roomPlanDao: RoomPlanDao
) {
    /**
     * Observes the single source of truth for the room.
     */
    fun observeRoomState(planId: String): Flow<SharedRoomState?> {
        return roomPlanDao.getRoomPlanWithDetails(planId).map { entity ->
            entity?.let { mapToDomain(it) }
        }
    }

    private fun mapToDomain(entity: RoomPlanWithDetails): SharedRoomState {
        return SharedRoomState(
            planId = entity.roomPlan.id,
            name = entity.roomPlan.name,
            roomAnchorId = entity.roomPlan.roomAnchorId,
            gridSizeCm = entity.roomPlan.gridSizeCm,
            walls = entity.walls.map {
                RoomWall(
                    id = it.id,
                    startX = it.startX,
                    startY = it.startY,
                    endX = it.endX,
                    endY = it.endY,
                    thicknessCm = it.thicknessCm,
                    heightCm = 250f // Default or load from entity
                )
            },
            doors = entity.doors.map {
                RoomDoor(
                    id = it.id,
                    startX = it.startX,
                    startY = it.startY,
                    endX = it.endX,
                    endY = it.endY,
                    thicknessCm = it.thicknessCm,
                    heightCm = 200f
                )
            },
            windows = entity.windows.map {
                RoomWindow(
                    id = it.id,
                    startX = it.startX,
                    startY = it.startY,
                    endX = it.endX,
                    endY = it.endY,
                    thicknessCm = it.thicknessCm,
                    heightCm = 120f,
                    elevationCm = 80f
                )
            },
            furniture = entity.items.map {
                RoomFurniture(
                    id = it.item.id,
                    furnitureId = it.item.furnitureId,
                    positionX = it.item.posX,
                    positionY = it.item.posY,
                    positionZ = it.item.posZ,
                    rotation = it.item.rotation,
                    scaleX = it.item.scaleX,
                    scaleY = it.item.scaleY,
                    scaleZ = it.item.scaleZ,
                    widthCm = (it.furniture.width ?: 1.0f) * 100f,
                    depthCm = (it.furniture.depth ?: 1.0f) * 100f,
                    heightCm = (it.furniture.height ?: 1.0f) * 100f,
                    colorHex = 0xFF8B4513,
                    zIndex = 0
                )
            }
        )
    }

    suspend fun updateRoomAnchor(planId: String, anchorId: String?) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(planId)
        val plan = planFlow.firstOrNull()?.roomPlan
        if (plan != null) {
            roomPlanDao.updateRoomPlan(plan.copy(roomAnchorId = anchorId))
        }
    }

    suspend fun addFurniture(planId: String, floorPlanItem: FloorPlanItemEntity) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(planId)
        val planWithDetails = planFlow.firstOrNull()
        if (planWithDetails != null) {
            val itemsEntityList = planWithDetails.items.map { it.item } + floorPlanItem
            roomPlanDao.saveFullPlan(
                planWithDetails.roomPlan,
                planWithDetails.walls,
                itemsEntityList,
                planWithDetails.doors,
                planWithDetails.windows
            )
        }
    }

    suspend fun removeFurniture(planId: String, itemId: String) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(planId)
        val planWithDetails = planFlow.firstOrNull()
        if (planWithDetails != null) {
            val itemsEntityList = planWithDetails.items.map { it.item }.filter { it.id != itemId }
            roomPlanDao.saveFullPlan(
                planWithDetails.roomPlan,
                planWithDetails.walls,
                itemsEntityList,
                planWithDetails.doors,
                planWithDetails.windows
            )
        }
    }
    
    suspend fun updateFurniture(planId: String, floorPlanItem: FloorPlanItemEntity) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(planId)
        val planWithDetails = planFlow.firstOrNull()
        if (planWithDetails != null) {
            val itemsEntityList = planWithDetails.items.map { it.item }.map { if (it.id == floorPlanItem.id) floorPlanItem else it }
            roomPlanDao.saveFullPlan(
                planWithDetails.roomPlan,
                planWithDetails.walls,
                itemsEntityList,
                planWithDetails.doors,
                planWithDetails.windows
            )
        }
    }
}
