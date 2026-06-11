# C4 Architecture Models

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: Sequence Diagrams](SequenceDiagrams.md)

---

## 1. Level 1: System Context Diagram

Shows the system in relation to the user and external dependencies.

```mermaid
C4Context
    title Level 1: System Context Diagram
    
    Person(user, "User", "An interior designer or home owner planning a space")
    System(lumiroom, "Lumiroom Application", "Mobile AR and 2D Interior Planner")
    System_Ext(vertex, "Vertex AI", "Analyzes room layouts and styles")
    System_Ext(firebase, "Firebase Storage & Firestore", "Cloud database, file storage, and sync")

    Rel(user, lumiroom, "Interacts via AR, 2D Canvas, and Voice")
    Rel(lumiroom, vertex, "Requests AI Layout Analysis")
    Rel(lumiroom, firebase, "Syncs states and downloads 3D models")
```

---

## 2. Level 2: Container Diagram

Breaks down the Lumiroom Application into distinct operational containers.

```mermaid
C4Container
    title Level 2: Container Diagram
    
    Person(user, "User")
    
    Container_Boundary(mobile, "Lumiroom Android App") {
        Container(state_manager, "State Manager", "RoomStateManager", "Maintains shared RoomState")
        Container(ar_engine, "AR Engine", "SceneView", "Renders 3D, Plane detection, Raycasting")
        Container(planner_2d, "2D Planner", "Compose Canvas", "Renders top-down view, Pan, Zoom")
        Container(asset_catalog, "Asset Catalog", "Repository", "Provides 3D/2D models and metadata")
        Container(history, "History Manager", "UndoRedoManager", "Manages undo/redo stacks")
        Container(repos, "Repositories", "Data Access", "Abstracts data sources")
        ContainerDb(persistence, "Persistence Layer", "Room SQLite", "Local database caching")
    }
    
    System_Ext(firebase, "Firebase Backend")
    
    Rel(user, ar_engine, "Views & Interacts in 3D")
    Rel(user, planner_2d, "Views & Interacts in 2D")
    Rel(ar_engine, state_manager, "Updates/Reads physical state")
    Rel(planner_2d, state_manager, "Updates/Reads logical state")
    Rel(state_manager, history, "Pushes mutations")
    Rel(state_manager, repos, "Requests entity data")
    Rel(repos, persistence, "Reads/Writes")
    Rel(persistence, firebase, "Background Sync Manager")
```

---

## 3. Level 3: Component Diagram

Focusing on the internal architecture of the Core Logic.

```mermaid
C4Component
    title Level 3: Component Diagram - Internal Core
    
    Container_Boundary(core, "Core Architecture Components") {
        Component(ar_session, "AR Session Manager", "Kotlin", "Handles ARCore tracking")
        Component(plane_manager, "Plane Manager", "Kotlin", "Extracts physical boundaries")
        Component(furniture_manager, "Furniture Manager", "Kotlin", "Handles item placements")
        Component(selection_manager, "Selection Manager", "Kotlin", "Handles active selections and transforms")
        Component(minimap, "Minimap Manager", "Kotlin", "Calculates mini-view matrices")
        Component(undo_redo, "UndoRedo Manager", "Kotlin", "Snapshot-based state restoration")
        Component(room_repo, "Room Repository", "Kotlin", "Coordinates local/remote room data")
        Component(canvas_ctrl, "Canvas Controller", "Kotlin", "Drives the 2D planner compose UI")
    }
    
    ComponentDb(db, "Room Database", "SQLite")
    
    Rel(canvas_ctrl, selection_manager, "Dispatches selection")
    Rel(ar_session, plane_manager, "Provides point clouds")
    Rel(plane_manager, room_repo, "Saves boundaries")
    Rel(selection_manager, furniture_manager, "Transforms targets")
    Rel(furniture_manager, undo_redo, "Pushes state")
    Rel(undo_redo, room_repo, "Persists snapshot")
    Rel(canvas_ctrl, minimap, "Requests view region")
    Rel(room_repo, db, "SQL Queries")
```
