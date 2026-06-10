# Lumiroom

**AI-Assisted Mobile AR Furniture Visualization & Interior Planning System**

[![CI](https://github.com/your-org/lumiroom/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/lumiroom/actions/workflows/ci.yml)

---

## Overview

Lumiroom lets users visualize furniture in their physical space using Augmented Reality, get personalized design recommendations from an AI assistant (Lumi), and plan room layouts with a 2D floor editor — all from a single Android app.

## Technology Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| AR | ARCore + SceneView (Filament) |
| 3D Models | glTF 2.0 / GLB |
| AI | Firebase Vertex AI (Gemini 1.5 Flash) |
| Voice | Android SpeechRecognizer API |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Preferences | DataStore |
| Cloud | Firebase Auth + Firestore + Storage |
| Network | Retrofit + OkHttp |
| Build | Multi-module Gradle + Version Catalog |

## Module Structure

```
:app                    ← Entry point, navigation host
:core:ui                ← Shared Compose components & theme
:core:common            ← Utilities, Result type, dispatchers
:core:network           ← Retrofit/OkHttp client
:core:database          ← Room DB, entities, DAOs
:core:datastore         ← DataStore preferences
:feature:onboarding     ← Splash, carousel, permissions
:feature:auth           ← Firebase Sign In/Up
:feature:catalog        ← Furniture browse, search, filter
:feature:ar             ← ARCore session, SceneView, placement
:feature:room-planner   ← 2D Canvas floor plan editor
:feature:ai-assistant   ← Gemini chat assistant
:feature:voice          ← SpeechRecognizer command engine
:feature:saved-rooms    ← Room persistence & cloud sync
:feature:settings       ← User preferences & theme
```

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android SDK API 26+
- A device or emulator with ARCore support
- Firebase project with Firestore, Auth, Vertex AI, and Storage enabled

### Setup

1. Clone the repository
2. Add your `google-services.json` to `app/`
3. Open in Android Studio
4. Run on a physical ARCore-supported device (emulator AR preview is limited)

```bash
./gradlew assembleDebug
```

## Architecture

Follows **Clean Architecture** with three layers:
- **Presentation**: Compose UI + ViewModels + StateFlow
- **Domain**: Use Cases + Repository interfaces + Domain Models
- **Data**: Room DAOs, Firestore, DataStore, ARCore session

See the full architecture documentation in [implementation_plan.md](docs/implementation_plan.md).

## Development Roadmap

| Milestone | Target | Description |
|---|---|---|
| M0 | Week 2 | Infrastructure complete |
| M1 | Week 4 | Auth & Onboarding |
| M2 | Week 7 | Furniture Catalog |
| M3 | Week 11 | AR Core Engine |
| M4 | Week 13 | Room Planner |
| M5 | Week 16 | AI Assistant |
| M6 | Week 18 | Voice Commands |
| M7 | Week 20 | Cloud Sync |
| M8 | Week 24 | v1.0 Release |

## License

Copyright © 2026 Lumiroom. All rights reserved.
