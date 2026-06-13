# Component Interactions

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

This document highlights how specific independent components interact across module boundaries.

## 1. Catalog -> Room Planner Interaction

When a user drags an item from the `feature:catalog` into the `feature:room-planner`:

1. The Catalog exposes a bottom sheet or a side panel with draggable items.
2. A `LocalDragAndDrop` composition local (or similar mechanism) passes the `FurnitureEntity.id` across the Compose boundary.
3. The `RoomPlannerScreen` detects the drop event at coordinate `(x,y)`.
4. It calls `RoomPlannerViewModel.onFurnitureDrop(id, x, y)`.
5. The `RoomStateManager` adds the item to the global state.

## 2. Voice Command -> AR Screen

When a user is in the `feature:ar` screen and speaks a command handled by `feature:voice`:

1. `VoiceCommandManager` (in `feature:voice`) transcribes the speech.
2. `CommandParser` parses the intent (e.g., "Rotate").
3. The intent is emitted via a Flow.
4. `ArViewModel` observes this Flow. If a "Rotate" intent is received, it applies the transformation to the currently selected object in the `RoomStateManager`.
