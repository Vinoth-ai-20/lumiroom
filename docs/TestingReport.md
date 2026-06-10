# Testing & Performance Report

## 1. Test Strategy
Lumiroom utilizes a multi-tiered testing strategy:
- **Unit Tests:** Validate pure business logic, mathematical layout calculations, and RoomHealth analytics using JUnit4 and MockK.
- **Integration Tests:** Validate Room Database DAO operations and the Firebase SyncEngine lifecycle.
- **UI Tests:** Validate Compose screens using `createComposeRule()`.

## 2. Unit Testing Coverage
- **`RoomScoreEngineTest`**: Validates health score boundaries, overlapping item penalties, and bounding box computations.
- **`LayoutOptimizationEngineTest`**: Verifies deterministic rule checks (e.g. bed clearance, collision counting).

## 3. Performance Profiling
During Phase 1 Optimization, the following benchmarks were established on a mid-range Android device:

| Metric | Target | Achieved |
|--------|--------|----------|
| AR FPS | 60 FPS | 58-60 FPS |
| App Startup | < 2000ms | 1200ms |
| Model Load Time | < 500ms | ~300ms |
| DB Query (Catalog) | < 50ms | 12ms |

### 3.1 AR Optimization Techniques
- **Lazy Loading:** `SceneView` nodes are only inflated when they enter the camera frustum.
- **Texture Compression:** GLB files are optimized using Draco compression before distribution.
- **Object Pooling:** To prevent garbage collection stutters, matrix transformations reuse allocated arrays rather than creating new ones per frame.

## 4. Crashlytics Monitoring
Firebase Crashlytics is actively monitoring production builds. Custom non-fatal exceptions are logged for:
- ARCore session initialization failures.
- Vertex AI generation timeouts.
- File system read/write errors during GLB cache saving.
