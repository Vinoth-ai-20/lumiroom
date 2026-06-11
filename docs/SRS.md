<p align="center">
  <img src="../Lumiroom-logo-alpha.png" width="120"/>
</p>

# Software Requirements Specification (SRS)

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Document Standard:** IEEE 830-1998  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: System Architecture](Architecture.md)

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) defines the functional, non-functional, and future requirements for the Lumiroom application. It acts as the definitive baseline for development, testing, and project validation.

### 1.2 Scope
Lumiroom is a state-of-the-art Android mobile application designed to assist users in planning and visualizing interior spaces using Augmented Reality (AR), a 2D Canvas Planner, and Artificial Intelligence (AI).

- **In-Scope**: ARCore tracking, SceneView rendering, 2D Compose Canvas planner, offline-first Voice Commands, Firebase cloud synchronization, conversational AI assistant, and local SQLite persistence.
- **Out-of-Scope**: iOS support, Web interfaces, VR headset support.

---

## 2. Functional Requirements

### FR-01: Augmented Reality (AR) Mode
- **FR-01.01**: The system shall detect horizontal and vertical planes using ARCore.
- **FR-01.02**: The system shall render `GLB` models anchored to detected planes in real-time.
- **FR-01.03**: The system shall allow users to select, move, rotate, and scale 3D items via touch gestures.

### FR-02: 2D Room Planner
- **FR-02.01**: The system shall provide a top-down 2D canvas editor.
- **FR-02.02**: The system shall allow users to drag-and-drop items from a catalog onto the canvas.
- **FR-02.03**: The system shall allow users to draw walls that snap to geometric corner points.
- **FR-02.04**: The system shall provide a minimap for rapid navigation of large canvases.

### FR-03: Shared State & Synchronization
- **FR-03.01**: The system shall instantly synchronize placed items between the 2D planner and AR mode without a database roundtrip.
- **FR-03.02**: The system shall maintain an Undo/Redo history stack for all geometric changes.

### FR-04: Voice & AI Capabilities
- **FR-04.01**: The system shall transcribe English speech via the Android SpeechRecognizer API.
- **FR-04.02**: The system shall map transcripts to predefined intents (Place, Move, Rotate, Delete).
- **FR-04.03**: The system shall provide a chat interface powered by Vertex AI for conversational interior design advice.

### FR-05: Persistence & Cloud Sync
- **FR-05.01**: The system shall auto-save room designs locally to an SQLite database with a debounced trigger.
- **FR-05.02**: The system shall sync local changes to Firebase Firestore in the background when connectivity allows, using a Last-Write-Wins strategy.

---

## 3. Non-Functional Requirements

### 3.1 Performance
- **AR Frame Rate**: Must maintain $\ge$ 30 FPS.
- **2D Planner Render**: Must render $\ge$ 60 FPS utilizing Compose hardware acceleration.
- **Voice Latency**: Command execution $\le$ 500ms post-transcription.

### 3.2 Security
- **Data Encryption**: TLS 1.3 for all Firebase transit. Data-at-rest encrypted via standard Android file system encryption.
- **Access Control**: Users can only read/write room data associated with their Firebase Auth UID.

### 3.3 Reliability
- **Offline-First**: Users must be able to create, edit, and save rooms entirely offline. The system guarantees 99.9% uptime for core layout features regardless of network state.

### 3.4 Usability
- UI built entirely in Jetpack Compose following Material Design 3 specifications.

---

## 4. Future Requirements (Out of Scope v2.0)

- **FUT-01**: LiDAR Scanning support to automatically generate 2D floor plans from a single room spin.
- **FUT-02**: Collaborative real-time editing (multiplayer mode) using CRDTs.
- **FUT-03**: iOS / ARKit cross-platform support via Kotlin Multiplatform.
- **FUT-04**: E-commerce checkout integration directly from the Furniture Catalog.
