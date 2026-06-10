# Deployment Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Security Architecture](SecurityArchitecture.md)

---

## 1. Physical Deployment Architecture

Maps the physical hardware and software execution environments.

```mermaid
C4Deployment
    title Deployment Architecture for Lumiroom
    
    Deployment_Node(mob, "User Mobile Device", "Android Smartphone") {
        Deployment_Node(os, "Android OS", "Android 10+") {
            Container(app, "Lumiroom APK", "Kotlin", "Provides AR UI and Logic")
            ContainerDb(db, "Room DB", "SQLite", "Local Caching")
        }
    }
    
    Deployment_Node(gcp, "Google Cloud Platform", "PaaS") {
        Deployment_Node(fb, "Firebase", "Backend-as-a-Service") {
            Container(fs, "Firestore", "NoSQL", "Cloud Sync")
        }
    }
    
    Rel(app, fs, "Reads/Writes via wss/HTTPS")
```

## 2. CI/CD Deployment Pipeline

Maps the Github Actions runner nodes for continuous deployment.

```mermaid
flowchart LR
    Dev[Developer Workstation] -->|git push| GitHub[GitHub Actions Runner]
    GitHub -->|./gradlew lint| Sonar[Code Quality]
    GitHub -->|./gradlew test| Test[JUnit JVM]
    GitHub -->|./gradlew assembleRelease| APK[Signed APK/AAB]
    APK -->|Deploy| Play[Google Play Console]
    Play -->|Rollout| Users[End Users]
```
