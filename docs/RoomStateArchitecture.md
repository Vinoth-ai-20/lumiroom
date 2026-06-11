# RoomState Architecture

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

`RoomState` is the single source of truth for the spatial, structural, and semantic state of the physical room being designed. 

## The `RoomStateManager`

Instead of individual ViewModels (like `ArViewModel` or `RoomPlannerViewModel`) managing their own state arrays, they hold a reference to a singleton `RoomStateManager`. 

### Responsibilities:
1. **State Holding**: Exposes a `StateFlow<RoomState>`.
2. **Action Dispatching**: Exposes a unified `dispatch(action: RoomAction)` function.
3. **Validation**: Prevents collisions or invalid geometric operations before mutating state.
4. **History Tracking**: Manages pointers for Undo/Redo.

## The `RoomState` Data Class

```kotlin
data class RoomState(
    val furniture: List<PlacedItem>,
    val walls: List<WallEntity>,
    val floorBoundary: PlaneBoundary?,
    val selectionState: SelectionState,
    val cameraState: CameraState,
    val minimapState: MinimapState,
    val metadata: RoomMetadata
)
```

## Why Centralized State?

Before this architecture, the AR screen had an array of `Node` objects, and the 2D planner had an array of `CanvasRect` objects. Syncing them was a nightmare. 

Now:
- **AR Screen**: Renders `RoomState.furniture` into 3D space. When an AR item is dragged, it sends an event to `RoomStateManager`.
- **2D Screen**: Renders `RoomState.furniture` onto a canvas. When a 2D item is dragged, it sends an event to `RoomStateManager`.

This means a user can drag a sofa on the 2D tablet view, and a user wearing AR glasses will instantly see the sofa move in real life.
