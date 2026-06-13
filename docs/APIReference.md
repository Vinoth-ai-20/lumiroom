# API Reference

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).
>
> **AR Synchronization & AppCheck Update (v1.1.1):**
> Addressed Firebase AppCheck initialization failures by applying `DebugAppCheckProviderFactory` on debug builds. AR placement stability is improved by correctly persisting tracking anchors and synchronizing Y/Z axes with the 2D planner.


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: Coding Standards](CodingStandards.md)

---

## 1. Managers (Core Domain)

### `RoomStateManager`
The central brain of Lumiroom. Maintains the `RoomState`.

- **`val state: StateFlow<RoomState>`**: Exposes the reactive room state.
- **`fun dispatch(action: RoomAction)`**: The single entry point for all state mutations (e.g., `MoveItem`, `RotateItem`, `Select`).
- **`fun undo()` / `fun redo()`**: Triggers history traversal.

### `LumiroomArSessionManager`
Manages the ARCore session and SceneView integrations.

- **`fun setupSession(config: ArConfig)`**: Configures AR planes, light estimation, and depth.
- **`fun performHitTest(x: Float, y: Float): HitResult?`**: Projects a 2D screen coordinate into physical 3D space.
- **`fun addAnchorToNode(node: Node, hitResult: HitResult)`**: Spatially locks a 3D node.

### `FurnitureSelectionManager`
Handles the selection state within a room.

- **`fun selectItem(id: UUID)`**: Marks an item as active.
- **`fun clearSelection()`**: Deselects all.
- **`val selectedItemId: StateFlow<UUID?>`**: Observable active selection.

---

## 2. Repositories

### `SharedRoomRepository`
Interface for observing and saving whole room snapshots.

- **`fun observeRoom(roomId: String): Flow<RoomModel>`**: Real-time room stream.
- **`suspend fun saveRoomSnapshot(room: RoomModel)`**: Overwrites local DB.
- **`suspend fun deleteRoom(roomId: String)`**

### `FurnitureRepository`
Access to 3D and 2D models.

- **`fun getCatalog(): Flow<List<Furniture>>`**: All available items.
- **`suspend fun getModelUri(itemId: String): Uri`**: Local file URI for the `GLB` asset.

### `CloudRepository`
Handles remote Firebase sync.

- **`suspend fun uploadRoom(cloudModel: CloudRoomDesign)`**: Pushes state to Firestore.
- **`suspend fun downloadRoom(roomId: String): CloudRoomDesign?`**: Pulls remote state.

---

## 3. ViewModels

### `ArViewModel`
State holder for the AR Screen.

- **`val uiState: StateFlow<ArUiState>`**
- **`fun onHitResult(hit: HitResult)`**: Relays AR taps to the domain layer.
- **`fun onVoiceCommand(transcript: String)`**

### `RoomPlannerViewModel`
State holder for the 2D Planner Canvas.

- **`val uiState: StateFlow<RoomPlannerUiState>`**
- **`fun onCanvasDrag(dx: Float, dy: Float)`**: Handles 2D movement.
- **`fun onZoom(scale: Float)`**: Adjusts camera zoom.
- **`fun onFurnitureDrop(catalogId: String, x: Float, y: Float)`**

### `ChatViewModel`
State holder for the AI Assistant.

- **`fun sendMessage(prompt: String)`**: Dispatches intent to Vertex AI.
- **`val messages: StateFlow<List<ChatMessage>>`**

---

## 4. Utilities

### `ArCaptureUtils`
- **`fun captureSnapshot(view: ArSceneView): Bitmap`**: Takes a 2D screenshot of the AR scene.

### `CoroutineExtensions`
- **`fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T>`**: Extends Kotlin flows to prevent rapid event spamming during AR drags.

---

## 5. Data Models

```kotlin
data class RoomModel(
    val id: String,
    val items: List<PlacedItem>,
    val walls: List<Wall>,
    val metadata: RoomMetadata
)

data class PlacedItem(
    val id: String,
    val catalogId: String,
    val position: Vector3,
    val rotation: Quaternion,
    val scale: Vector3
)
```


## Asset-Driven Catalog Architecture (v12)
The furniture catalog is now completely dynamically generated from local assets.
No manual registration, hardcoded arrays, or JSON seeding is required.
During the application startup (specifically database creation), the system automatically scans the assets/models/ and assets/thumbnails/ directories.
It uses the naming convention roomType_category_variant.glb (e.g. bathroom_bathtub_01.glb) to dynamically generate metadata, categories, pricing, and tags. This forms the single source of truth for the entire application catalog, powering search, filters, and AR persistence.
