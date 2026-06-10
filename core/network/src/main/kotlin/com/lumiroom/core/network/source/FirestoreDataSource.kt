package com.lumiroom.core.network.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lumiroom.core.network.model.CloudRoomDesign
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")
    private val roomsCollection = firestore.collection("rooms")

    suspend fun uploadRoom(userId: String, room: CloudRoomDesign) {
        // Ensure the room document is structured under the user or global rooms collection.
        // For simplicity and security rules (users can only read their own data), we use the global "rooms" collection 
        // with a userId field, but it's more secure to nest it under users/{userId}/rooms/{roomId} if we want.
        // Let's use users/{userId}/rooms/{roomId}
        val roomDoc = usersCollection.document(userId).collection("rooms").document(room.roomId)
        roomDoc.set(room, SetOptions.merge()).await()
    }

    suspend fun downloadRooms(userId: String): List<CloudRoomDesign> {
        val snapshot = usersCollection.document(userId).collection("rooms").get().await()
        return snapshot.documents.mapNotNull { it.toObject(CloudRoomDesign::class.java) }
    }

    suspend fun deleteRoom(userId: String, roomId: String) {
        usersCollection.document(userId).collection("rooms").document(roomId).delete().await()
    }
}
