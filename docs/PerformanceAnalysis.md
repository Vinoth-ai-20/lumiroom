# Performance Analysis

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: Glossary](Glossary.md)

---

## 1. Benchmarking Hardware

Tests executed on a **Samsung Galaxy S23 Ultra** (Snapdragon 8 Gen 2, 12GB RAM) running Android 14.

## 2. Rendering Constraints & FPS

### AR Mode (Native 3D Pipeline)
- **Target Framerate**: 30 FPS minimum, 60 FPS target.
- **Maximum Polygon Count per Model**: 50,000 tris.
- **Maximum Poly Budget (Total Scene)**: 500,000 tris (Approx. 10 complex models).
- **Texture Limits**: 1024x1024 compressed via KTX2.

### 2D Planner (Compose Canvas Pipeline)
- **Target Framerate**: 60 FPS / 120 FPS on supported displays.
- **Recomposition Count**: Due to `StateFlow` updates and modifier usage, dragging a furniture item triggers recomposition on exactly *one* Composable (the parent Canvas). Avoids massive recomposition trees.

## 3. Large Room Scenarios

A "Large Room" is defined as a project containing >50 `PlacedItemEntity` objects and >20 `WallEntity` segments.

**Optimization Strategies:**
- **2D Planner**: The Compose Canvas utilizes spatial clipping. Objects outside the current `CameraState` viewport are mathematically omitted from the `drawImage` / `drawRect` call loop.
- **AR View**: SceneView frustum culling natively prevents rendering objects behind the camera.

## 4. Memory Profiling

| Operation | Peak Memory (RAM) | Execution Time |
|-----------|-------------------|----------------|
| App Startup | 85 MB | 1.2s |
| SceneView Initialization | 210 MB | 2.5s |
| Loading 1st GLB Model | 280 MB | 800ms |
| Loading 10th GLB Model | 450 MB | 900ms |
| Voice Command Processing | +15 MB | 450ms |
| Local DB UPSERT (Debounced)| +5 MB | ~15ms |
| Firebase Background Sync | +10 MB | ~150ms |

## 5. OOM (Out-of-Memory) Prevention

1. **Model Pooling**: SceneView implements internal GLB caching. Placing 5 identical chairs uses only 1 memory footprint.
2. **History Capping**: `UndoRedoManager` is capped at 50 snapshots of the `RoomState`.
3. **Image Caching**: Coil is used for 2D catalog thumbnails, strictly bound to a 20% max-memory footprint.
