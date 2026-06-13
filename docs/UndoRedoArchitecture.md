# Undo/Redo Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom implements a robust history tracking system capable of reverting complex geometric and relational changes instantly.

## The Snapshot Model

Because `RoomState` is an immutable Kotlin data class, implementing Undo/Redo is trivial and highly performant. We do not use the Command Pattern (where each action requires a custom inverse function). Instead, we use the **Snapshot Pattern**.

### `HistoryManager` (Inside RoomStateManager)

1. **Stack**: Maintains a `List<RoomState>` and a `currentIndex` pointer.
2. **Push**: Whenever a destructive action occurs (move, delete, add wall), the `RoomStateManager` takes the current state, adds it to the list, increments the pointer, and drops any "redo" states ahead of the pointer.
3. **Undo**: Decrements the pointer and emits the `RoomState` at that index.
4. **Redo**: Increments the pointer and emits the `RoomState`.

## Memory Management

To prevent Out-Of-Memory (OOM) errors during long sessions, the History Stack is capped at **50 states**. When the 51st state is added, the oldest state (index 0) is evicted.

Because data classes share references to unchanged objects (e.g., if you move Sofa A, the new `RoomState` references the exact same memory address for Sofa B as the previous state), the memory footprint of storing 50 snapshots is exceptionally small.
