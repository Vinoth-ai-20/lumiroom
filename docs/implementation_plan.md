# AR Voice Command Fixes and Documentation Updates

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


This document outlines the investigation and planned fixes for the AR Scene Voice Commands and documentation sync.

## User Review Required

Please review the proposed approach. Once approved, I will implement the fixes.

## Proposed Changes

### AR Voice Command Fixes

#### [MODIFY] [ArViewModel.kt](file:///e:/projects/Lumiroom/feature/ar/src/main/kotlin/com/lumiroom/feature/ar/presentation/ArViewModel.kt)
- **Select/Deselect**: Implement `VoiceCommand.Select` and `VoiceCommand.SelectLast` correctly.
- **Remove Bug**: Fix `VoiceCommand.Remove` which incorrectly looks for `it.name` on `RoomFurniture` instead of `it.furnitureId`. This will use `furnitureId.contains(..., ignoreCase = true)` to find matching items.
- **Placement Logic**: Modify `VoiceCommand.Place` so it does not arbitrarily place objects at world origin `(0, 0, -1)`. Instead, it will emit a new event `ArViewEvent.PlaceFromVoice(furnitureId)` to be handled by the UI.

#### [MODIFY] [ArScreen.kt](file:///e:/projects/Lumiroom/feature/ar/src/main/kotlin/com/lumiroom/feature/ar/presentation/ArScreen.kt)
- Add `PlaceFromVoice` to the `ArViewEvent` sealed class.
- When `PlaceFromVoice` is received, use `arSceneView.hitTest(centerOffset)` to find the real-world surface directly in front of the camera and pass the correct world coordinates back to the view model's `onPlaneTapped()`.

### Build Configuration Fix

#### [MODIFY] [build.gradle.kts](file:///e:/projects/Lumiroom/feature/room-planner/build.gradle.kts)
- The recent `VoiceCommandManager` injection caused a compilation error in `room-planner` due to the missing `:feature:voice` dependency. This has already been fixed to restore compilation.

### Documentation Updates

#### [MODIFY] [docs/](file:///e:/projects/Lumiroom/docs/)
- Review `SRS.md`, `C4Architecture.md`, `RoomStateArchitecture.md`, and any other documentation files.
- Ensure all recent changes (such as meters-to-cm conversions, minimap removal, unified RoomState singleton architecture, and voice command expansions) are fully documented and accurately reflect the current repo structure.

## Verification Plan

### Automated Tests
- Ensure `app:assembleDebug` builds successfully without KSP errors.

### Manual Verification
- In AR mode, say "Place a chair" and verify it appears on the plane in front of the camera, not at world origin.
- Say "Select the chair" and verify the bounding box appears.
- Say "Make it bigger" and verify it scales.
- Say "Rotate left" and verify it rotates.
- Check documentation files for consistency.
