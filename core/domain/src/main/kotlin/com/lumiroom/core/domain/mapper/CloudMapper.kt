package com.lumiroom.core.domain.mapper

import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.entity.RoomDesignEntity
import com.lumiroom.core.database.relation.RoomDesignWithItems
import com.lumiroom.core.network.model.CloudPlacedItem
import com.lumiroom.core.network.model.CloudRoomDesign

fun RoomDesignWithItems.toCloudModel(userId: String): CloudRoomDesign {
    return CloudRoomDesign(
        roomId = this.roomDesign.id,
        userId = userId,
        roomName = this.roomDesign.name,
        description = this.roomDesign.description,
        roomType = this.roomDesign.roomType,
        widthM = this.roomDesign.widthM,
        lengthM = this.roomDesign.lengthM,
        heightM = this.roomDesign.heightM,
        arEnvironmentId = this.roomDesign.arEnvironmentId,
        thumbnailUrl = this.roomDesign.thumbnailPath,
        isArchived = this.roomDesign.isArchived,
        styleTag = this.roomDesign.styleTag,
        createdAt = this.roomDesign.createdAt,
        updatedAt = this.roomDesign.updatedAt,
        layoutData = this.placedItems.map { it.toCloudModel() }
    )
}

fun PlacedItemEntity.toCloudModel(): CloudPlacedItem {
    return CloudPlacedItem(
        id = this.id,
        furnitureId = this.furnitureId,
        transformMatrix = this.transformMatrix,
        posX = this.posX,
        posY = this.posY,
        posZ = this.posZ,
        rotX = this.rotX,
        rotY = this.rotY,
        rotZ = this.rotZ,
        rotW = this.rotW,
        scaleX = this.scaleX,
        scaleY = this.scaleY,
        scaleZ = this.scaleZ,
        selectedColor = this.selectedColor,
        label = this.label,
        createdAt = this.createdAt
    )
}

fun CloudRoomDesign.toRoomDesignEntity(syncStatus: String = "synced"): RoomDesignEntity {
    return RoomDesignEntity(
        id = this.roomId,
        userId = this.userId,
        name = this.roomName,
        description = this.description,
        roomType = this.roomType,
        widthM = this.widthM,
        lengthM = this.lengthM,
        heightM = this.heightM,
        arEnvironmentId = this.arEnvironmentId,
        thumbnailPath = this.thumbnailUrl,
        firestoreDocId = this.roomId,
        syncStatus = syncStatus,
        isArchived = this.isArchived,
        styleTag = this.styleTag,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun CloudPlacedItem.toPlacedItemEntity(roomId: String): PlacedItemEntity {
    return PlacedItemEntity(
        id = this.id,
        roomDesignId = roomId,
        furnitureId = this.furnitureId,
        transformMatrix = this.transformMatrix,
        posX = this.posX,
        posY = this.posY,
        posZ = this.posZ,
        rotX = this.rotX,
        rotY = this.rotY,
        rotZ = this.rotZ,
        rotW = this.rotW,
        scaleX = this.scaleX,
        scaleY = this.scaleY,
        scaleZ = this.scaleZ,
        selectedColor = this.selectedColor,
        label = this.label,
        createdAt = this.createdAt
    )
}
