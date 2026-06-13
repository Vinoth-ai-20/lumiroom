# Future Scope

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md)

---

## 1. Upcoming Features (Roadmap)

### 1.1 Multi-User AR Collaboration

- **Description**: Allow two users in the same physical room to view and edit the same AR layout simultaneously using ARCore Cloud Anchors.
- **Dependency**: Requires migration from standard Room Database to realtime WebSockets or high-frequency Firestore Snapshots.

### 1.2 Generative AI Furniture Creation

- **Description**: Integrate Google Vertex AI 3D generation to allow users to speak a prompt ("Create a rustic oak coffee table") and instantly spawn a custom generated `.glb` model into their room.

### 1.3 Material and Texture Customization

- **Description**: Allow users to tap on an AR model and swap its materials (e.g., change a sofa from leather to velvet) dynamically without downloading entirely new 3D meshes.

### 1.4 Dynamic Lighting Estimation Expansion

- **Description**: Current lighting estimation relies on ARCore's ambient environmental spherical harmonics. Future scope includes placing virtual light sources (e.g., AR Lamps) that cast real-time shadows onto the physical floor.

### 1.5 LiDAR Floorplan Export

- **Description**: For devices equipped with LiDAR (e.g., certain Samsung tablets), automatically generate and export an exact 2D AutoCAD/PDF blueprint of the physical room geometry.
