package com.lumiroom.feature.ar.domain

import com.lumiroom.core.database.dao.PlacedItemDao
import javax.inject.Inject

class RemoveFurnitureUseCase @Inject constructor(
    private val placedItemDao: PlacedItemDao,
) {
    suspend operator fun invoke(itemId: String) {
        placedItemDao.deleteById(itemId)
    }
}
