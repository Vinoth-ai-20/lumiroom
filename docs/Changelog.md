# Changelog

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Future Scope](FutureScope.md)

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
