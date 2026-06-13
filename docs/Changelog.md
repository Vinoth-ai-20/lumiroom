# Changelog

## [1.1.2] - 2026-06-13

### Fixed
- **Firebase App Check Quirk**: Fixed an issue where the AI Assistant failed to authenticate because the Android AppCheck SDK enforced an internally cached debug token instead of reading from `strings.xml`. Resolved by manually syncing the generated token in the Firebase Console.
- **Filament Rendering Silence**: Resolved an issue where 3D models were completely invisible in AR. The `ModelLoader` coroutine was silently swallowing exceptions due to a malformed `file:///android_asset/` URI that incorrectly included a redundant `models/` prefix.

### Changed
- **Database Diagnostic Cleanup**: Purged leftover diagnostic database dumps (`db.b64`, `db.sqlite`) from the project root to maintain a clean workspace.

## [1.1.1] - 2026-06-13

### Fixed
- **AR Placement Bug**: Fixed an issue where tapping to place furniture in AR failed due to `RoomModel` not initializing when opened directly from the catalog.
- **AR Anchor Syncing**: AR placement anchor data (`PoseData`) is now correctly saved into `RoomState`, preventing drift or placement failures.
- **2D/AR Axis Sync**: Corrected the axis mapping bug where height and depth axes were swapped when generating generic 3D objects from 2D coordinates.
- **Asset Filtering**: Normalised the dynamic asset discovery string conversion so that `livingroom` correctly populates under the "Living Room" catalog filter.
- **App Check Token**: Enforced the `DebugAppCheckProviderFactory` initialization when running debug variants, restoring AI assistant functionality.

### Changed
- **Documentation**: Synchronized all major architecture documents to reflect the corrected AppCheck, Axis synchronization, and AR tracking state logic.


## [1.1.0] - 2026-06-12

### Added
- **Dynamic Asset Discovery**: Replaced hardcoded catalog JSON with an automated asset parser that crawls `/assets/models/`.
- **Indian Market Pricing**: Embedded an algorithmic pricing strategy bounded by realistic Indian Rupee (₹) limits.

### Changed
- **Database Architecture**: Bumped `LumiroomDatabase` version to 10 and triggered a destructive migration to purge obsolete catalog and project entries.


## [1.0.1] - 2026-06-12

### Fixed
- **AR Rotation Bug**: Object rotation is no longer constrained to [-90, 90]. Rotations are now absolutely tracked in `RoomState`, preventing gimbal lock jumps when converted through Quaternions.
- **AR Duplication Bug**: Fixed an issue where moving existing AR items duplicated them on the next load. AR Anchor detachments and re-assignments are now correctly synced back to the `RoomModel.anchors`.
- **Firebase App Check**: Fixed AI Assistant Firebase access by enforcing the `PlayIntegrityAppCheckProviderFactory` for all variants and enabling auto token refresh.

### Changed
- **2D Planner UI**: Replaced text buttons with scalable icon buttons (`Refresh`, `Clear`, `Add`, `Delete`) in the furniture toolbar. Removed the redundant 'Remove' selection mode.
- **About Page**: Updated Settings About screen to include Academic Project Information (Vinoth M, Sri Krishna Engineering College).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to README](../README.md) | [Next: Future Scope](FutureScope.md)

---


## [2.1.0] - 2026-06-12

### Added
- **Coordinate Synchronization**: Resolved discrepancy between 2D planner (centimeters) and AR scene (meters). `RoomStateManager` and database entities now standardize on meters for all properties (`positionX`, `positionY`, `positionZ`, `startX`, `endX`, etc.), while the 2D planner UI maps to and from centimeters for rendering.
- **Total Price Estimate**: Added a total price estimate overlay to the 2D Planner UI, formatting in Indian Rupees (₹).
- **Home Page Filters**: Added `Room Type` categorization and filtering on the Catalog Screen.
- **Price Synchronization**: Standardized price data structure in `furniture_seed.json` with accurate INR prices. Added `room_type` metadata.

### Fixed
- Fixed bug where furniture added in the 2D planner would not appear in the AR scene due to being out-of-bounds (interpreting cm as meters).
- Restored AR scene stability when transitioning from the 2D Planner.

---

## [2.0.0] - 2026-06-11

### Added
- **Unified State Architecture**: Introduced `RoomStateManager` in `core:domain` to act as the single source of truth for both AR and 2D Planners.
- **2D Room Planner**: Added Compose Canvas-based top-down room editor with Wall drawing and Furniture snapping.
- **Minimap Support**: Added Minimap feature to the 2D Canvas for easy navigation of large floor plans.
- **Snapshot Undo/Redo**: Implemented robust history tracking capped at 50 states utilizing Kotlin immutable data classes.
- **Debounced Save**: Added `LayoutPersistenceManager` to handle offline-first automatic saves seamlessly without locking the UI thread.
- **Massive Documentation Update**: Refactored the entire `docs/` suite to reflect the new Shared State multi-modal architecture.

### Changed
- Migrated ViewModels to observe `RoomStateManager` via `StateFlow` rather than holding independent lists of objects.
- Modified Room DB Schema (`DatabaseDesign.md`) to include `WallEntity`, `CornerPointEntity`, and generic `AnchorEntity` to support unified saving.

---

## [1.0.0] - 2026-06-10

### Added
- Mega Documentation suite encompassing 28 ISO/IEEE compliant markdown files.
- Fully rewritten `README.md` with C4 Context diagram.
- CI/CD workflow `.github/workflows/ci.yml` stabilized and lint-checked.
- Implemented `Icons.AutoMirrored.Filled` to resolve Jetpack Compose deprecations.

### Fixed
- Fixed bug where `google-services.json` prevented CI builds by providing a mock `google-services.json.example`.
- Fixed out-of-memory errors by clearing out multi-gigabyte `.hprof` memory dumps from the repository history.

---

## [0.5.0] - 2026-01-20

### Added
- Voice Command Parser using fuzzy logic.
- Offline-first Room Database architecture with automatic Firestore synchronization.

### Fixed
- SceneView memory leaks during `onPause()`.

---

## [0.1.0] - 2025-11-30

### Added
- Initial project scaffolding.
- Hilt Dependency Injection.
- Jetpack Navigation Compose setup.
- Basic AR Plane Detection using SceneView.


## Asset-Driven Catalog Architecture (v12)
The furniture catalog is now completely dynamically generated from local assets.
No manual registration, hardcoded arrays, or JSON seeding is required.
During the application startup (specifically database creation), the system automatically scans the assets/models/ and assets/thumbnails/ directories.
It uses the naming convention roomType_category_variant.glb (e.g. bathroom_bathtub_01.glb) to dynamically generate metadata, categories, pricing, and tags. This forms the single source of truth for the entire application catalog, powering search, filters, and AR persistence.
