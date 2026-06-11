package com.lumiroom.core.domain

import com.lumiroom.core.database.dao.RoomPlanDao
import com.lumiroom.core.database.entity.*
import com.lumiroom.core.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedRoomRepository @Inject constructor(
    private val roomPlanDao: RoomPlanDao
) {
    suspend fun getRoomState(planId: String): RoomModel? {
        val entity = roomPlanDao.getRoomPlanWithDetails(planId).firstOrNull()
        return entity?.let { mapToDomain(it) }
    }
    
    fun observeRoomState(planId: String): Flow<RoomModel?> {
        return roomPlanDao.getRoomPlanWithDetails(planId).map { entity ->
            entity?.let { mapToDomain(it) }
        }
    }
    
    suspend fun saveRoomState(model: RoomModel) {
        val plan = RoomPlanEntity(
            id = model.planId,
            name = model.name,
            updatedAt = System.currentTimeMillis(),
            roomAnchorId = model.roomAnchorId,
            gridSizeCm = model.gridSizeCm,
            cameraPanX = model.cameraState.panX,
            cameraPanY = model.cameraState.panY,
            cameraZoom = model.cameraState.zoom,
            anchorsJson = JSONArray().apply {
                model.anchors.forEach { anchor ->
                    put(JSONObject().apply {
                        put("id", anchor.id)
                        put("x", anchor.pose.x.toDouble())
                        put("y", anchor.pose.y.toDouble())
                        put("z", anchor.pose.z.toDouble())
                        put("qx", anchor.pose.qx.toDouble())
                        put("qy", anchor.pose.qy.toDouble())
                        put("qz", anchor.pose.qz.toDouble())
                        put("qw", anchor.pose.qw.toDouble())
                    })
                }
            }.toString(),
            planeBoundariesJson = JSONArray().apply {
                model.planeBoundaries.forEach { plane ->
                    put(JSONObject().apply {
                        put("id", plane.id)
                        put("cx", plane.centerPose.x.toDouble())
                        put("cy", plane.centerPose.y.toDouble())
                        put("cz", plane.centerPose.z.toDouble())
                        put("cqx", plane.centerPose.qx.toDouble())
                        put("cqy", plane.centerPose.qy.toDouble())
                        put("cqz", plane.centerPose.qz.toDouble())
                        put("cqw", plane.centerPose.qw.toDouble())
                        put("extentX", plane.extentX.toDouble())
                        put("extentZ", plane.extentZ.toDouble())
                        put("polygon", JSONArray().apply {
                            plane.polygon.forEach { p ->
                                put(JSONObject().apply {
                                    put("x", p.x.toDouble())
                                    put("y", p.y.toDouble())
                                })
                            }
                        })
                    })
                }
            }.toString()
        )
        
        val wallEntities = model.walls.map { w ->
            WallEntity(id = w.id, planId = model.planId, startX = w.startX, startY = w.startY, endX = w.endX, endY = w.endY, thicknessCm = w.thicknessCm)
        }
        val doorEntities = model.doors.map { d ->
            DoorEntity(id = d.id, planId = model.planId, startX = d.startX, startY = d.startY, endX = d.endX, endY = d.endY, thicknessCm = d.thicknessCm, swingAngle = d.swingAngle)
        }
        val windowEntities = model.windows.map { w ->
            WindowEntity(id = w.id, planId = model.planId, startX = w.startX, startY = w.startY, endX = w.endX, endY = w.endY, thicknessCm = w.thicknessCm)
        }
        val itemEntities = model.furniture.map { f ->
            FloorPlanItemEntity(id = f.id, planId = model.planId, furnitureId = f.furnitureId, posX = f.positionX, posY = f.positionY, posZ = f.positionZ, rotation = f.rotation, scaleX = f.scaleX, scaleY = f.scaleY, scaleZ = f.scaleZ)
        }
        
        roomPlanDao.saveFullPlan(plan, wallEntities, itemEntities, doorEntities, windowEntities)
    }

    private fun mapToDomain(entity: RoomPlanWithDetails): RoomModel {
        return RoomModel(
            planId = entity.roomPlan.id,
            name = entity.roomPlan.name,
            roomAnchorId = entity.roomPlan.roomAnchorId,
            gridSizeCm = entity.roomPlan.gridSizeCm,
            cameraState = CameraState(
                panX = entity.roomPlan.cameraPanX,
                panY = entity.roomPlan.cameraPanY,
                zoom = entity.roomPlan.cameraZoom
            ),
            anchors = try {
                val array = JSONArray(entity.roomPlan.anchorsJson ?: "[]")
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    AnchorData(
                        id = obj.getString("id"),
                        pose = PoseData(
                            obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat(), obj.getDouble("z").toFloat(),
                            obj.getDouble("qx").toFloat(), obj.getDouble("qy").toFloat(), obj.getDouble("qz").toFloat(), obj.getDouble("qw").toFloat()
                        )
                    )
                }
            } catch (e: Exception) { emptyList() },
            planeBoundaries = try {
                val array = JSONArray(entity.roomPlan.planeBoundariesJson ?: "[]")
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val polyArray = obj.getJSONArray("polygon")
                    PlaneBoundary(
                        id = obj.getString("id"),
                        centerPose = PoseData(
                            obj.getDouble("cx").toFloat(), obj.getDouble("cy").toFloat(), obj.getDouble("cz").toFloat(),
                            obj.getDouble("cqx").toFloat(), obj.getDouble("cqy").toFloat(), obj.getDouble("cqz").toFloat(), obj.getDouble("cqw").toFloat()
                        ),
                        extentX = obj.getDouble("extentX").toFloat(),
                        extentZ = obj.getDouble("extentZ").toFloat(),
                        polygon = (0 until polyArray.length()).map { j ->
                            val pObj = polyArray.getJSONObject(j)
                            Point2DData(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat())
                        }
                    )
                }
            } catch (e: Exception) { emptyList() },
            walls = entity.walls.map {
                RoomWall(
                    id = it.id,
                    startX = it.startX,
                    startY = it.startY,
                    endX = it.endX,
                    endY = it.endY,
                    thicknessCm = it.thicknessCm,
                    heightCm = 250f
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
                    heightCm = 200f,
                    swingAngle = it.swingAngle
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
                    zIndex = 0,
                    name = it.furniture.name,
                    category = it.furniture.category,
                    modelPath = it.furniture.modelPath,
                    thumbnailPath = it.furniture.thumbnailPath,
                    priceEstimate = it.furniture.priceEstimate
                )
            }
        )
    }

    fun getAllRooms(): Flow<List<RoomPlanEntity>> {
        return roomPlanDao.getAllRoomPlans()
    }

    suspend fun createRoom(name: String, roomType: String): String {
        val newPlan = RoomPlanEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            updatedAt = System.currentTimeMillis()
        )
        roomPlanDao.insertRoomPlan(newPlan)
        return newPlan.id
    }

    suspend fun renameRoom(roomId: String, newName: String) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(roomId)
        val plan = planFlow.firstOrNull()?.roomPlan
        if (plan != null) {
            roomPlanDao.updateRoomPlan(plan.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun duplicateRoom(roomId: String) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(roomId)
        val planWithDetails = planFlow.firstOrNull()
        if (planWithDetails != null) {
            val newPlanId = java.util.UUID.randomUUID().toString()
            val newPlan = planWithDetails.roomPlan.copy(
                id = newPlanId,
                name = "${planWithDetails.roomPlan.name} (Copy)",
                updatedAt = System.currentTimeMillis()
            )
            
            val newWalls = planWithDetails.walls.map { it.copy(id = java.util.UUID.randomUUID().toString(), planId = newPlanId) }
            val newDoors = planWithDetails.doors.map { it.copy(id = java.util.UUID.randomUUID().toString(), planId = newPlanId) }
            val newWindows = planWithDetails.windows.map { it.copy(id = java.util.UUID.randomUUID().toString(), planId = newPlanId) }
            val newItems = planWithDetails.items.map { it.item.copy(id = java.util.UUID.randomUUID().toString(), planId = newPlanId) }
            
            roomPlanDao.saveFullPlan(newPlan, newWalls, newItems, newDoors, newWindows)
        }
    }

    suspend fun deleteRoom(roomId: String) {
        val planFlow = roomPlanDao.getRoomPlanWithDetails(roomId)
        val plan = planFlow.firstOrNull()?.roomPlan
        if (plan != null) {
            roomPlanDao.deleteRoomPlan(plan.id)
        }
    }
}
