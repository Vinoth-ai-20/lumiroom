package com.lumiroom.core.database.repository

import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.model.Furniture
import com.lumiroom.core.database.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FurnitureRepository @Inject constructor(
    private val furnitureDao: FurnitureDao,
) {

    fun getAllFurniture(): Flow<List<Furniture>> {
        return furnitureDao.getAllFurniture().map { list -> list.map { it.toDomain() } }
    }

    fun getFilteredFurniture(category: String?, roomType: String?, query: String?): Flow<List<Furniture>> {
        return furnitureDao.getFilteredFurniture(category, roomType, query).map { list -> list.map { it.toDomain() } }
    }

    fun getFavorites(): Flow<List<Furniture>> {
        return furnitureDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    fun getFurnitureById(id: String): Flow<Furniture?> {
        return furnitureDao.getFurnitureById(id).map { it?.toDomain() }
    }
    
    suspend fun getFurnitureByIdOnce(id: String): Furniture? {
        return furnitureDao.getFurnitureByIdOnce(id)?.toDomain()
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        furnitureDao.setFavorite(id, isFavorite)
    }

    suspend fun findFirstByNameOrCategory(query: String): Furniture? {
        return furnitureDao.findFirstByNameOrCategory(query)?.toDomain()
    }
}
