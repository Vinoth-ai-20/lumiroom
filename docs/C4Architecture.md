# C4 Architecture Models

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Sequence Diagrams](SequenceDiagrams.md)

---

## 1. Level 1: System Context Diagram
Shows the system in relation to the user and external dependencies.

```mermaid
C4Context
    title Level 1: System Context Diagram
    
    Person(user, "Interior Designer", "A user planning a room layout")
    System(lumiroom, "Lumiroom Application", "Mobile AR Furniture Visualizer")
    System_Ext(vertex, "Vertex AI", "Analyzes room health and layouts")
    System_Ext(firebase, "Firebase Services", "Auth, Storage, and Firestore DB")
    System_Ext(speech, "Google Speech API", "Transcribes voice commands")

    Rel(user, lumiroom, "Interacts using voice and touch")
    Rel(lumiroom, vertex, "Requests AI Layout Analysis")
    Rel(lumiroom, firebase, "Syncs states and downloads 3D assets")
    Rel(lumiroom, speech, "Sends audio for transcription")
```

---

## 2. Level 2: Container Diagram
Breaks down the Lumiroom Application into executable/deployable units.

```mermaid
C4Container
    title Level 2: Container Diagram
    
    Person(user, "User")
    
    Container_Boundary(mobile, "Lumiroom Android App") {
        Container(ui, "Compose UI Module", "Kotlin/Jetpack Compose", "Renders the UI")
        Container(ar_engine, "AR Engine", "SceneView/Filament", "Renders 3D GLB assets natively")
        Container(domain, "Domain Logic", "Kotlin Coroutines", "Executes business logic")
        ContainerDb(room, "Local Database", "SQLite/Room", "Caches room configurations")
    }
    
    System_Ext(firebase, "Firebase Backend")
    
    Rel(user, ui, "Taps controls")
    Rel(user, ar_engine, "Views AR space")
    Rel(ui, domain, "Dispatches intents")
    Rel(ar_engine, domain, "Reports hit tests")
    Rel(domain, room, "Reads/Writes state")
    Rel(room, firebase, "Background sync", "gRPC")
```

---

## 3. Level 3: Component Diagram (Domain Logic Container)
Breaks down the Domain Logic container.

```mermaid
C4Component
    title Level 3: Component Diagram - Domain Logic
    
    Container_Boundary(domain, "Domain Logic Container") {
        Component(vm, "ArViewModel", "Kotlin", "State holder for AR Screen")
        Component(placement, "PlaceFurnitureUseCase", "Kotlin", "Validates and processes placements")
        Component(voice, "VoiceCommandParser", "Kotlin", "NLP fuzz matching logic")
        Component(repo, "FurnitureRepository", "Kotlin", "Abstracts data source")
    }
    
    ComponentDb(room, "Room Database", "SQLite")
    
    Rel(vm, voice, "Routes transcriptions")
    Rel(voice, placement, "Triggers intent")
    Rel(vm, placement, "Direct touch intent")
    Rel(placement, repo, "Persists entity")
    Rel(repo, room, "Executes SQL query")
```
