# Known Issues and Limitations

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

This document tracks known architectural limitations and system issues within the current implementation.

## 1. AR Tracking Drift in Low Light
- **Description**: ARCore relies on visual feature points. In low light or on featureless surfaces (like solid white floors), tracking may drift over time.
- **Workaround**: Encourage users via UI hints to ensure rooms are well-lit before anchoring important furniture.

## 2. Sync Conflicts
- **Description**: Because we use a Last-Write-Wins (LWW) strategy, if two users are modifying the exact same room on different devices simultaneously while offline, the device that connects to the internet last will overwrite the other's changes.
- **Future Scope**: Investigate CRDTs (Conflict-free Replicated Data Types) for true collaborative real-time editing.

## 3. Very Large Rooms OOM
- **Description**: For extremely large floor plans (e.g., massive warehouses), the 2D planner's Canvas rendering might become sluggish due to the massive number of `drawLine` and `drawImage` calls per frame.
- **Workaround**: The current implementation does not implement spatial partitioning (like QuadTrees) for the canvas render loop.

## 4. Cloud Anchor Expiration
- **Description**: Google ARCore Cloud Anchors expire after a set duration (typically 1 day for standard anchors, up to 365 days for persistent anchors). 
- **Limitation**: If a user loads a room after the Cloud Anchor has expired, the furniture will default to the relative `RoomCoordinateSystem` origin rather than snapping to the physical real-world location.
