package com.lumiroom.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.entity.AiSessionEntity
import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.entity.RoomDesignEntity
import com.lumiroom.core.database.entity.UserProfileEntity

/**
 * Lumiroom Room database.
 *
 * Version history:
 *  v1 — Initial schema (furniture, room_design, placed_item, user_profile, ai_session)
 *
 * Migration strategy: use [androidx.room.migration.Migration] objects added to
 * the builder in [com.lumiroom.core.database.di.DatabaseModule]. Never use
 * destructive migrations in production builds.
 */
@Database(
    entities = [
        FurnitureEntity::class,
        RoomDesignEntity::class,
        PlacedItemEntity::class,
        UserProfileEntity::class,
        AiSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LumiroomDatabase : RoomDatabase() {

    abstract fun furnitureDao(): FurnitureDao
    abstract fun roomDesignDao(): RoomDesignDao
    abstract fun placedItemDao(): PlacedItemDao

    companion object {
        const val DATABASE_NAME = "lumiroom.db"
    }
}
