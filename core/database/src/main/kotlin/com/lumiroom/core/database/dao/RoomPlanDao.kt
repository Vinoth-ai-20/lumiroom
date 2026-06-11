package com.lumiroom.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lumiroom.core.database.entity.FloorPlanItemEntity
import com.lumiroom.core.database.entity.RoomPlanEntity
import com.lumiroom.core.database.entity.RoomPlanWithDetails
import com.lumiroom.core.database.entity.WallEntity
import com.lumiroom.core.database.entity.DoorEntity
import com.lumiroom.core.database.entity.WindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomPlan(plan: RoomPlanEntity)

    @Update
    suspend fun updateRoomPlan(plan: RoomPlanEntity)

    @Query("DELETE FROM room_plans WHERE id = :planId")
    suspend fun deleteRoomPlan(planId: String)

    @Query("SELECT * FROM room_plans ORDER BY updatedAt DESC")
    fun getAllRoomPlans(): Flow<List<RoomPlanEntity>>

    @Transaction
    @Query("SELECT * FROM room_plans WHERE id = :planId")
    fun getRoomPlanWithDetails(planId: String): Flow<RoomPlanWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalls(walls: List<WallEntity>)

    @Query("DELETE FROM walls WHERE planId = :planId")
    suspend fun deleteWallsByPlanId(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FloorPlanItemEntity>)

    @Query("DELETE FROM floor_plan_items WHERE planId = :planId")
    suspend fun deleteItemsByPlanId(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoors(doors: List<DoorEntity>)

    @Query("DELETE FROM doors WHERE planId = :planId")
    suspend fun deleteDoorsByPlanId(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindows(windows: List<WindowEntity>)

    @Query("DELETE FROM windows WHERE planId = :planId")
    suspend fun deleteWindowsByPlanId(planId: String)

    @Transaction
    suspend fun saveFullPlan(
        plan: RoomPlanEntity,
        walls: List<WallEntity>,
        items: List<FloorPlanItemEntity>,
        doors: List<DoorEntity>,
        windows: List<WindowEntity>
    ) {
        insertRoomPlan(plan)
        deleteWallsByPlanId(plan.id)
        insertWalls(walls)
        deleteItemsByPlanId(plan.id)
        insertItems(items)
        deleteDoorsByPlanId(plan.id)
        insertDoors(doors)
        deleteWindowsByPlanId(plan.id)
        insertWindows(windows)
    }
}
