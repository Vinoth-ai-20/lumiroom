package com.lumiroom.core.network.source

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadThumbnail(userId: String, roomId: String, file: File): String {
        val ref = storage.reference.child("users/$userId/rooms/$roomId/thumbnail.jpg")
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteThumbnail(userId: String, roomId: String) {
        val ref = storage.reference.child("users/$userId/rooms/$roomId/thumbnail.jpg")
        try {
            ref.delete().await()
        } catch (e: Exception) {
            // Ignore if it doesn't exist
        }
    }
}
