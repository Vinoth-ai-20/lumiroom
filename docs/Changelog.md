# Changelog

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to README](../README.md) | [Next: Future Scope](FutureScope.md)

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
