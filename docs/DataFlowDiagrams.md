# Data Flow Diagrams (DFD)

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: State Machine Diagrams](StateMachineDiagrams.md)

---

## 1. Level 0: Context Data Flow
High-level data inputs and outputs.

```mermaid
flowchart TD
    User((User))
    Sys[Lumiroom AR System]
    Cloud[(Firebase)]
    AI[(Vertex AI)]

    User -- "Voice Audio / Touch Inputs" --> Sys
    Sys -- "3D Renders / UI" --> User
    Sys -- "Layout Data" --> AI
    AI -- "Health Score" --> Sys
    Sys -- "Local State Changes" --> Cloud
    Cloud -- "Synced Data / Assets" --> Sys
```

---

## 2. Level 1: Core Processing DFD

```mermaid
flowchart LR
    UI[Compose UI]
    VM[ViewModel]
    DB[(Room SQLite)]
    Net[Sync Manager]

    UI -- "1. Dispatch Event (e.g., Move)" --> VM
    VM -- "2. Transform & Validate" --> DB
    DB -- "3. Emit Flow<List<Item>>" --> VM
    VM -- "4. Update StateFlow" --> UI
    
    DB -- "5. Observe Changes" --> Net
    Net -- "6. Batch HTTP Push" --> Cloud[(Firestore)]
```
