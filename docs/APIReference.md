# API Reference

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Coding Standards](CodingStandards.md)

---

## 1. Repositories

### `FurnitureRepository`
Interface defining access to 3D models.
- **`fun getCatalog(): Flow<List<CatalogItem>>`**
  - **Returns**: A Kotlin Flow of all available items from the local DB.
- **`suspend fun getModelUri(itemId: String): Uri`**
  - **Returns**: The local file URI for the `GLB` asset. Triggers a Firebase Storage download if not cached.

### `PlacedItemRepository`
- **`fun getPlacedItems(roomId: String): Flow<List<PlacedItem>>`**
- **`suspend fun insertItem(item: PlacedItem)`**
- **`suspend fun updateTransform(itemId: String, pos: Vector3, rot: Quaternion)`**

---

## 2. Use Cases (Domain Layer)

### `PlaceFurnitureUseCase`
- **`suspend operator fun invoke(catalogId: String, hitResult: HitResult): Result<Unit>`**
  - Validates the hit result, calculates world space matrices, and dispatches the insertion to the repository.

### `VoiceCommandParser`
- **`fun parse(transcript: String): VoiceIntent`**
  - Executes Regex and fuzzy matching on transcriptions.

---

## 3. Data Models

```kotlin
data class PlacedItem(
    val id: String,
    val roomId: String,
    val catalogId: String,
    val position: Position3D,
    val rotation: Quaternion3D,
    val scale: Vector3D
)

data class RoomHealth(
    val score: Int,
    val category: RoomHealthCategory,
    val recommendations: List<String>
)
```
