# Architecture Decision Records (ADR)

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

This document logs significant architectural decisions made during the development of Lumiroom.

## ADR-01: SceneView over ArSceneform

- **Status**: Accepted
- **Context**: Google deprecated ArSceneform in favor of native ARCore usage. Maintaining a custom 3D renderer is out of scope.
- **Decision**: We will use the open-source `SceneView` library.
- **Consequences**: We gain native Kotlin support and Filament PBR rendering, but we are tied to the SceneView community for updates.

## ADR-02: Room + Firestore Hybrid (Offline-First)

- **Status**: Accepted
- **Context**: AR requires instantaneous response. Depending entirely on Firestore introduces latency and fails completely in offline scenarios (e.g., new house construction without WiFi).
- **Decision**: All application logic reads and writes to a local SQLite database using Android Room. A background `SyncManager` handles batching those changes to Firestore.
- **Consequences**: Dramatically improved UI responsiveness. Introduces complexity in state reconciliation (Last-Write-Wins strategy adopted).

## ADR-03: Shared `RoomState`

- **Status**: Accepted
- **Context**: Implementing a 2D planner and an AR mode separately led to massive code duplication and sync issues (e.g., placing an item in 2D didn't appear in AR without a DB roundtrip).
- **Decision**: Extract state management to a unified `RoomStateManager` residing in the `core:domain` module. Both AR and 2D viewmodels observe this single source of truth.
- **Consequences**: True multi-modal synchronization. The 2D canvas and AR view can theoretically run on split-screen simultaneously and remain perfectly in sync.

## ADR-04: Memento Pattern for History

- **Status**: Accepted
- **Context**: Users need Undo/Redo. Implementing the Command pattern for complex 3D transformations and wall drawings is highly error-prone.
- **Decision**: Because `RoomState` is an immutable data class, we use the Memento (Snapshot) pattern. We push the entire state object to a stack on every change.
- **Consequences**: Trivial implementation. Minimal memory overhead due to Kotlin's structural sharing in data classes. Capped at 50 states to prevent memory leaks over time.
