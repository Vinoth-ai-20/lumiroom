# Sequence Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Class Diagrams](ClassDiagrams.md)

---

## 1. Voice Command Placement Sequence

This sequence details the complete flow from a user speaking a command to a 3D object rendering in AR space.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Mic as SpeechRecognizer
    participant UI as Compose UI
    participant VM as ArViewModel
    participant Parser as CommandParser
    participant UseCase as PlaceFurnitureUseCase
    participant DB as Room Database
    participant AR as SceneView Engine

    User->>Mic: "Place modern sofa here"
    Mic-->>UI: Transcription ("Place modern sofa here")
    UI->>VM: onVoiceResult(transcription)
    VM->>Parser: parseCommand(transcription)
    Parser-->>VM: Intent: PLACE, Target: SOFA_ID
    
    VM->>AR: executeRaycast(screenCenter)
    AR-->>VM: HitResult(x,y,z)
    
    VM->>UseCase: invoke(SOFA_ID, HitResult)
    UseCase->>DB: INSERT into placed_items
    DB-->>VM: Flow emits updated list
    VM->>UI: Update State
    UI->>AR: loadModel(SOFA_ID)
    AR-->>User: Visualizes 3D Sofa in physical space
```

---

## 2. Cloud Synchronization Sequence

Details the background sync mechanism when network connectivity is restored.

```mermaid
sequenceDiagram
    participant DB as Local Room DB
    participant Sync as SyncManager
    participant Net as NetworkListener
    participant FB as Firestore

    Net->>Sync: Connection Restored Event
    Sync->>DB: queryUnsyncedChanges()
    DB-->>Sync: List<LocalChange>
    
    loop For each change
        Sync->>FB: writeBatch.set(document)
    end
    
    Sync->>FB: writeBatch.commit()
    FB-->>Sync: Success Acknowledgment
    Sync->>DB: markAsSynced()
```
