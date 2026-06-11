<p align="center">
  <img src="../Lumiroom-logo-alpha.png" width="120"/>
</p>

# QA and Testing Strategy Report

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 2.0  

[⬅ Back to README](../README.md) | [Next: Use Cases](UseCases.md)

---

## 1. Testing Strategy

Lumiroom follows a Test-Driven Development (TDD) approach where possible, augmented by rigorous manual QA for the Augmented Reality modules and Compose UI snapshot testing.

### 1.1 Testing Pyramid

- **Unit Tests (70%)**: Run via JUnit 4 on the local JVM. Targets `RoomStateManager`, Use Cases, Voice Parsers, and Room Analytics Engines. MockK is used for mocking Repositories.
- **Integration/Instrumentation Tests (20%)**: Run via AndroidX Test on emulators/devices. Targets Room Database schema migrations, `SyncManager` offline buffering, and multi-module dependency graphs.
- **Manual/AR Testing (10%)**: Physical device testing for ARCore spatial awareness, lighting estimation, cloud anchors, and voice recognition.

## 2. Test Environments

- **Local JVM**: JDK 17.
- **Android Emulator**: Pixel 7 Pro (API 34).
- **Physical Devices**: Samsung Galaxy S23 Ultra (API 34) (Depth/ToF equipped), Google Pixel 6 (API 33).

## 3. Coverage Targets

- **Core Domain Layer (`RoomStateManager`, `RoomAnalyticsManager`)**: 95% Code Coverage.
- **Data Layer (Repositories, `SyncManager`)**: 85% Code Coverage.
- **UI Layer (Compose ViewModels)**: 70% Code Coverage.

## 4. Specific Testing Scenarios

### 4.1 State Synchronization Testing

The most critical test suite ensures that `RoomStateManager` correctly emits state to both AR and 2D Planner viewmodels without race conditions.
- **Scenario**: Verify Multi-modal selection.
- **Action**: Simulate a "Tap" event in AR.
- **Expected**: `RoomStateManager` updates selection. The 2D planner instantly highlights the corresponding 2D icon.

### 4.2 Persistence & Auto-save

- **Scenario**: Validate Debounce Save.
- **Action**: Emit 5 `MoveItem` events within 1.5 seconds.
- **Expected**: `LayoutPersistenceManager` triggers only exactly 1 DB write call after 2 seconds of inactivity.

### 4.3 AR Testing

Due to the physical nature of AR, automated UI tests mock the `ArSceneView`. Actual spatial testing requires manual execution:
- **Scenario**: Verify Cloud Anchors.
- **Action**: Host an anchor, close app, clear cache, reopen, resolve anchor.
- **Expected**: Furniture appears in the exact same physical coordinates.

### 4.4 2D Canvas Testing

- **Scenario**: Verify Wall Snapping.
- **Action**: Draw a wall ending within 10dp of an existing `CornerPointEntity`.
- **Expected**: The new wall automatically locks its end coordinate to the existing corner point.

## 5. Bug Tracking and Failure Scenarios

Bugs are tracked using GitHub Issues. Critical failure scenarios include:

1. **ARCore Unsupported**: Handled gracefully by hiding the "AR Mode" button and defaulting the user entirely to the 2D planning mode.
2. **Network Offline**: Handled via Room DB caching and WorkManager; all 2D/AR interactions function normally, cloud sync is queued until network restores.
