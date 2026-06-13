package com.lumiroom.core.database.di

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumiroom.core.database.dao.FurnitureDao
import javax.inject.Provider
import kotlin.random.Random

import kotlinx.coroutines.launch

class DatabaseSeeder(
    private val context: Context,
    private val furnitureDaoProvider: Provider<FurnitureDao>
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val cursor = db.query("SELECT COUNT(1) FROM furniture")
                var count = 0
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0)
                }
                cursor.close()
                
                if (count == 0) {
                    android.util.Log.d("LumiroomSeeder", "Database empty, starting seed...")
                    seedDatabase(db)
                } else {
                    android.util.Log.d("LumiroomSeeder", "Database already seeded, count = " + count)
                }
            } catch (e: Exception) {
                android.util.Log.e("LumiroomSeeder", "Error in onOpen", e)
            }
        }
    }

    

    

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        
        try {
            // Read from app/src/main/assets/models dynamically
            val models = context.assets.list("models")?.filter { it.endsWith(".glb") } ?: emptyList()
            val thumbnails = context.assets.list("thumbnails")?.filter { it.endsWith(".webp") } ?: emptyList()
            
            Log.d("LumiroomSeeder", "Models discovered: ${models.size}")
            Log.d("LumiroomSeeder", "Thumbnails discovered: ${thumbnails.size}")
            
            db.beginTransaction()
            var insertedCount = 0
            val categories = mutableSetOf<String>()
            
            try {
                for (filename in models) {
                    val id = filename.substringBeforeLast(".glb")
                    
                    // Parse category and type from format: roomType_category_variant
                    val parts = id.split("_")
                    val rawRoomType = if (parts.isNotEmpty()) parts[0] else "general"
                    val rawCategory = if (parts.size > 1) parts[1] else "furniture"
                    val variant = if (parts.size > 2) parts[2] else "01"
                    
                    val roomType = when (rawRoomType.lowercase()) {
                        "livingroom" -> "Living Room"
                        "diningroom" -> "Dining Room"
                        else -> rawRoomType.replaceFirstChar { it.uppercase() }
                    }
                    val category = rawCategory.replaceFirstChar { it.uppercase() }
                    
                    val name = "$category $variant"
                    val priceEstimate = generateRealisticPrice(rawRoomType, rawCategory, variant)
                    val (w, d, h) = estimateDimensions(rawCategory)

                    categories.add(category)
                    Log.d("LumiroomSeeder", "Metadata generated: $id -> RoomType: $roomType, Category: $category, Price: ₹$priceEstimate")

                    val values = ContentValues().apply {
                        put("id", id)
                        put("name", name)
                        put("category", category)
                        put("room_type", roomType)
                        put("description", "A premium $category for your $roomType.")
                        put("width", w)
                        put("depth", d)
                        put("height", h)
                        put("price_estimate", priceEstimate)
                        put("model_path", "file:///android_asset/models/$id.glb")
                        put("thumbnail_path", "file:///android_asset/thumbnails/$id.webp")
                        put("style", "Modern")
                        put("is_downloaded", if (true) 1 else 0) // Room booleans are 1/0
                        put("is_favorite", if (false) 1 else 0)
                        put("created_at", System.currentTimeMillis())
                        put("updated_at", System.currentTimeMillis())
                    }
                    
                    val rowId = db.insert("furniture", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                    if (rowId != -1L) {
                        insertedCount++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            
            Log.d("LumiroomSeeder", "Furniture records inserted: $insertedCount")
            
            val cursor = db.query("SELECT COUNT(*) FROM furniture")
            if (cursor.moveToFirst()) {
                Log.d("LumiroomSeeder", "Database count: ${cursor.getInt(0)}")
            }
            cursor.close()
            
            Log.d("LumiroomSeeder", "Category count: ${categories.size}")
            
        } catch (e: Exception) {
            Log.e("LumiroomSeeder", "Error during seeding", e)
            e.printStackTrace()
        }
    }

    private fun generateRealisticPrice(roomType: String, category: String, variant: String): Double {
        val random = Random(variant.hashCode())
        val price = when (roomType.lowercase()) {
            "bathroom" -> when (category.lowercase()) {
                "bathtub" -> random.nextInt(15000, 80000)
                "toilet" -> random.nextInt(5000, 25000)
                "vanity", "washbasin" -> random.nextInt(8000, 40000)
                else -> random.nextInt(2000, 10000)
            }
            "livingroom" -> when (category.lowercase()) {
                "sofa" -> random.nextInt(15000, 120000)
                "table", "coffeetable" -> random.nextInt(4000, 30000)
                "tvunit", "drawer", "closet" -> random.nextInt(8000, 60000)
                "chair" -> random.nextInt(5000, 25000)
                else -> random.nextInt(4000, 30000)
            }
            "bedroom" -> when (category.lowercase()) {
                "bed" -> random.nextInt(12000, 150000)
                "wardrobe" -> random.nextInt(10000, 100000)
                "nightstand" -> random.nextInt(2000, 15000)
                else -> random.nextInt(5000, 40000)
            }
            "diningroom" -> when (category.lowercase()) {
                "table", "diningtable" -> random.nextInt(10000, 80000)
                "chair", "diningchair" -> random.nextInt(2000, 15000)
                else -> random.nextInt(5000, 30000)
            }
            "kitchen" -> when (category.lowercase()) {
                "cabinet" -> random.nextInt(10000, 60000)
                "counter" -> random.nextInt(15000, 80000)
                "refrigerator" -> random.nextInt(20000, 100000)
                else -> random.nextInt(5000, 40000)
            }
            "office" -> when (category.lowercase()) {
                "desk" -> random.nextInt(5000, 40000)
                "chair", "officechair" -> random.nextInt(4000, 30000)
                else -> random.nextInt(3000, 20000)
            }
            else -> random.nextInt(5000, 50000)
        }
        return (price / 100 * 100).toDouble()
    }
    
    private fun estimateDimensions(category: String): Triple<Float, Float, Float> {
        return when (category.lowercase()) {
            "bathtub" -> Triple(1.5f, 0.8f, 0.6f)
            "toilet" -> Triple(0.5f, 0.7f, 0.8f)
            "vanity", "washbasin" -> Triple(1.0f, 0.6f, 0.9f)
            "sofa" -> Triple(2.0f, 0.9f, 0.8f)
            "table", "coffeetable" -> Triple(1.2f, 0.6f, 0.45f)
            "tvunit", "drawer", "closet" -> Triple(1.5f, 0.4f, 0.6f)
            "chair", "diningchair", "officechair" -> Triple(0.6f, 0.6f, 0.9f)
            "bed" -> Triple(1.8f, 2.0f, 1.1f)
            "wardrobe" -> Triple(1.2f, 0.6f, 2.0f)
            "nightstand" -> Triple(0.5f, 0.4f, 0.6f)
            "refrigerator" -> Triple(0.8f, 0.8f, 1.8f)
            "desk" -> Triple(1.4f, 0.7f, 0.75f)
            else -> Triple(1.0f, 1.0f, 1.0f)
        }
    }
}
