package com.lumiroom.core.database.di

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.entity.FurnitureEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import javax.inject.Provider

class DatabaseSeeder(
    private val context: Context,
    private val furnitureDaoProvider: Provider<FurnitureDao>
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.assets.open("furniture_seed.json").use { inputStream ->
                    val jsonString = InputStreamReader(inputStream).readText()
                    
                    // We need a serializable data class to parse from JSON, or just use Kotlinx Serialization
                    // We can use a custom data class to parse the JSON and map to FurnitureEntity
                    val seedItems = Json { ignoreUnknownKeys = true }.decodeFromString<List<FurnitureSeedItem>>(jsonString)
                    
                    val entities = seedItems.map { item ->
                        FurnitureEntity(
                            id = item.id,
                            name = item.name,
                            category = item.category,
                            description = item.description,
                            width = item.width,
                            depth = item.depth,
                            height = item.height,
                            priceEstimate = item.priceEstimate,
                            modelPath = item.modelPath,
                            thumbnailPath = item.thumbnailPath,
                            style = item.style,
                            isDownloaded = false,
                            isFavorite = false
                        )
                    }
                    
                    furnitureDaoProvider.get().insertAll(entities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class FurnitureSeedItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val width: Float,
    val depth: Float,
    val height: Float,
    val priceEstimate: Double?,
    val modelPath: String,
    val thumbnailPath: String?,
    val style: String?
)
