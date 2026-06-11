# Sequence Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to C4 Architecture](C4Architecture.md) | [Next: State Machine Diagrams](StateMachineDiagrams.md)

---

## 1. Voice Command AR Placement

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Mic as SpeechRecognizer
    participant UI as ArScreen
    participant VM as ArViewModel
    participant Parser as CommandParser
    participant SM as RoomStateManager
    participant AR as SceneView Engine

    User->>Mic: "Place modern sofa here"
    Mic-->>UI: Transcription
    UI->>VM: onVoiceResult(transcription)
    VM->>Parser: parseCommand(transcription)
    Parser-->>VM: Intent: PLACE, Target: SOFA
    
    VM->>AR: executeRaycast(screenCenter)
    AR-->>VM: HitResult(x,y,z)
    
    VM->>SM: dispatch(AddItem(SOFA, HitResult))
    SM->>SM: Mutate RoomState
    SM-->>VM: Emit new RoomState
    VM->>UI: Update State
    UI->>AR: Render Model at coords
```

---

## 2. 2D Planner Placement

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as RoomPlannerScreen
    participant VM as RoomPlannerViewModel
    participant SM as RoomStateManager

    User->>UI: Drag and drop from Catalog
    UI->>VM: onFurnitureDrop(catalogId, x, y)
    VM->>SM: dispatch(AddItem(catalogId, x, y, 0))
    SM->>SM: Mutate RoomState
    SM-->>VM: Emit new RoomState
    VM->>UI: Render new canvas object
```

---

## 3. Selection Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Compose UI
    participant SM as RoomStateManager
    
    User->>UI: Tap on item
    UI->>SM: dispatch(SelectItem(itemId))
    SM->>SM: Update SelectionState
    SM-->>UI: Emit new RoomState
    UI->>UI: Draw bounding box / highlight
```

---

## 4. Undo Operation

```mermaid
sequenceDiagram
    actor User
    participant UI as Compose UI
    participant VM as ViewModel
    participant SM as RoomStateManager
    participant History as UndoRedoManager
    
    User->>UI: Tap Undo
    UI->>VM: undo()
    VM->>SM: dispatch(Undo)
    SM->>History: popPreviousState()
    History-->>SM: Previous RoomState Snapshot
    SM->>SM: Set current state = snapshot
    SM-->>UI: Emit restored RoomState
```

---

## 5. Delete Furniture

```mermaid
sequenceDiagram
    actor User
    participant UI as Compose UI
    participant SM as RoomStateManager
    
    User->>UI: Tap Delete (on selected item)
    UI->>SM: dispatch(RemoveItem(selectedId))
    SM->>SM: Filter items, push history snapshot
    SM-->>UI: Emit updated RoomState
```

---

## 6. Save Project & Autosave

```mermaid
sequenceDiagram
    participant SM as RoomStateManager
    participant Persist as LayoutPersistenceManager
    participant Repo as SharedRoomRepository
    participant DB as RoomDatabase
    
    SM->>Persist: State Flow Emission
    Persist->>Persist: Debounce (e.g. 1000ms)
    Persist->>Repo: saveRoomSnapshot(state.toModel())
    Repo->>DB: UPSERT room_designs & placed_items
```

---

## 7. Load Project

```mermaid
sequenceDiagram
    actor User
    participant UI as SavedRoomsScreen
    participant Repo as SharedRoomRepository
    participant DB as RoomDatabase
    participant SM as RoomStateManager
    
    User->>UI: Tap on saved room
    UI->>Repo: observeRoom(roomId)
    Repo->>DB: SELECT *
    DB-->>Repo: Database Entities
    Repo-->>UI: Domain Model Flow
    UI->>SM: dispatch(LoadRoom(model))
    SM-->>UI: Emit loaded RoomState
```

---

## 8. Cloud Synchronization

```mermaid
sequenceDiagram
    participant DB as Local Room DB
    participant Sync as SyncManager
    participant Net as NetworkListener
    participant FB as Firestore

    Net->>Sync: Connection Restored
    Sync->>DB: queryUnsyncedChanges()
    DB-->>Sync: List<RoomModel>
    
    loop For each change
        Sync->>FB: writeBatch.set(document)
    end
    
    Sync->>FB: writeBatch.commit()
    FB-->>Sync: Success Acknowledgment
    Sync->>DB: markAsSynced()
```
