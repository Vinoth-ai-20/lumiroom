# Class Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: ER Diagrams](ERDiagrams.md)

---

## 1. Domain Layer Class Diagram
Demonstrates the separation between presentation state holders (ViewModels) and business logic (UseCases).

```mermaid
classDiagram
    class ArViewModel {
        -StateFlow~ArState~ uiState
        -PlaceFurnitureUseCase placeFurniture
        -VoiceCommandParser voiceParser
        +onVoiceCommand(transcript: String)
        +onHitResult(hit: HitResult)
        +undo()
    }
    
    class PlaceFurnitureUseCase {
        -FurnitureRepository repository
        +invoke(id: String, hit: HitResult): Result~Unit~
    }
    
    class VoiceCommandParser {
        +parse(transcript: String): VoiceIntent
    }
    
    class VoiceIntent {
        <<enumeration>>
        PLACE
        MOVE
        ROTATE
        DELETE
    }
    
    ArViewModel --> PlaceFurnitureUseCase
    ArViewModel --> VoiceCommandParser
    VoiceCommandParser ..> VoiceIntent
```

---

## 2. Data Layer Repository Pattern
Shows the abstraction of Local and Remote data sources.

```mermaid
classDiagram
    class FurnitureRepository {
        <<interface>>
        +getPlacedItems(roomId: String): Flow~List~PlacedItem~~
        +insertItem(item: PlacedItem)
        +deleteItem(itemId: String)
    }
    
    class FurnitureRepositoryImpl {
        -PlacedItemDao localDao
        -FirebaseFirestore remoteDb
    }
    
    class PlacedItemDao {
        <<interface>>
        +getAll(roomId): Flow
        +insert(item)
    }
    
    FurnitureRepositoryImpl ..|> FurnitureRepository
    FurnitureRepositoryImpl --> PlacedItemDao
```
