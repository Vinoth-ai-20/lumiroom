package com.lumiroom.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lumiroom.core.database.entity.FurnitureEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for furniture catalog CRUD operations.
 */
@Dao
interface FurnitureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(furniture: List<FurnitureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(furniture: FurnitureEntity)

    @Update
    suspend fun update(furniture: FurnitureEntity)

    @Delete
    suspend fun delete(furniture: FurnitureEntity)

    @Query("UPDATE furniture SET is_favorite = :isFavorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE furniture 
        SET is_downloaded = :isDownloaded, 
            updated_at = :now 
        WHERE id = :id
    """)
    suspend fun setDownloaded(
        id: String,
        isDownloaded: Boolean,
        now: Long = System.currentTimeMillis(),
    )

    @Query("SELECT * FROM furniture WHERE id = :id")
    fun getFurnitureById(id: String): Flow<FurnitureEntity?>

    @Query("SELECT * FROM furniture WHERE id = :id")
    suspend fun getFurnitureByIdOnce(id: String): FurnitureEntity?

    @Query("SELECT * FROM furniture ORDER BY name ASC")
    fun getAllFurniture(): Flow<List<FurnitureEntity>>

    @Query("""
        SELECT * FROM furniture 
        WHERE (:category IS NULL OR category = :category)
          AND (:query IS NULL OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun getFilteredFurniture(
        category: String?,
        query: String?,
    ): Flow<List<FurnitureEntity>>

    @Query("""
        SELECT * FROM furniture 
        WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'
        LIMIT 1
    """)
    suspend fun findFirstByNameOrCategory(query: String): FurnitureEntity?

    @Query("SELECT * FROM furniture WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavorites(): Flow<List<FurnitureEntity>>

    @Query("SELECT * FROM furniture WHERE is_downloaded = 1")
    suspend fun getDownloadedFurniture(): List<FurnitureEntity>

    @Query("SELECT COUNT(*) FROM furniture")
    fun getCatalogCount(): Flow<Int>
}
