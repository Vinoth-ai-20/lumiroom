# Canvas Architecture

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

The 2D Planner is built entirely using Jetpack Compose `Canvas`. It avoids Android `View` hierarchy overhead by drawing primitives directly.

## Rendering Pipeline

1. **State Observation**: `RoomPlannerScreen` collects the `RoomState`.
2. **Clear Canvas**: The background is drawn (usually a grid pattern).
3. **Draw Walls**: `WallEntity` and `CornerPointEntity` objects are drawn using `drawLine` and `drawCircle`.
4. **Draw Furniture**: `PlacedItem` objects are drawn. If they have a 2D thumbnail, `drawImage` is used. Otherwise, a semantic bounding box is drawn based on the 3D model's bounds.
5. **Draw Selection**: If a `PlacedItem` is selected, an overlay bounding box with rotation/scale handles is drawn.

## Interaction Handling

Because Canvas does not have native click listeners for drawn shapes, interaction is managed via `pointerInput(Unit) { detectTransformGestures { ... } }`.

When a tap occurs:
1. The canvas coordinates are converted to logical room coordinates based on the `CameraState` (pan/zoom).
2. A hit-test algorithm iterates through all `PlacedItem` bounding boxes to see if the tap intersects.
3. If an intersection occurs, `RoomStateManager.dispatch(SelectItem)` is called.
