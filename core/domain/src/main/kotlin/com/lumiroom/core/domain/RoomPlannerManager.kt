package com.lumiroom.core.domain

import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.entity.RoomDesignEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPlannerManager @Inject constructor(
    private val roomDesignDao: RoomDesignDao,
    private val placedItemDao: PlacedItemDao
) {
    fun getAllRooms(userId: String = "local_user"): Flow<List<RoomDesignEntity>> {
        return roomDesignDao.getUserRooms(userId)
    }

    suspend fun createRoom(name: String, roomType: String): String {
        val id = UUID.randomUUID().toString()
        val room = RoomDesignEntity(
            id = id,
            userId = "local_user",
            name = name,
            description = null,
            roomType = roomType,
            widthM = null,
            lengthM = null,
            heightM = null,
            arEnvironmentId = null,
            thumbnailPath = null,
            firestoreDocId = null,
            styleTag = null
        )
        roomDesignDao.insert(room)
        return id
    }

    suspend fun renameRoom(roomId: String, newName: String) {
        val room = roomDesignDao.getRoomWithItemsOnce(roomId)?.roomDesign
        if (room != null) {
            roomDesignDao.update(room.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun duplicateRoom(roomId: String): String? {
        val roomWithItems = roomDesignDao.getRoomWithItemsOnce(roomId) ?: return null
        val room = roomWithItems.roomDesign
        val newId = UUID.randomUUID().toString()
        val duplicatedRoom = room.copy(
            id = newId,
            name = "${room.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        roomDesignDao.insert(duplicatedRoom)
        
        val duplicatedItems = roomWithItems.placedItems.map { it.copy(
            id = UUID.randomUUID().toString(),
            roomDesignId = newId,
            createdAt = System.currentTimeMillis()
        )}
        if (duplicatedItems.isNotEmpty()) {
            placedItemDao.insertAll(duplicatedItems)
        }
        return newId
    }

    suspend fun deleteRoom(roomId: String) {
        roomDesignDao.getRoomWithItemsOnce(roomId)?.roomDesign?.let {
            roomDesignDao.delete(it)
        }
    }
}
