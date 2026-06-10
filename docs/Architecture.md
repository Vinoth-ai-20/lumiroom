# Software Architecture Description

**Project Title:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

---

## 1. Introduction and Architectural Principles

The architecture of Lumiroom strictly follows modern Android development principles, heavily leaning on **Unidirectional Data Flow (UDF)**, **Dependency Injection**, and the **Repository Pattern**. It is designed with an offline-first philosophy to ensure responsiveness.

### 1.1 Key Principles
- **Separation of Concerns**: UI, Domain, and Data layers are strictly decoupled.
- **Offline-First**: All state modifications happen against local Room databases first, syncing with Firestore asynchronously.
- **Reactive State**: Jetpack Compose observes Kotlin `StateFlow` streams exposed by `ViewModel`s.

---

## 2. C4 Model Diagrams

### 2.1 System Context Diagram
```mermaid
C4Context
    title System Context Diagram for Lumiroom
    
    Person(user, "User", "Homeowner or designer looking to visualize furniture")
    System(lumiroom, "Lumiroom App", "AR application for furniture visualization")
    System_Ext(firestore, "Firebase Firestore", "Cloud NoSQL Database")
    System_Ext(vertexai, "Google Vertex AI", "Provides ML/AI room recommendations")
    
    Rel(user, lumiroom, "Views and places furniture via AR")
    Rel(lumiroom, firestore, "Syncs room layouts", "HTTPS")
    Rel(lumiroom, vertexai, "Requests health score & compatibility", "REST API")
```

### 2.2 Container Diagram
```mermaid
C4Container
    title Container Diagram for Lumiroom App
    
    Container(ui, "UI Layer", "Jetpack Compose", "Renders the interface and handles user events")
    Container(arcore, "AR Engine", "SceneView / Filament", "Renders 3D GLB models in AR space")
    Container(domain, "Domain Layer", "Kotlin", "Use cases (e.g., PlaceFurnitureUseCase)")
    Container(data, "Data Layer", "Kotlin / Flow", "Repositories orchestrating local & remote data")
    ContainerDb(room, "Local Database", "SQLite / Room", "Stores offline catalogs and layouts")
    
    Rel(ui, arcore, "Embeds ArSceneView")
    Rel(ui, domain, "Dispatches intents")
    Rel(domain, data, "Requests data operations")
    Rel(data, room, "Reads from and writes to")
```

### 2.3 Component Diagram (Data Layer)
```mermaid
C4Component
    title Component Diagram for the Data Layer
    
    Component(repo, "FurnitureRepository", "Kotlin Class", "Abstracts data sources")
    Component(roomDao, "FurnitureDao", "Room Interface", "Executes local queries")
    Component(firestoreApi, "FirestoreDataSource", "Kotlin Class", "Handles network calls")
    
    Rel(repo, roomDao, "Reads/Writes local state")
    Rel(repo, firestoreApi, "Fetches updates from cloud")
```

---

## 3. Detailed UML Diagrams

### 3.1 MVVM / Unidirectional Data Flow
```mermaid
sequenceDiagram
    participant UI as Compose UI
    participant VM as ArViewModel
    participant UC as PlaceFurnitureUseCase
    participant Repo as PlacedItemRepository
    
    UI->>VM: executeCommand(VoiceCommand.Place)
    VM->>UC: invoke(furnitureId, coords)
    UC->>Repo: insertPlacedItem(entity)
    Repo-->>UC: success
    UC-->>VM: result
    VM->>UI: _uiState.update(items)
```

### 3.2 State Machine Diagram: AR Placement Mode
```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> VoiceListening : User activates mic
    VoiceListening --> IntentParsed : Speech API returns string
    IntentParsed --> PlacementMode : "Place X" command
    IntentParsed --> Idle : Unrecognized command
    
    PlacementMode --> AnchorCreated : User taps plane OR "Here" command
    AnchorCreated --> ObjectManipulating : Object spawned
    ObjectManipulating --> ObjectManipulating : Rotate/Scale
    ObjectManipulating --> Idle : Deselect
```

---

## 4. Architectural Decisions (ADRs)

- **ADR-001: SceneView over ArSceneform**: SceneView provides native Kotlin APIs and active maintainership compared to the deprecated ArSceneform.
- **ADR-002: Offline-First SQLite**: Ensures AR experience does not stutter or fail in dead zones inside homes.
- **ADR-003: Hilt over Koin**: Compile-time safety for dependency injection is preferred in a multi-module architecture.
