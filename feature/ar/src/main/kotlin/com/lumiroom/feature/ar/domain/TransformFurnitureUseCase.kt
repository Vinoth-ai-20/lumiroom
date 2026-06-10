package com.lumiroom.feature.ar.domain

import com.lumiroom.core.database.dao.PlacedItemDao
import javax.inject.Inject

class TransformFurnitureUseCase @Inject constructor(
    private val placedItemDao: PlacedItemDao,
) {
    suspend operator fun invoke(
        itemId: String,
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float,
        scaleX: Float, scaleY: Float, scaleZ: Float,
        matrixJson: String,
    ) {
        placedItemDao.updateTransform(
            id = itemId,
            posX = posX, posY = posY, posZ = posZ,
            rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
            scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ,
            matrix = matrixJson,
        )
    }
}
