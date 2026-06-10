package com.lumiroom.feature.ar.domain

import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.entity.RoomDesignEntity
import com.lumiroom.core.database.relation.PlacedItemWithFurniture
import com.lumiroom.core.database.dao.FurnitureDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/**
 * Places a furniture item in the AR scene and persists it to Room DB.
 *
 * Responsibilities:
 * 1. Creates/retrieves the active [RoomDesignEntity] for this session.
 * 2. Builds a [PlacedItemEntity] with the provided AR world-space transforms.
 * 3. Inserts the entity into the local database.
 * 4. Returns the [PlacedItemWithFurniture] for immediate scene rendering.
 */
class PlaceFurnitureUseCase @Inject constructor(
    private val placedItemDao: PlacedItemDao,
    private val roomDesignDao: RoomDesignDao,
    private val furnitureDao: FurnitureDao,
) {
    suspend operator fun invoke(
        instanceId: String,
        furnitureId: String,
        roomDesignId: String?,
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float,
        scaleX: Float = 1f, scaleY: Float = 1f, scaleZ: Float = 1f,
        selectedColor: String? = null,
    ): LumiroomResult<PlacedItemWithFurniture> {
        return try {
            val furniture = furnitureDao.getFurnitureByIdOnce(furnitureId)
                ?: return LumiroomResult.Error(IllegalArgumentException("Furniture $furnitureId not found"))

            var finalRoomId = roomDesignId
            if (finalRoomId == null) {
                finalRoomId = UUID.randomUUID().toString()
                roomDesignDao.insert(
                    RoomDesignEntity(
                        id = finalRoomId,
                        userId = "local_user",
                        name = "Untitled Room",
                        description = null,
                        roomType = "Unknown",
                        widthM = null,
                        lengthM = null,
                        heightM = null,
                        arEnvironmentId = null,
                        thumbnailPath = null,
                        firestoreDocId = null,
                        styleTag = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Build the 4×4 identity-derived matrix (simplified; real impl uses Pose.getMatrix())
            val matrix = buildTransformMatrix(posX, posY, posZ, rotX, rotY, rotZ, rotW, scaleX, scaleY, scaleZ)

            val item = PlacedItemEntity(
                id             = instanceId,
                roomDesignId   = finalRoomId,
                furnitureId    = furnitureId,
                transformMatrix = Json.encodeToString(matrix),
                posX = posX, posY = posY, posZ = posZ,
                rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
                scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ,
                initPosX = posX, initPosY = posY, initPosZ = posZ,
                initRotX = rotX, initRotY = rotY, initRotZ = rotZ, initRotW = rotW,
                initScaleX = scaleX, initScaleY = scaleY, initScaleZ = scaleZ,
                selectedColor  = selectedColor,
                label          = null,
            )

            placedItemDao.insert(item)

            LumiroomResult.Success(
                PlacedItemWithFurniture(placedItem = item, furniture = furniture),
            )
        } catch (e: Exception) {
            android.util.Log.e("ArPlacement", "Database error", e)
            LumiroomResult.Error(e)
        }
    }

    /** Builds a column-major 4×4 transform float array from decomposed TRS. */
    private fun buildTransformMatrix(
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float,
        scaleX: Float, scaleY: Float, scaleZ: Float,
    ): List<Float> {
        // Quaternion to rotation matrix
        val x2 = rotX * rotX; val y2 = rotY * rotY; val z2 = rotZ * rotZ
        val xy = rotX * rotY; val xz = rotX * rotZ; val yz = rotY * rotZ
        val wx = rotW * rotX; val wy = rotW * rotY; val wz = rotW * rotZ

        return listOf(
            scaleX * (1f - 2f*(y2+z2)),  scaleX * 2f*(xy+wz),         scaleX * 2f*(xz-wy),         0f,
            scaleY * 2f*(xy-wz),          scaleY * (1f - 2f*(x2+z2)),  scaleY * 2f*(yz+wx),         0f,
            scaleZ * 2f*(xz+wy),          scaleZ * 2f*(yz-wx),          scaleZ * (1f - 2f*(x2+y2)), 0f,
            posX,                          posY,                          posZ,                        1f,
        )
    }
}
