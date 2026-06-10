package com.lumiroom.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumiroom.core.database.LumiroomDatabase
import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.dao.RoomDesignDao
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
        .fallbackToDestructiveMigrationOnDowngrade()
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
}
