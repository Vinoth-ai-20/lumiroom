<p align="center">
  <img src="../Lumiroom-logo-alpha.png" width="120"/>
</p>

# QA and Testing Strategy Report

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Use Cases](UseCases.md)

---

## 1. Testing Strategy
Lumiroom follows a Test-Driven Development (TDD) approach where possible, augmented by rigorous manual QA for the Augmented Reality modules.

### 1.1 Testing Pyramid
- **Unit Tests (70%)**: Run via JUnit 4 on the local JVM. Targets ViewModels, Use Cases, Parsers, and Repositories.
- **Integration/Instrumentation Tests (20%)**: Run via AndroidX Test on emulators/devices. Targets Room Database and Compose UI Navigation.
- **Manual/AR Testing (10%)**: Physical device testing for ARCore spatial awareness, lighting estimation, and voice recognition.

## 2. Test Environments
- **Local JVM**: JDK 17.
- **Android Emulator**: Pixel 7 Pro (API 34).
- **Physical Devices**: Samsung Galaxy S23 Ultra (API 34), Google Pixel 6 (API 33).

## 3. Coverage Targets
- **Domain Layer**: 90% Code Coverage.
- **Data Layer (Repositories)**: 85% Code Coverage.
- **UI Layer (Compose)**: 60% Code Coverage.

## 4. Specific Testing Scenarios

### 4.1 AR Testing
Due to the physical nature of AR, automated UI tests mock the `ArSceneView`. Actual spatial testing requires manual execution:
- **Scenario**: Verify Plane Detection.
- **Action**: Pan the device across a textured floor.
- **Expected**: A visual grid appears mapping the floor geometry within 3 seconds.

### 4.2 Voice Command Testing
The `CommandParser` is tested extensively via parameterized unit tests:
- Input: *"Place the modern vanity here"* -> Output: `Action: Place, Target: Modern Vanity, Location: Raycast`.
- Input: *"Remove that sofa"* -> Output: `Action: Remove, Target: Selected Item`.

## 5. Bug Tracking and Failure Scenarios
Bugs are tracked using GitHub Issues. Critical failure scenarios include:
1. **ARCore Unsupported**: Handled gracefully by redirecting the user to the 2D planning mode.
2. **Network Offline**: Handled via Room DB caching; all AR interactions function normally, cloud sync is queued.

