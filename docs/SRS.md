# Software Requirements Specification (SRS)

**Project Title:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

---

## 1. Introduction

### 1.1 Purpose
The purpose of this Software Requirements Specification (SRS) is to provide a complete and comprehensive description of the Lumiroom application. It defines the system's purpose, scope, features, and constraints. This document is intended for project stakeholders, developers, software architects, and quality assurance engineers.

### 1.2 Scope
Lumiroom is a state-of-the-art Android mobile application designed to assist users in planning and visualizing interior spaces using Augmented Reality (AR) and Artificial Intelligence (AI). 
The core functionalities include:
- Visualizing 3D furniture models (GLB format) in real-world environments using ARCore.
- Controlling object manipulation via an offline-capable Voice Command engine.
- AI-assisted design, offering room health scoring and compatibility recommendations.
- A seamless database synchronization backend allowing users to persist AR layouts across sessions via Firebase and Room.

### 1.3 Definitions and Abbreviations
- **ARCore**: Google’s platform for building augmented reality experiences.
- **GLB**: The binary file format representation of 3D models saved in the GL Transmission Format (glTF).
- **FMP**: Furniture Mega Pack, the primary repository for the system's 3D assets.
- **SRS**: Software Requirements Specification.
- **UI / UX**: User Interface / User Experience.

### 1.4 References
- IEEE Std 830-1998, Recommended Practice for Software Requirements Specifications.
- Google ARCore Documentation.
- Android Jetpack Compose Documentation.

---

## 2. Overall Description

### 2.1 Product Perspective
Lumiroom operates as a standalone mobile application for Android devices that support ARCore. It utilizes a cloud backend (Firebase Firestore) for authentication, telemetry, and cross-device synchronization, and relies on an offline-first architecture powered by SQLite (Room Database) to ensure robust performance in poor network conditions.

### 2.2 Product Functions
- **AR Visualization**: Anchor, scale, and rotate high-fidelity 3D models in a live camera feed.
- **Voice Control**: Command placement and manipulation of objects through natural language ("Place the modern vanity here").
- **Asset Cataloging**: Search and browse thousands of localized and remote 3D models.
- **Project Persistence**: Save room layouts and states locally and to the cloud.

### 2.3 User Classes
- **Homeowners/Consumers**: Utilizing the app to preview furniture purchases. They require an intuitive UI and robust voice controls.
- **Interior Designers**: Professionals using the app for rapid prototyping. They require precision tools (measurement, grouping).

### 2.4 Operating Environment
- **OS**: Android 10 (API Level 29) or higher.
- **Hardware**: ARCore-supported Android device with a minimum of 4GB RAM.
- **Network**: Internet connection required for initial asset downloads and cloud sync; functional offline for previously downloaded assets.

### 2.5 Assumptions and Dependencies
- The device camera is functional and unobstructed.
- The environment has adequate lighting for planar detection by ARCore.
- The user grants microphone and camera permissions.

---

## 3. Functional Requirements

### FR1: AR Placement and Manipulation
- **FR1.1**: The system shall detect horizontal and vertical planes using ARCore.
- **FR1.2**: The user shall be able to place a 3D GLB model onto a detected plane.
- **FR1.3**: The user shall be able to translate, rotate, and uniformly scale placed objects.
- **FR1.4**: The system shall restrict object scaling beyond a 300% upper limit and a 10% lower limit to prevent rendering issues.

### FR2: Voice Interaction System
- **FR2.1**: The system shall continuously transcribe user speech when Voice Mode is active.
- **FR2.2**: The system shall execute fuzzy matching against the Furniture Catalog based on transcriptions.
- **FR2.3**: The system shall support context-aware spatial commands (e.g., executing a center-screen raycast if the command includes the word "here").

### FR3: Cloud and Local Persistence
- **FR3.1**: The system shall save the current positional matrix of all placed items locally via Room Database.
- **FR3.2**: The system shall automatically background-sync local saves to Firebase Firestore when a network connection is established.

---

## 4. Non-Functional Requirements

### 4.1 Performance Requirements
- **Rendering**: The application must maintain a steady 30 FPS minimum during active AR tracking to prevent motion sickness.
- **Voice Latency**: The voice command parser must react and dispatch AR view events within 500ms of transcription completion.

### 4.2 Security Requirements
- All cloud communications shall be encrypted via TLS 1.3.
- Firebase security rules must enforce that users can only read/write their own UID-scoped documents.

### 4.3 Reliability & Availability
- The application shall implement an offline-first architecture ensuring 100% of core AR features function without network connectivity (assuming assets are cached).

### 4.4 Usability
- The UI shall adhere to Material Design 3 guidelines.
- Interactive AR elements shall display visual feedback (e.g., selection rings, ghost previews) to guide user interaction.

---

## 5. External Interface Requirements

### 5.1 User Interfaces
- Developed entirely in Jetpack Compose.
- Must support both landscape and portrait orientations without losing access to the primary toolbar.

### 5.2 Software Interfaces
- **Firebase SDK**: Authentication and Firestore.
- **Android Speech API**: For continuous voice transcription.
- **SceneView / ARCore**: The 3D rendering and tracking engine.

---

## 6. Use Cases

### 6.1 Use Case Diagram
```mermaid
usecaseDiagram
    actor "User" as user
    actor "ARCore System" as arcore
    actor "Speech API" as speech
    
    user --> (Browse Catalog)
    user --> (Place Furniture)
    user --> (Voice Command)
    
    (Place Furniture) .> (Detect Planes) : include
    (Detect Planes) <-- arcore
    
    (Voice Command) .> (Transcribe Speech) : include
    (Transcribe Speech) <-- speech
```

### 6.2 Primary Use Case: Place Furniture via Voice
- **Primary Actor**: User
- **Preconditions**: The app is open, AR camera is active, microphone permission granted.
- **Main Flow**:
  1. User says "Place the modern sofa here".
  2. Speech API transcribes the text.
  3. System parses intent ("Place"), object ("modern sofa"), and spatial context ("here").
  4. System executes a center-screen raycast.
  5. If raycast hits a plane, the system spawns the sofa model.
- **Postconditions**: The 3D model is visible and anchored to the real world.
- **Exception Flow**: If the raycast misses a plane, the system prompts the user to "Tap a detected plane to place the object."
