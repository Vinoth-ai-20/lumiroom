# Security Architecture

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: API Reference](APIReference.md)

---

## 1. Authentication Flow Diagram

All users must be authenticated anonymously or via Google Sign-In to access cloud features.

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Auth as Firebase Auth
    participant DB as Firestore
    
    User->>App: Launch App
    App->>Auth: checkCurrentUser()
    alt User is NULL
        App->>Auth: signInAnonymously()
        Auth-->>App: Return UserUID
    end
    App->>DB: queryRooms(UserUID)
    DB-->>App: User Data
```

## 2. Threat Model & Trust Boundaries

| Threat | Vulnerability | Mitigation Strategy |
|--------|--------------|---------------------|
| Man-in-the-Middle (MitM) | Intercepting Firestore traffic | Handled natively by Firebase via TLS 1.3 pinning. |
| Insecure Local Storage | Reading cached Room DB | Room database is stored in Android's sandboxed `data/data/com.lumiroom.app` directory. |
| Unauthorized Cloud Reads | Malicious API calls | Firestore Security Rules strictly enforce `request.auth.uid == resource.data.user_id`. |

## 3. Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/rooms/{roomId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /items/{itemId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    match /catalog/{itemId} {
      allow read: if true; // Public catalog
      allow write: if false; // Only admins can edit catalog
    }
  }
}
```
