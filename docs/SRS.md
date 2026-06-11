<p align="center">
  <img src="../Lumiroom-logo-alpha.png" width="120"/>
</p>

# Software Requirements Specification (SRS)

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Document Standard:** IEEE 830-1998  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: System Architecture](Architecture.md)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) defines the functional, non-functional, and interface requirements for the Lumiroom application. It acts as the definitive baseline for development, testing, and project validation.

### 1.2 Scope

Lumiroom is a state-of-the-art Android mobile application designed to assist users in planning and visualizing interior spaces using Augmented Reality (AR) and Artificial Intelligence (AI).

- **In-Scope**: ARCore tracking, SceneView rendering, offline-first Voice Commands, Firebase cloud synchronization, and local SQLite persistence.
- **Out-of-Scope**: iOS support, Web interfaces, VR headset support.

### 1.3 Definitions and Abbreviations

See the project [Glossary](Glossary.md) for full definitions of terms like ARCore, GLB, FMP, UDF, and Hilt.

### 1.4 References

- IEEE Std 830-1998, Recommended Practice for Software Requirements Specifications.
- [C4 Architecture Models](C4Architecture.md)
- [Database Design](DatabaseDesign.md)

---

## 2. Overall Description

### 2.1 Product Perspective

Lumiroom is an independent mobile application. It interfaces locally with the Android OS (Camera, Microphone, Sensors) and remotely with Firebase Cloud Services.

### 2.2 Product Functions

1. **AR Visualization**: Anchor and manipulate high-fidelity 3D models.
2. **Voice Control**: Command execution through natural language processing.
3. **Project Persistence**: Save layouts locally and cloud-sync them automatically.
4. **AI Recommendations**: Room health scoring and furniture compatibility checks.

### 2.3 User Classes

1. **Consumer**: Requires intuitive onboarding and error-forgiving controls.
2. **Designer**: Requires precision placement tools, measurement grids, and bulk selection features.

### 2.4 Operating Environment

- **Platform**: Android 10 (API 29) minimum.
- **Dependencies**: Google Play Services for AR (ARCore).

### 2.5 Constraints

- **Memory**: AR applications consume massive amounts of VRAM. Hard limit of 50,000 polygons per model.
- **Offline Limitations**: Initial catalog downloads and AI recommendation features strictly require internet access.

### 2.6 Assumptions and Dependencies

- The user's device camera functions correctly in standard indoor lighting conditions.

---

## 3. Functional Requirements

### FR-01: AR Placement System

- **FR-01.01**: The system shall detect planes (horizontal/vertical) using ARCore.
- **FR-01.02**: The system shall render `GLB` models anchored to detected planes.

### FR-02: Voice Command Engine

- **FR-02.01**: The system shall transcribe English speech via the Android SpeechRecognizer API.
- **FR-02.02**: The system shall map transcripts to predefined intents (Place, Move, Rotate, Delete).

### FR-03: Cloud Sync

- **FR-03.01**: The system shall write all state changes to local Room SQLite databases instantly.
- **FR-03.02**: The system shall sync local changes to Firebase Firestore silently in the background when connectivity allows.

---

## 4. Non-Functional Requirements

### 4.1 Performance

- **AR Frame Rate**: Must maintain $\ge$ 30 FPS.
- **Voice Latency**: Command execution $\le$ 500ms post-transcription.

### 4.2 Security

- **Data Encryption**: TLS 1.3 for all Firebase transit. Data-at-rest encrypted via standard Android file system encryption.
- For detailed specs, see [Security Architecture](SecurityArchitecture.md).

### 4.3 Reliability

- Offline-first design guarantees 99.9% uptime for core AR placement features regardless of network state.

### 4.4 Usability

- UI built entirely in Jetpack Compose following Material Design 3 specifications.

---

## 5. External Interface Requirements

### 5.1 User Interfaces

- Intuitive bottom sheets for catalog browsing.
- Floating Action Buttons for quick mic access.

### 5.2 Software Interfaces

- **Firebase SDK**: Version 32.x.
- **ARCore SDK**: Version 1.41.x.

---

## 6. Use Cases

See [UseCases.md](UseCases.md) for full use case specifications and Actor diagrams.

### 6.1 Core Use Case: Voice Placement

| Use Case ID | UC-01 |
|-------------|-------|
| **Name**    | Voice Furniture Placement |
| **Actor**   | User |
| **Description** | User places an object using a vocal command. |
| **Preconditions** | App open, AR active, Mic permitted. |
| **Postconditions** | Object spawned in AR space. |

---

## 7. Acceptance Criteria

- 100% of P0 functional requirements pass automated CI instrumentation tests.
- Zero memory leaks detected by LeakCanary during a 10-minute active AR session.
