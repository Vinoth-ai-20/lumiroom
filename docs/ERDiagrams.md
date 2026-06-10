# ER Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Data Flow Diagrams](DataFlowDiagrams.md)

---

## 1. Local Database Schema (Room SQLite)

```mermaid
erDiagram
    USERS ||--o{ ROOM_DESIGNS : "creates"
    ROOM_DESIGNS ||--o{ PLACED_ITEMS : "contains"
    FURNITURE_CATALOG ||--o{ PLACED_ITEMS : "templates"

    USERS {
        string id PK
        string email
        string display_name
    }

    ROOM_DESIGNS {
        string id PK
        string user_id FK
        string name
        int updated_at
    }

    FURNITURE_CATALOG {
        string id PK
        string name
        string category
        string glb_uri
        int polygon_count
    }

    PLACED_ITEMS {
        string id PK
        string room_id FK
        string catalog_id FK
        float pos_x
        float pos_y
        float pos_z
        float rot_x
        float rot_y
        float rot_z
        float rot_w
    }
```

## 2. Constraints & Indexes
- Foreign key constraints heavily cascade on delete (e.g., deleting a `ROOM_DESIGN` cascades deletion to all associated `PLACED_ITEMS`).
- Composite index on `(room_id, catalog_id)` optimizes scene loading performance.
