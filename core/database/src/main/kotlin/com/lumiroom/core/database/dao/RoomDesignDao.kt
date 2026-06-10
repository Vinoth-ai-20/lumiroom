package com.lumiroom.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lumiroom.core.database.entity.RoomDesignEntity
import com.lumiroom.core.database.relation.RoomDesignWithItems
import kotlinx.coroutines.flow.Flow

/**
 * DAO for room design persistence.
 *
 * [@Transaction] is required on any query returning a [@Relation]
 * (like [RoomDesignWithItems]) to avoid possible data inconsistencies.
 */
@Dao
interface RoomDesignDao {

    // ── Write Operations ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(roomDesign: RoomDesignEntity)

    @Update
    suspend fun update(roomDesign: RoomDesignEntity)

    @Delete
    suspend fun delete(roomDesign: RoomDesignEntity)

    @Query("UPDATE room_design SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archiveRoom(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE room_design SET is_archived = 0, updated_at = :now WHERE id = :id")
    suspend fun restoreRoom(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE room_design SET sync_status = :status, updated_at = :now WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE room_design SET thumbnail_path = :path, updated_at = :now WHERE id = :id")
    suspend fun updateThumbnail(id: String, path: String, now: Long = System.currentTimeMillis())

    // ── Reads — Single ────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM room_design WHERE id = :id")
    fun getRoomWithItems(id: String): Flow<RoomDesignWithItems?>

    @Transaction
    @Query("SELECT * FROM room_design WHERE id = :id")
    suspend fun getRoomWithItemsOnce(id: String): RoomDesignWithItems?

    // ── Reads — List ──────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM room_design 
        WHERE user_id = :userId AND is_archived = 0 
        ORDER BY updated_at DESC
    """)
    fun getUserRooms(userId: String): Flow<List<RoomDesignEntity>>

    @Query("""
        SELECT * FROM room_design 
        WHERE user_id = :userId AND is_archived = 1 
        ORDER BY updated_at DESC
    """)
    fun getArchivedRooms(userId: String): Flow<List<RoomDesignEntity>>

    // ── Sync Queue ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM room_design WHERE sync_status IN ('local', 'failed', 'queued') AND is_archived = 0")
    suspend fun getPendingSyncRooms(): List<RoomDesignEntity>

    // ── Count ─────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM room_design WHERE user_id = :userId AND is_archived = 0")
    fun getRoomCount(userId: String): Flow<Int>
}
