package com.lumiroom.core.network.repository

import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.core.network.model.CloudRoomDesign
import com.lumiroom.core.network.source.FirestoreDataSource
import com.lumiroom.core.network.source.StorageDataSource
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource
) {
    suspend fun uploadRoom(userId: String, room: CloudRoomDesign): LumiroomResult<Unit> {
        return try {
            firestoreDataSource.uploadRoom(userId, room)
            LumiroomResult.Success(Unit)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    suspend fun downloadRooms(userId: String): LumiroomResult<List<CloudRoomDesign>> {
        return try {
            val rooms = firestoreDataSource.downloadRooms(userId)
            LumiroomResult.Success(rooms)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    suspend fun deleteRoom(userId: String, roomId: String): LumiroomResult<Unit> {
        return try {
            firestoreDataSource.deleteRoom(userId, roomId)
            storageDataSource.deleteThumbnail(userId, roomId)
            LumiroomResult.Success(Unit)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    suspend fun uploadThumbnail(userId: String, roomId: String, file: File): LumiroomResult<String> {
        return try {
            val url = storageDataSource.uploadThumbnail(userId, roomId, file)
            LumiroomResult.Success(url)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }
}
