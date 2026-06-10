package com.lumiroom.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single furniture item placed within an AR scene, belonging to a [RoomDesignEntity].
 *
 * All spatial transforms are stored redundantly:
 * - [transformMatrix]: full 4×4 column-major float array as JSON (authoritative)
 * - pos/rot/scale component columns: for efficient SQL queries and sorting
 *
 * The quaternion fields (rotX..rotW) are in local ARCore world space.
 */
@Entity(
    tableName = "placed_item",
    foreignKeys = [
        ForeignKey(
            entity = RoomDesignEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_design_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FurnitureEntity::class,
            parentColumns = ["id"],
            childColumns = ["furniture_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("room_design_id"),
        Index("furniture_id"),
    ],
)
data class PlacedItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "room_design_id") val roomDesignId: String,
    @ColumnInfo(name = "furniture_id")   val furnitureId: String,

    /** JSON-encoded 4×4 column-major float array from ARCore Pose */
    @ColumnInfo(name = "transform_matrix")
    val transformMatrix: String,

    // ── Position (world space, metres) ────────────────────────────────────────
    @ColumnInfo(name = "pos_x") val posX: Float,
    @ColumnInfo(name = "pos_y") val posY: Float,
    @ColumnInfo(name = "pos_z") val posZ: Float,

    // ── Rotation (quaternion) ─────────────────────────────────────────────────
    @ColumnInfo(name = "rot_x") val rotX: Float,
    @ColumnInfo(name = "rot_y") val rotY: Float,
    @ColumnInfo(name = "rot_z") val rotZ: Float,
    @ColumnInfo(name = "rot_w") val rotW: Float = 1f,

    // ── Scale ─────────────────────────────────────────────────────────────────
    @ColumnInfo(name = "scale_x") val scaleX: Float = 1f,
    @ColumnInfo(name = "scale_y") val scaleY: Float = 1f,
    @ColumnInfo(name = "scale_z") val scaleZ: Float = 1f,

    // ── Initial Transform (For Reset Feature) ─────────────────────────────────
    @ColumnInfo(name = "init_pos_x") val initPosX: Float = 0f,
    @ColumnInfo(name = "init_pos_y") val initPosY: Float = 0f,
    @ColumnInfo(name = "init_pos_z") val initPosZ: Float = 0f,
    
    @ColumnInfo(name = "init_rot_x") val initRotX: Float = 0f,
    @ColumnInfo(name = "init_rot_y") val initRotY: Float = 0f,
    @ColumnInfo(name = "init_rot_z") val initRotZ: Float = 0f,
    @ColumnInfo(name = "init_rot_w") val initRotW: Float = 1f,
    
    @ColumnInfo(name = "init_scale_x") val initScaleX: Float = 1f,
    @ColumnInfo(name = "init_scale_y") val initScaleY: Float = 1f,
    @ColumnInfo(name = "init_scale_z") val initScaleZ: Float = 1f,

    /** Active color variant hex (e.g. "#1A1A2E"), null = default material */
    @ColumnInfo(name = "selected_color") val selectedColor: String?,

    /** Optional user-assigned label shown in scene */
    val label: String?,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_locked") val isLocked: Boolean = false,
    @ColumnInfo(name = "is_visible") val isVisible: Boolean = true,
)
