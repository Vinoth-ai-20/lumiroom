# Minimap Architecture

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

The 2D Room Planner features a Minimap to help users navigate large floor plans when zoomed in.

## Architecture

The minimap is not a separate rendering engine; it is a secondary Compose `Canvas` that observes the exact same `RoomState` as the primary 2D canvas, but applies a different `Matrix` transform.

### Components

1. **`MinimapState`**: Stored inside `RoomState`. Holds the minimap's overall scale, offset, and the viewport bounds of the primary canvas.
2. **Viewport Rect**: A highlighted rectangle drawn on top of the minimap representing the current viewable area of the primary canvas.
3. **Event Forwarding**: Dragging the viewport rect on the minimap fires a `PanCamera` event to the `RoomStateManager`, which updates the primary canvas's `CameraState`, causing both to re-render in unison.

## Mathematical Transformation

To render the minimap, the system calculates the bounding box of all `WallEntity` and `PlacedItem` objects, adds a 10% padding margin, and calculates a uniform scale factor to fit that bounding box within the allocated minimap UI dimensions (e.g., 150x150 dp).
