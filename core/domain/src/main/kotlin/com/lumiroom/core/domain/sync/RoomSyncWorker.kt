package com.lumiroom.core.domain.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.domain.mapper.toCloudModel
import com.lumiroom.core.network.repository.CloudRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RoomSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val roomDesignDao: RoomDesignDao,
    private val cloudRepository: CloudRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            return@withContext Result.failure()
        }

        val pendingRooms = roomDesignDao.getPendingSyncRooms()
        var allSuccess = true

        for (room in pendingRooms) {
            try {
                // Mark as syncing
                roomDesignDao.updateSyncStatus(room.id, "syncing")

                // We need the full RoomDesignWithItems object
                val roomWithItems = roomDesignDao.getRoomWithItemsOnce(room.id)
                if (roomWithItems == null) {
                    allSuccess = false
                    roomDesignDao.updateSyncStatus(room.id, "failed")
                    continue
                }

                // Upload to cloud
                val cloudModel = roomWithItems.toCloudModel(user.uid)
                val result = cloudRepository.uploadRoom(user.uid, cloudModel)

                if (result is LumiroomResult.Success) {
                    // Mark as synced
                    roomDesignDao.updateSyncStatus(room.id, "synced")
                } else {
                    allSuccess = false
                    roomDesignDao.updateSyncStatus(room.id, "failed")
                }
            } catch (e: Exception) {
                allSuccess = false
                roomDesignDao.updateSyncStatus(room.id, "failed")
            }
        }

        if (allSuccess) Result.success() else Result.retry()
    }
}
