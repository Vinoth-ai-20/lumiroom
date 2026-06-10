package com.lumiroom.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.relation.PlacedItemWithFurniture
import kotlinx.coroutines.flow.Flow

/** DAO for AR placed items within a room design. */
@Dao
interface PlacedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlacedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlacedItemEntity>)

    @Update
    suspend fun update(item: PlacedItemEntity)

    @Delete
    suspend fun delete(item: PlacedItemEntity)

    @Query("DELETE FROM placed_item WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM placed_item WHERE room_design_id = :roomId")
    suspend fun deleteAllForRoom(roomId: String)

    // ── Reads ──────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM placed_item WHERE room_design_id = :roomId ORDER BY created_at ASC")
    fun getItemsForRoom(roomId: String): Flow<List<PlacedItemEntity>>

    @Transaction
    @Query("SELECT * FROM placed_item WHERE room_design_id = :roomId ORDER BY created_at ASC")
    fun getItemsWithFurnitureForRoom(roomId: String): Flow<List<PlacedItemWithFurniture>>

    @Query("SELECT * FROM placed_item WHERE id = :id")
    suspend fun getItemById(id: String): PlacedItemEntity?

    @Query("SELECT COUNT(*) FROM placed_item WHERE room_design_id = :roomId")
    fun getItemCountForRoom(roomId: String): Flow<Int>

    // ── Transform Update (hot path — avoids full object rewrite) ─────────────

    @Query("""
        UPDATE placed_item 
        SET pos_x = :posX, pos_y = :posY, pos_z = :posZ,
            rot_x = :rotX, rot_y = :rotY, rot_z = :rotZ, rot_w = :rotW,
            scale_x = :scaleX, scale_y = :scaleY, scale_z = :scaleZ,
            transform_matrix = :matrix
        WHERE id = :id
    """)
    suspend fun updateTransform(
        id: String,
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float,
        scaleX: Float, scaleY: Float, scaleZ: Float,
        matrix: String,
    )
}
