# Plane Detection

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Room Scanning Architecture](RoomScanningArchitecture.md)

Plane detection is the process by which Lumiroom understands the physical constraints of a user's environment to allow realistic scaling and collision of furniture.

## ARCore Capabilities
- **Horizontal Planes**: Floors and ceilings.
- **Vertical Planes**: Walls and partitions.

## Logical Conversion
Once ARCore detects a plane and provides a `com.google.ar.core.Plane` object, Lumiroom extracts its boundary polygon.

1. **Polygon Extraction**: The vertices of the plane's boundary polygon are extracted.
2. **Convex Hull**: Lumiroom simplifies the geometry to a 2D convex hull to represent the floor boundary in the 2D planner.
3. **Filtering**: Planes smaller than `0.5m x 0.5m` are ignored to prevent placing furniture on small clutter objects (like tables) when intending to place them on the floor.
