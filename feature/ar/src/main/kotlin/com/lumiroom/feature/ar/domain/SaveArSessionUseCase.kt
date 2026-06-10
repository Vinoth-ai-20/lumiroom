package com.lumiroom.feature.ar.domain

import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.entity.RoomDesignEntity
import java.util.UUID
import javax.inject.Inject

class SaveArSessionUseCase @Inject constructor(
    private val roomDesignDao: RoomDesignDao,
    private val placedItemDao: PlacedItemDao,
) {
    suspend operator fun invoke(
        name: String,
        items: List<PlacedItemEntity>,
        existingRoomId: String? = null,
    ): LumiroomResult<String> {
        return try {
            val roomId = existingRoomId ?: UUID.randomUUID().toString()
            val room = RoomDesignEntity(
                id         = roomId,
                userId     = "local", // TODO: replace with authenticated user ID in Milestone 1
                name       = name,
                description = null,
                roomType   = "Living Room",
                widthM     = null, lengthM = null, heightM = null,
                arEnvironmentId = null,
                thumbnailPath   = null,
                firestoreDocId  = null,
                syncStatus      = "local",
                isArchived      = false,
                styleTag        = null,
            )
            roomDesignDao.insert(room)

            // Update all items to reference this room
            val updatedItems = items.map { it.copy(roomDesignId = roomId) }
            placedItemDao.insertAll(updatedItems)

            LumiroomResult.Success(roomId)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }
}
