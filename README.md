# Lumiroom
> **AI-Assisted Mobile AR Furniture Visualization and Interior Planning System**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com/)
[![CI](https://github.com/Vinoth-ai-20/lumiroom/actions/workflows/ci.yml/badge.svg)](https://github.com/Vinoth-ai-20/lumiroom/actions)

Lumiroom is a state-of-the-art Android mobile application that redefines how users plan and visualize interior spaces. By combining **ARCore** spatial tracking with an offline-capable **Voice Command Engine** and **AI-powered room recommendations**, Lumiroom provides a seamless, hands-free design experience.

---

## 📸 Screenshots

| AR Placement | Voice Commands | 2D Planning |
|:---:|:---:|:---:|
| <img src="https://via.placeholder.com/250x500.png?text=AR+Mode" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Voice+Control" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=2D+Canvas" width="200"/> |

---

## 🚀 Features
- **True-to-Scale AR Visualization**: Anchor 3D models seamlessly into your physical environment.
- **Hands-Free Voice Control**: "Place the modern sofa here" - control the entire UI using natural language.
- **Offline-First Architecture**: Build rooms without a network connection; syncs automatically to the cloud when online.
- **AI Recommendations**: Vertex AI integration analyzes your layout and suggests complimentary furniture and layout improvements.
- **Furniture Mega Pack (FMP)**: Access to hundreds of high-quality, optimized 3D GLB assets.

---

## 🛠️ Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **AR Engine**: SceneView & ARCore
- **Architecture**: MVVM, Unidirectional Data Flow (UDF), Clean Architecture
- **Dependency Injection**: Hilt
- **Local Database**: Room (SQLite)
- **Cloud Backend**: Firebase Firestore, Storage, & Authentication
- **Voice Recognition**: Android SpeechRecognizer API
- **AI Backend**: Google Vertex AI

---

## 🏗️ Architecture Overview

Lumiroom uses a modern Android architecture based on the **Repository Pattern** and **Unidirectional Data Flow**.

```mermaid
C4Context
    title High-Level System Architecture
    Person(user, "User")
    System(app, "Lumiroom Android App")
    SystemDb(room, "Local Room DB")
    SystemDb_Ext(firestore, "Firebase Cloud")
    
    Rel(user, app, "Interacts via Touch/Voice")
    Rel(app, room, "Reads/Writes (Source of Truth)")
    Rel(room, firestore, "Background Sync")
```
*For deeper architectural details, see [Architecture Documentation](docs/Architecture.md).*

---

## 📂 Folder Structure

```text
lumiroom/
├── app/                  # Application entry point and DI setup
├── core/                 # Shared logic, databases, analytics, networking
├── feature/              # Independent feature modules (ar, voice, onboarding)
├── docs/                 # ISO/IEEE compliant documentation suite
└── .github/workflows/    # CI/CD pipelines
```

---

## 💻 Installation & Build Guide

### Prerequisites
- **Android Studio Koala** (or newer)
- **JDK 17**
- An ARCore supported Android device (API 29+)

### 1. Firebase Setup
Lumiroom requires Firebase to run.
1. Create a project in the Firebase Console.
2. Download your `google-services.json` file.
3. Place the file inside the `app/` directory (see `google-services.json.example`).

### 2. Building the Project
Clone the repository and build via Gradle:
```bash
git clone https://github.com/Vinoth-ai-20/lumiroom.git
cd lumiroom
./gradlew assembleDebug
```

---

## 🎙️ Voice Commands Guide
Lumiroom features a robust fuzzy-matching voice parser. Activate the microphone and try:
- *"Place a modern vanity here"* (Executes a center-screen raycast)
- *"Rotate the sofa 90 degrees"*
- *"Remove this item"*
- *"Undo"*

---

## 🚧 Known Limitations
- Extremely reflective surfaces may disrupt AR tracking.
- Voice recognition accuracy degrades in noisy environments (a push-to-talk fallback is provided).

---

## 🗺️ Roadmap
See our [Implementation Plan](docs/implementation_plan.md) for full details.
- [x] Phase 1: AR Engine Integration
- [x] Phase 2: Offline-First Database Sync
- [x] Phase 3: Voice Command Engine
- [ ] Phase 4: Vertex AI Room Health Scoring (In Progress)

---

## 🤝 Contributing
We welcome contributions! Please see our [FMP Integration Guide](docs/FMP_Integration_Guide.md) for adding new 3D models to the project.
Ensure all PRs pass the GitHub Actions CI pipeline (Lint & Unit Tests) before requesting a review.

---

## 📜 License
This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements
- [SceneView](https://github.com/SceneView/sceneview-android) for the incredible AR rendering engine.
- IEEE Standard documentation templates.
