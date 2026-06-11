# Performance Analysis

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Glossary](Glossary.md)

---

## 1. Benchmarking Hardware

Tests executed on a **Samsung Galaxy S23 Ultra** (Snapdragon 8 Gen 2, 12GB RAM) running Android 14.

## 2. Rendering Constraints

- **Target Framerate**: 30 FPS minimum, 60 FPS target.
- **Maximum Polygon Count per Model**: 50,000 tris.
- **Maximum Poly Budget (Total Scene)**: 500,000 tris (Approx. 10 complex models).
- **Texture Limits**: 1024x1024 compressed via KTX2.

## 3. Memory Profiling

| Operation | Peak Memory (RAM) | Execution Time |
|-----------|-------------------|----------------|
| App Startup | 85 MB | 1.2s |
| SceneView Initialization | 210 MB | 2.5s |
| Loading 1st GLB Model | 280 MB | 800ms |
| Loading 10th GLB Model | 450 MB | 900ms |
| Voice Command Processing | +15 MB | 450ms |
| Firebase Background Sync | +10 MB | ~150ms |

## 4. OOM (Out-of-Memory) Prevention

1. **Model Pooling**: SceneView implements internal GLB caching. Placing 5 identical chairs uses only 1 memory footprint.
2. **Lifecycle Unloading**: Models are evicted from memory when they are deleted from the Room DB or hidden in the UI.
