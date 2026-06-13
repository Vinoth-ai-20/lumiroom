# Design Patterns in Lumiroom

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

Lumiroom implements several well-known design patterns to maintain a Clean Architecture across its multiple modules.

## 1. MVI (Model-View-Intent)
The UI layer heavily relies on MVI principles to maintain Unidirectional Data Flow.
- **Model**: `UiState` data classes.
- **View**: Jetpack Compose functions.
- **Intent**: Methods on ViewModels or `RoomStateManager.dispatch(action)`.

## 2. Repository Pattern
Data access is abstracted via interfaces. 
- Example: `FurnitureRepository` abstracts whether a 3D model is fetched from local cache or downloaded from Firebase Storage.

## 3. Snapshot Pattern (Memento)
Used for Undo/Redo functionality. The `RoomState` is an immutable snapshot of the entire room at a given point in time. Rather than storing a diff of changes (Command Pattern), Lumiroom pushes the entire snapshot to a stack.

## 4. Observer Pattern
Kotlin `StateFlow` and `SharedFlow` are used extensively to observe state changes and events without tight coupling.

## 5. Dependency Injection (Singleton)
Dagger Hilt provides singleton instances of heavy objects (like `RoomDatabase` or `FirestoreDataSource`) across the app's lifecycle.
