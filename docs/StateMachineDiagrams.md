# State Machine Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Activity Diagrams](ActivityDiagrams.md)

---

## 1. AR Entity Lifecycle

Describes the state of a 3D furniture model inside the AR Scene.

```mermaid
stateDiagram-v2
    [*] --> Placed : User taps screen with selected catalog item
    
    Placed --> Selected : User taps object
    Selected --> Moving : User drags object
    Moving --> Selected : User releases object
    
    Selected --> Hidden : User toggles visibility
    Hidden --> Selected : User toggles visibility
    
    Selected --> Locked : User locks object
    Locked --> Selected : User unlocks object
    
    Selected --> Deleted : User taps delete
    Deleted --> [*]
```

## 2. Voice Command State Machine

Handles the state progression of the SpeechRecognizer.

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Listening : User holds Mic button
    Listening --> Processing : User releases Mic button
    Processing --> Executing : Intent parsed successfully
    Processing --> Error : NLP Error / Invalid command
    Executing --> Idle
    Error --> Idle
```
