package com.lumiroom.core.domain.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.domain.mapper.toPlacedItemEntity
import com.lumiroom.core.domain.mapper.toRoomDesignEntity
import com.lumiroom.core.network.repository.CloudRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudRepository: CloudRepository,
    private val roomDesignDao: RoomDesignDao,
    private val placedItemDao: PlacedItemDao
) {
    fun scheduleRoomSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<RoomSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "RoomSyncWorker",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
    }

    suspend fun syncDownRooms() = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext
        val result = cloudRepository.downloadRooms(user.uid)

        if (result is LumiroomResult.Success) {
            val cloudRooms = result.data
            for (cloudRoom in cloudRooms) {
                // Conflict resolution: Local Write Wins
                // Only overwrite if cloud updatedAt is strictly > local updatedAt, or local doesn't exist.
                val localRoom = roomDesignDao.getRoomWithItemsOnce(cloudRoom.roomId)
                
                if (localRoom == null || cloudRoom.updatedAt > localRoom.roomDesign.updatedAt) {
                    // Update Local
                    roomDesignDao.insert(cloudRoom.toRoomDesignEntity(syncStatus = "synced"))
                    
                    // Replace all placed items
                    placedItemDao.deleteAllForRoom(cloudRoom.roomId)
                    val items = cloudRoom.layoutData.map { it.toPlacedItemEntity(cloudRoom.roomId) }
                    placedItemDao.insertAll(items)
                }
            }
        }
    }
}
