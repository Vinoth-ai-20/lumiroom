# RoomState Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


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

## The `RoomModel` Data Class

```kotlin
data class RoomModel(
    val planId: String,
    val name: String,
    val walls: List<RoomWall>,
    val furniture: List<RoomFurniture>,
    val cameraState: CameraState,
    val selectionState: SelectionState,
    val gridSizeCm: Int = 10
)
```

Note that the coordinate system is unified in centimeters (cm) rather than meters. The `TransformManager` ensures consistency when placing and moving objects in both AR and 2D.

## Why Centralized State?

Before this architecture, the AR screen had an array of `Node` objects, and the 2D planner had an array of `CanvasRect` objects. Syncing them was a nightmare. 

Now:
- **AR Screen**: Renders `RoomState.furniture` into 3D space. When an AR item is dragged, it sends an event to `RoomStateManager`.
- **2D Screen**: Renders `RoomState.furniture` onto a canvas. When a 2D item is dragged, it sends an event to `RoomStateManager`.

This means a user can drag a sofa on the 2D tablet view, and a user wearing AR glasses will instantly see the sofa move in real life.

### AR Integration and Anchors
When items are placed or moved in AR:
1. `ArScreen` manages a local `anchorCache`.
2. When the user completes a move gesture, the object generates a new `Anchor` from the SceneView trackable.
3. The new anchor's UUID and `PoseData` are explicitly saved to `RoomModel.anchors` via `ArViewModel.onItemTransformed`. This prevents unlinked nodes from duplicating on room reload.
4. Absolute rotation is preserved in `RoomState` directly to bypass ARCore/SceneView's quaternion-to-euler conversions that introduce gimbal lock.
