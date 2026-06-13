# Persistence Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom implements an **Offline-First Reactive Persistence** architecture. The application never blocks the UI waiting for a database write or a network request.

## Local Database (Room SQLite)

The primary persistence layer is `core:database`.

### `LayoutPersistenceManager`

Because `RoomState` updates 60 times a second during AR dragging, we cannot write every frame to the database.

1. **Observation**: `LayoutPersistenceManager` collects the `RoomStateManager.state` Flow.
2. **Debouncing**: It applies a `.debounce(2000)` operator. If the user is actively dragging an item, no database writes occur.
3. **Commit**: When the user stops interacting for 2 seconds, the `RoomState` is mapped to `RoomModel` and passed to `SharedRoomRepository.saveRoomSnapshot()`.
4. **UPSERT**: The `RoomDao` and `PlacedItemDao` use `OnConflictStrategy.REPLACE` to execute massive batch updates instantaneously on a background dispatcher.

## Asset Persistence

3D Models (`.glb`) and 2D thumbnails are too large for SQLite.
- They are cached locally in the App's `CacheDir`.
- The local SQLite `FurnitureEntity` holds a `glb_uri` pointing to either the local cache or the remote Firebase Storage URL.
- The repository handles downloading and caching transparently.
