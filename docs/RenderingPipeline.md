# Rendering Pipeline

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

Lumiroom has two distinct rendering pipelines that operate concurrently: The Native 3D Pipeline and the Compose 2D Pipeline.

## 1. Native 3D Pipeline (SceneView / Filament)

`SceneView` uses the Google `Filament` physically based rendering (PBR) engine.

1. **Model Loading**: When `RoomState` updates, the `ArViewModel` checks for new items. It requests the GLB asset from the repository.
   - *Important Note*: The Filament `ModelLoader` natively supports and resolves `file:///android_asset/` URIs. Developers must ensure that absolute asset paths passed to `SceneView` are not incorrectly prepended with redundant `models/` directory prefixes, as this causes `FileNotFoundException`s which are silently swallowed by coroutines, leading to invisible models.
2. **Node Creation**: A `ModelNode` is instantiated.
3. **PBR Material Setup**: SceneView automatically extracts the diffuse, metallic, and roughness maps from the GLB file.
4. **Lighting**: An HDR environment map (from the camera feed) is applied to match ambient physical lighting.
5. **Frame Rendering**: The C++ Filament engine renders the 3D frame overlaying the camera feed at 60 FPS.

## 2. Compose 2D Pipeline

The 2D canvas is a pure Kotlin software-based rendering pipeline.

1. **Recomposition**: When `RoomState` changes, the Compose compiler triggers recomposition of the `RoomPlannerScreen`.
2. **Matrix Transform**: The `CameraState` (pan/zoom) creates an affine transformation matrix.
3. **Draw Loop**:
   - `drawRect` (Background grid)
   - `drawLine` (Walls)
   - `drawImage` (Furniture thumbnails, scaled and rotated to match `PlacedItem` rotation quaternions converted to Euler degrees).
4. **Hardware Acceleration**: Compose leverages Android's hardware-accelerated Skia graphics library to render the Canvas efficiently.
