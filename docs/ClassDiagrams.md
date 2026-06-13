# Class Diagrams

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: ER Diagrams](ERDiagrams.md)

---

## 1. Domain Layer: Shared State Architecture

Demonstrates how the `RoomStateManager` serves as the central brain for both AR and 2D views, preventing logic duplication.

```mermaid
classDiagram
    class RoomStateManager {
        -MutableStateFlow~RoomState~ _state
        +val state: StateFlow~RoomState~
        +dispatch(action: RoomAction)
        -handleMoveItem(id, x, y)
        -handleWallDraw(points)
    }
    
    class RoomState {
        +List~Furniture~ furnitureList
        +List~Wall~ walls
        +SelectionState selection
        +CameraState camera
    }
    
    class FurnitureSelectionManager {
        +selectItem(id: UUID)
        +clearSelection()
        +rotateSelected(degrees: Float)
    }
    
    class RoomPlannerManager {
        +addWallNode(x: Float, y: Float)
        +completeWall()
    }

    RoomStateManager --> RoomState : manages
    RoomStateManager *-- FurnitureSelectionManager : delegates selection
    RoomStateManager *-- RoomPlannerManager : delegates 2D layout
```

---

## 2. AR Engine Layer

Shows how ARCore and SceneView are abstracted from the ViewModels.

```mermaid
classDiagram
    class LumiroomArSessionManager {
        -ArSceneView sceneView
        +setupSession(config: Config)
        +performHitTest(x, y): HitResult?
        +addAnchorToNode(node: Node)
    }

    class CloudAnchorManager {
        +hostCloudAnchor(anchor: Anchor): Deferred~String~
        +resolveCloudAnchor(cloudId: String): Deferred~Anchor~
    }
    
    class ArViewModel {
        -RoomStateManager stateManager
        +onTap(x, y)
    }
    
    ArViewModel --> LumiroomArSessionManager : uses
    LumiroomArSessionManager --> CloudAnchorManager : relies on
```

---

## 3. Data Layer Repository Pattern

Shows the abstraction of Local and Remote data sources and synchronization.

```mermaid
classDiagram
    class SharedRoomRepository {
        <<interface>>
        +observeRoom(roomId: String): Flow~RoomModel~
        +saveRoomSnapshot(room: RoomModel)
        +deleteRoom(roomId: String)
    }
    
    class SharedRoomRepositoryImpl {
        -RoomDesignDao localDao
        -CloudRepository remoteRepo
        -CloudMapper mapper
    }
    
    class CloudRepository {
        -FirestoreDataSource firestore
        -StorageDataSource storage
        +uploadRoom(cloudModel: CloudRoomDesign)
    }
    
    SharedRoomRepositoryImpl ..|> SharedRoomRepository
    SharedRoomRepositoryImpl --> RoomDesignDao : Local SQLite
    SharedRoomRepositoryImpl --> CloudRepository : Remote Firebase
    SharedRoomRepositoryImpl --> CloudMapper : Entity mapping
```

---

## 4. UI Layer ViewModels

```mermaid
classDiagram
    class RoomPlannerViewModel {
        -RoomStateManager roomStateManager
        +onCanvasDrag(x, y)
        +onZoom(scale)
    }
    
    class ArViewModel {
        -RoomStateManager roomStateManager
        -PlaceFurnitureUseCase placeFurniture
        +onHitResult(hit: HitResult)
    }
    
    RoomPlannerViewModel --> RoomStateManager
    ArViewModel --> RoomStateManager
```
