# Room Scanning Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom relies on Google ARCore and SceneView for spatial understanding. 

## The `LumiroomArSessionManager`

This manager encapsulates all interactions with the SceneView engine.

### Capabilities

1. **Plane Detection**: Automatically identifies horizontal and vertical surfaces. It filters out planes that are too small to be meaningful for furniture placement.
2. **Raycasting (Hit Testing)**: When a user taps the screen, a ray is projected from the camera lens through the screen coordinates into the 3D scene.
   - It returns a `HitResult` containing the exact 3D coordinates, the plane it hit, and the normal vector.
3. **Anchoring**: ARCore uses anchors to prevent models from drifting as the user walks around. When an item is placed, an `Anchor` is created at that `HitResult` and attached to the 3D `Node`.

## Cloud Anchors

To support multi-device persistence, Lumiroom integrates ARCore Cloud Anchors.
- When a room is saved, local anchors can be "hosted" to the Google Cloud, returning a unique Cloud Anchor ID.
- This ID is saved in the `AnchorEntity` in the local DB.
- When the room is loaded later, the ID is used to "resolve" the anchor, placing the furniture back in its exact physical location relative to the real world.
