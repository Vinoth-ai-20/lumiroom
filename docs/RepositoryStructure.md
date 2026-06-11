# Repository Structure

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom follows a strict multi-module clean architecture approach. This ensures separation of concerns, fast build times, and clear dependency graphs.

## Module Overview

```text
lumiroom/
├── app/                  
├── core/                 
│   ├── common/           
│   ├── database/         
│   ├── datastore/        
│   ├── domain/           
│   ├── network/          
│   ├── recommendation/   
│   ├── room-analysis/    
│   └── ui/               
├── feature/              
│   ├── ai-assistant/     
│   ├── ar/               
│   ├── auth/             
│   ├── catalog/          
│   ├── onboarding/       
│   ├── room-planner/     
│   ├── saved-rooms/      
│   ├── settings/         
│   └── voice/            
└── docs/                 
```

## 1. App Module (`app/`)
The `app` module is the entry point of the application.
- It contains the `MainActivity`, global navigation graphs (`LumiroomNavHost`), and application-level `ViewModel`.
- It acts as the final aggregator for all Dagger Hilt components.

## 2. Core Modules (`core/`)
Core modules contain foundational logic and data layers shared across feature modules.
- **`core:common`**: Extensions, analytics (`AnalyticsManager`), coroutine dispatchers, and result wrappers.
- **`core:database`**: Local Room database setup, Daos (`FurnitureDao`, `RoomDesignDao`, `PlacedItemDao`), and entities.
- **`core:datastore`**: Proto DataStore or Preferences DataStore for local user preferences.
- **`core:domain`**: The heart of the application logic. Contains `RoomStateManager`, `RoomSyncWorker`, use cases, models, and shared repositories (`SharedRoomRepository`).
- **`core:network`**: Retrofit/Ktor setup, Firebase integrations (`FirestoreDataSource`, `StorageDataSource`, `CloudRepository`), and network models.
- **`core:recommendation`**: AI and algorithmic recommendation engines (`FurnitureCompatibilityEngine`, `StyleCompatibilityScorer`).
- **`core:room-analysis`**: Mathematical models for computing room budgets, layout scores, and styles.
- **`core:ui`**: Shared Jetpack Compose UI components, themes, colors, and typography.

## 3. Feature Modules (`feature/`)
Feature modules are independent user-facing capabilities. They depend on `core` modules but rarely on each other.
- **`feature:ai-assistant`**: Chat-based AI design assistant interface.
- **`feature:ar`**: The AR tracking, rendering, and manipulation screen (`ArScreen`, `LumiroomArSessionManager`).
- **`feature:auth`**: Login, signup, and profile screens.
- **`feature:catalog`**: Browsing and searching the furniture asset library.
- **`feature:onboarding`**: Splash screen and introductory flows.
- **`feature:room-planner`**: The 2D top-down grid and canvas planner.
- **`feature:saved-rooms`**: List and gallery of the user's previously designed rooms.
- **`feature:settings`**: App configuration and permissions.
- **`feature:voice`**: The voice command parser and executor.

## 4. Documentation (`docs/`)
Contains all UML, architecture diagrams, and system requirements conforming to ISO/IEC/IEEE 42010.
