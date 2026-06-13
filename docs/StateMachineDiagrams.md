# State Machine Diagrams

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to Sequence Diagrams](SequenceDiagrams.md) | [Next: Activity Diagrams](ActivityDiagrams.md)

---

## 1. AR UI States (`ArUiState`)

The states that the AR Screen composable reacts to.

```mermaid
stateDiagram-v2
    [*] --> Initializing : Session starts
    Initializing --> Scanning : ARCore Active
    
    Scanning --> PlaneDetected : First plane found
    PlaneDetected --> Placing : User taps to place
    Placing --> Selected : Item successfully anchored
    
    Selected --> Scanning : User clears selection
    Selected --> Placing : User taps elsewhere
    
    Initializing --> Error : Camera permissions denied
    Scanning --> Error : Tracking failure
```

---

## 2. 2D Planner UI States (`RoomPlannerUiState`)

The top-down canvas planner states.

```mermaid
stateDiagram-v2
    [*] --> ViewMode : Default navigation
    ViewMode --> EditMode : User selects an item
    ViewMode --> DrawWallMode : User taps "Draw Wall"
    
    DrawWallMode --> Drawing : User drags on canvas
    Drawing --> DrawWallMode : User lifts finger
    DrawWallMode --> ViewMode : User cancels drawing
    
    EditMode --> ViewMode : Deselect item
```

---

## 3. Selection States

The shared selection lifecycle inside the `RoomStateManager`.

```mermaid
stateDiagram-v2
    [*] --> None
    None --> SingleItem : selectItem(id)
    SingleItem --> None : clearSelection()
    SingleItem --> SingleItem : selectItem(different_id)
    SingleItem --> Transforming : User moves/rotates
    Transforming --> SingleItem : Transform ends
```

---

## 4. Save States (LayoutPersistenceManager)

Debounced saving states to local Room DB.

```mermaid
stateDiagram-v2
    [*] --> Synced
    Synced --> PendingChanges : RoomState mutation
    PendingChanges --> Saving : Debounce timer (2s) expires
    Saving --> Synced : DB Write Success
    Saving --> Error : DB Write Failure
    Error --> PendingChanges : Retry logic
```

---

## 5. Synchronization States (SyncManager)

Background state of syncing to Firebase.

```mermaid
stateDiagram-v2
    [*] --> Offline
    Offline --> Connecting : Network Restored
    Connecting --> Syncing : Checking local queue
    Syncing --> InSync : No pending changes
    Syncing --> Uploading : Sending batches to Firestore
    Uploading --> InSync : Batch commit success
    Uploading --> Offline : Connection Lost
```
