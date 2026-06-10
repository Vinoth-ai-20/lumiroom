# Software Requirements Specification (SRS)
## Lumiroom: AI-Powered AR Interior Design

### 1. Introduction
**1.1 Purpose**
The purpose of this document is to define the requirements and specifications for Lumiroom, an Android application designed to assist users in planning and visualizing interior spaces using Augmented Reality (AR) and Artificial Intelligence (AI).

**1.2 Scope**
Lumiroom allows users to:
- Visualize 3D furniture models in their real-world environment using ARCore.
- Plan room layouts in a 2D canvas editor.
- Sync designs across devices using Firebase Cloud Firestore.
- Receive AI-powered recommendations for furniture compatibility, layout optimization, and room health scoring via Vertex AI.
- Control the application hands-free via an Android SpeechRecognizer voice engine.

### 2. Overall Description
**2.1 Product Perspective**
Lumiroom operates as a standalone Android application relying on Google ARCore for spatial tracking and Firebase for authentication, database synchronization, and AI capabilities.

**2.2 User Characteristics**
- **Homeowners/Renters:** Seeking to visualize new furniture before purchase.
- **Interior Designers:** Utilizing the app as a rapid prototyping tool.

### 3. Functional Requirements
1. **Authentication:** Users shall be able to sign in using Google Auth or Email/Password.
2. **Catalog Browsing:** Users shall be able to browse, filter, and download 3D models (GLB).
3. **AR Placement:** Users shall be able to anchor models to detected planes, translate, rotate, and scale them.
4. **Cloud Sync:** The system shall synchronize local Room Database entities to Firestore in a background queue.
5. **AI Assistant:** Users shall interact with a conversational agent that has context of their current room design.
6. **Voice Commands:** The system shall support voice commands for item placement ("place sofa"), manipulation ("rotate left"), and selection.
7. **Report Export:** The system shall generate a PDF report of the room layout, health score, and estimated cost.

### 4. Non-Functional Requirements
- **Performance:** AR scenes must maintain 60 FPS on supported devices.
- **Offline Resilience:** All core features (except Cloud Sync and Vertex AI) must function without an internet connection.
- **Security:** User data must be isolated via Firestore Security Rules.
