package com.lumiroom.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumiroom.core.database.LumiroomDatabase
import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.dao.RoomPlanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAOs as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesLumiroomDatabase(
        @ApplicationContext context: Context,
        furnitureDaoProvider: Provider<FurnitureDao>
    ): LumiroomDatabase = Room.databaseBuilder(
        context,
        LumiroomDatabase::class.java,
        LumiroomDatabase.DATABASE_NAME,
    )
        .fallbackToDestructiveMigration()
        .addCallback(DatabaseSeeder(context, furnitureDaoProvider))
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_pos_x REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_pos_y REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_pos_z REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_rot_x REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_rot_y REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_rot_z REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_rot_w REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_scale_x REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_scale_y REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN init_scale_z REAL NOT NULL DEFAULT 1.0")
            }
        })
        .addMigrations(object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE placed_item ADD COLUMN is_locked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE placed_item ADD COLUMN is_visible INTEGER NOT NULL DEFAULT 1")
            }
        })
        .addMigrations(object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `room_plans` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `thumbnailPath` TEXT, `gridSizeCm` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `walls` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `startX` REAL NOT NULL, `startY` REAL NOT NULL, `endX` REAL NOT NULL, `endY` REAL NOT NULL, `thicknessCm` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `room_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_walls_planId` ON `walls` (`planId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `floor_plan_items` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `furnitureId` TEXT NOT NULL, `posX` REAL NOT NULL, `posY` REAL NOT NULL, `rotation` REAL NOT NULL, `scaleX` REAL NOT NULL, `scaleY` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `room_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`furnitureId`) REFERENCES `furniture`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_floor_plan_items_planId` ON `floor_plan_items` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_floor_plan_items_furnitureId` ON `floor_plan_items` (`furnitureId`)")
            }
        })
        .addMigrations(object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `doors` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `startX` REAL NOT NULL, `startY` REAL NOT NULL, `endX` REAL NOT NULL, `endY` REAL NOT NULL, `thicknessCm` REAL NOT NULL, `swingAngle` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `room_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_doors_planId` ON `doors` (`planId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `windows` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `startX` REAL NOT NULL, `startY` REAL NOT NULL, `endX` REAL NOT NULL, `endY` REAL NOT NULL, `thicknessCm` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `room_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_windows_planId` ON `windows` (`planId`)")
            }
        })
        .addMigrations(object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE room_plans ADD COLUMN roomAnchorId TEXT")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN posZ REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN scaleZ REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN isVisible INTEGER NOT NULL DEFAULT 1")
            }
        })
        .addMigrations(object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE room_plans ADD COLUMN cameraPanX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE room_plans ADD COLUMN cameraPanY REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE room_plans ADD COLUMN cameraZoom REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE room_plans ADD COLUMN planeBoundariesJson TEXT")
                db.execSQL("ALTER TABLE room_plans ADD COLUMN anchorsJson TEXT")
            }
        })
        .addMigrations(object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initPosX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initPosY REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initPosZ REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initRotation REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initScaleX REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initScaleY REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE floor_plan_items ADD COLUMN initScaleZ REAL NOT NULL DEFAULT 1.0")
            }
        })
        .addMigrations(object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE furniture ADD COLUMN room_type TEXT")
            }
        })
        .build()

    @Provides
    @Singleton
    fun providesFurnitureDao(db: LumiroomDatabase): FurnitureDao = db.furnitureDao()

    @Provides
    @Singleton
    fun providesRoomDesignDao(db: LumiroomDatabase): RoomDesignDao = db.roomDesignDao()

    @Provides
    @Singleton
    fun providesPlacedItemDao(db: LumiroomDatabase): PlacedItemDao = db.placedItemDao()

    @Provides
    @Singleton
    fun providesRoomPlanDao(db: LumiroomDatabase): RoomPlanDao = db.roomPlanDao()
}
