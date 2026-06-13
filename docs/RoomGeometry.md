# Room Geometry Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

This document describes how physical room spaces are quantified, stored, and manipulated within Lumiroom across both the 3D AR space and the 2D layout planner.

---

## 1. Plane Detection (ARCore)

When a user launches the AR mode, ARCore's native plane detection algorithm begins mapping the physical environment.

1. **Point Clouds**: Feature points are extracted from the camera feed and depth sensor.
2. **Plane Boundary Generation**: ARCore groups coplanar feature points to form a polygon mesh representing a surface (Floor, Wall, Ceiling).
3. **`PlaneBoundary` Object**: Lumiroom intercepts these meshes and converts them into `PlaneBoundary` objects. These contain an ID, a normal vector (defining vertical vs horizontal), and a 2D polygon footprint.

---

## 2. Wall Entities

While ARCore detects raw planes, the 2D Planner and the logical RoomState rely on `WallEntity` structures to define the strict perimeter of a room.

A `WallEntity` is defined by:
- **`start_x`, `start_y`**: The starting coordinate in the 2D coordinate space.
- **`end_x`, `end_y`**: The ending coordinate in the 2D coordinate space.
- **`thickness`**: The physical thickness of the wall.
- **`height`**: The height from floor to ceiling.

Walls are strictly linear. Curved walls are represented by multiple small `WallEntity` segments.

---

## 3. Corner Points and Snapping

To ensure structural integrity within the 2D Planner, walls are connected via `CornerPointEntity` nodes.

- A `CornerPoint` exists at every junction where two or more walls meet.
- When a user draws a new wall in the 2D planner, the coordinate snaps to the nearest `CornerPoint` within a threshold radius.
- **Continuous Outline**: By traversing from `CornerPoint` to `CornerPoint` along `WallEntity` paths, the system can calculate the exact internal square footage of the room for the `BudgetPlanner` and `RoomScoreEngine`.

---

## 4. Sub-Geometries (Doors and Windows)

Walls can contain sub-geometries:

- **`WindowEntity`**: Defined by a relative offset along the parent wall, a width, and a height off the floor.
- **`DoorEntity`**: Similar to windows but typically extend to the floor and have an opening direction (swing arc) that the 2D planner can visually render.

---

## 5. Coordinate System Transformation

The AR coordinate system is a physical 3D space relative to the device's start position (Origin `0,0,0`).
The 2D Planner uses an arbitrary 2D grid.

The `RoomCoordinateSystem` utility maps between these two spaces. When an AR plane is established as the "Floor", its Y-level becomes `0` on the 2D planner, and X/Z mapped directly to X/Y on the canvas.
