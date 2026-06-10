<p align="center">
  <img src="Lumiroom-logo-alpha.png" width="180"/>
</p>

<h1 align="center">
Lumiroom
</h1>

<p align="center">
AI-Assisted Mobile AR Furniture Visualization and Interior Planning System
</p>

<p align="center">
Visualize Furniture Before It Arrives
</p>

<p align="center">
  <img src="https://img.shields.io/badge/kotlin-2.0.0-blue.svg?logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Android-green.svg?logo=android" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/ARCore-Supported-orange.svg?logo=google" alt="ARCore"/>
  <img src="https://img.shields.io/badge/SceneView-3D-blue.svg" alt="SceneView"/>
  <img src="https://img.shields.io/badge/Firebase-Backend-yellow.svg?logo=firebase" alt="Firebase"/>
  <img src="https://img.shields.io/badge/Material_3-UI-purple.svg?logo=materialdesign" alt="Material 3"/>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"/></a>
</p>

Lumiroom is an enterprise-grade Android application that redefines how users plan and visualize interior spaces. By combining **ARCore** spatial tracking with an offline-capable **Voice Command Engine** and **AI-powered room recommendations**, Lumiroom provides a seamless, hands-free interior design experience.

---

## 📸 Screenshots

| AR Placement | Voice Commands | 2D Planning |
|:---:|:---:|:---:|
| <img src="https://placehold.co/250x500?text=AR+Mode" width="200"/> | <img src="https://placehold.co/250x500?text=Voice+Control" width="200"/> | <img src="https://placehold.co/250x500?text=2D+Canvas" width="200"/> |

---

## 🚀 Features
- **True-to-Scale AR Visualization**: Anchor 3D models seamlessly into your physical environment.
- **Hands-Free Voice Control**: Control the entire UI using natural language (e.g., "Place the modern sofa here").
- **Offline-First Architecture**: Build rooms without a network connection; syncs automatically to the cloud when online.
- **AI Recommendations**: Vertex AI analyzes your layout and suggests complimentary furniture.
- **Furniture Mega Pack (FMP)**: Access to hundreds of high-quality, optimized 3D GLB assets.

---

## 📚 Documentation Directory

Lumiroom features a complete, international-standard documentation suite adhering to ISO/IEC/IEEE 42010 and IEEE 830.

| Document | Description |
|-----------|-------------|
| [Software Requirements Specification](docs/SRS.md) | IEEE 830 functional and non-functional requirements |
| [System Architecture](docs/Architecture.md) | ISO 42010 Architecture, Principles, and Patterns |
| [C4 Architecture Models](docs/C4Architecture.md) | System Context, Container, Component, and Deployment diagrams |
| [UML Diagrams Index](docs/UMLDiagrams.md) | Central hub for all UML models |
| [Database Design](docs/DatabaseDesign.md) | ER diagrams, Room schemas, and Firestore sync strategy |
| [Asset Pipeline](docs/AssetPipeline.md) | 3D Furniture asset optimization workflow |
| [Deployment Guide](docs/DeploymentGuide.md) | Build, signing, and CI/CD deployment |
| [FMP Integration Guide](docs/FMP_Integration_Guide.md) | Asset extraction and preprocessing process |
| [Implementation Plan](docs/implementation_plan.md) | Agile roadmap, WBS, and Gantt charts |
| [Testing Report](docs/TestingReport.md) | QA testing strategy, matrices, and coverage |
| [Use Cases](docs/UseCases.md) | Actor diagrams and use case narratives |
| [Sequence Diagrams](docs/SequenceDiagrams.md) | Time-ordered flow of operations |
| [Class Diagrams](docs/ClassDiagrams.md) | Domain and Data layer object structures |
| [ER Diagrams](docs/ERDiagrams.md) | Database Entity-Relationship diagrams |
| [Data Flow Diagrams](docs/DataFlowDiagrams.md) | System data processing pipelines |
| [State Machine Diagrams](docs/StateMachineDiagrams.md) | Lifecycle and application states |
| [Activity Diagrams](docs/ActivityDiagrams.md) | Process and logic workflows |
| [Deployment Diagrams](docs/DeploymentDiagrams.md) | Physical node distributions |
| [Security Architecture](docs/SecurityArchitecture.md) | Threat models, Trust boundaries, Auth flows |
| [API Reference](docs/APIReference.md) | Repository, ViewModel, and Service APIs |
| [Coding Standards](docs/CodingStandards.md) | Kotlin and Compose formatting guidelines |
| [Risk Assessment](docs/RiskAssessment.md) | Risk matrices and mitigation strategies |
| [Performance Analysis](docs/PerformanceAnalysis.md) | Benchmarking, rendering FPS, and memory limits |
| [Glossary](docs/Glossary.md) | Definitions and terminology |
| [Contributing Guide](docs/ContributingGuide.md) | Workflows for open-source contributors |
| [Changelog](docs/Changelog.md) | Version history |
| [Future Scope](docs/FutureScope.md) | Roadmap for future features |

---

## 🏗️ Architecture Overview
Lumiroom uses a modern Android architecture based on **Unidirectional Data Flow (UDF)** and the **Repository Pattern**.

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

---

## 🛠️ Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **AR Engine**: SceneView & ARCore
- **Dependency Injection**: Hilt
- **Local Database**: Room (SQLite) & DataStore
- **Cloud Backend**: Firebase Firestore, Storage, & Authentication
- **Voice Recognition**: Android SpeechRecognizer API
- **AI Backend**: Google Vertex AI
- **Concurrency**: Kotlin Coroutines & Flow

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

### Firebase Setup
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable Firestore, Storage, and Authentication.
3. Replace `app/google-services.json.example` with your real `google-services.json`.

### Building the Project
```bash
git clone https://github.com/Vinoth-ai-20/lumiroom.git
cd lumiroom
./gradlew assembleDebug
```

---

## 🎙️ Voice Commands Guide
Activate the microphone and speak natural commands:
- *"Place a modern vanity here"* (Executes center-screen raycast)
- *"Rotate the sofa 90 degrees"*
- *"Remove this item"*
- *"Undo"*

---

## 📱 Supported Devices
Lumiroom requires a device with a Time-of-Flight (ToF) sensor or sufficient camera capabilities to support **Google ARCore Depth API**. Standard support begins at devices equivalent to the Google Pixel 4 / Samsung Galaxy S10 and newer.

---

## 🗺️ Roadmap
Please see our detailed [Implementation Plan](docs/implementation_plan.md) and [Future Scope](docs/FutureScope.md) for full roadmap details.

---

## 🤝 Contributing
We welcome contributions! Please see our [Contributing Guide](docs/ContributingGuide.md) and [Coding Standards](docs/CodingStandards.md) before submitting a Pull Request.

---

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements
- [SceneView](https://github.com/SceneView/sceneview-android) for the incredible AR rendering engine.
- [Google ARCore](https://developers.google.com/ar) for spatial tracking.

---

## 📚 References
- ISO/IEC/IEEE 42010:2011, Systems and software engineering — Architecture description.
- IEEE Std 830-1998, IEEE Recommended Practice for Software Requirements Specifications.
