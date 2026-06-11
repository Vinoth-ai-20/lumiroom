# Project File Format

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

Lumiroom synchronizes Room projects to Firestore. This document details the serialization format of the "Lumiroom Project".

## Schema Representation

When `RoomModel` is serialized to Firestore (or exported), it takes the shape of a hierarchical JSON structure.

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "My Living Room",
  "user_id": "firebase_auth_uid_123",
  "created_at": "2026-06-10T12:00:00Z",
  "updated_at": "2026-06-10T14:30:00Z",
  "thumbnail_path": "rooms/123e4567.../thumb.jpg",
  
  "metadata": {
    "budget": 5000.00,
    "style_preference": "Modern Minimalist"
  },
  
  "items": [
    {
      "id": "a1b2c3d4...",
      "catalog_id": "sofa_001",
      "position": { "x": 1.5, "y": 0.0, "z": -2.0 },
      "rotation": { "x": 0.0, "y": 0.707, "z": 0.0, "w": 0.707 },
      "scale": { "x": 1.0, "y": 1.0, "z": 1.0 },
      "cloud_anchor_id": "ua-8732bf9..."
    }
  ],
  
  "walls": [
    {
      "id": "w1...",
      "start": { "x": 0.0, "y": 0.0 },
      "end": { "x": 5.0, "y": 0.0 },
      "thickness": 0.15,
      "height": 2.8
    }
  ]
}
```

## Cloud Anchors
Notice that `cloud_anchor_id` is serialized alongside the placed item. This is crucial for cross-device AR experiences, ensuring the virtual sofa appears exactly where the previous user left it.
