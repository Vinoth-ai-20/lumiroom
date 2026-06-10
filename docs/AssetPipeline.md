# Lumiroom Furniture Asset Pipeline

This document defines the standard operating procedure (SOP) for preparing, optimizing, and registering 3D furniture models into the Lumiroom platform.

By following this pipeline, new furniture assets can be added dynamically via Firebase Cloud Storage and Firestore without requiring an application update.

---

## 1. Folder Structure

Maintain this structure locally before uploading to Firebase:

```text
furniture_assets/
├── src_models/               # Raw FBX models from "Furniture Mega Pack Free"
├── working_files/            # Blender (.blend) files
├── export_glb/               # Final, optimized .glb files
├── export_thumbnails/        # Rendered .webp or .png thumbnails
└── metadata.json             # Registration schema for Firestore
```

---

## 2. Naming Conventions

Strict naming conventions ensure automated scripts can map thumbnails to GLB files.

- **Format:** `category_style_name_variant` (All lowercase, snake_case)
- **GLB File:** `sofa_modern_carlton_grey.glb`
- **Thumbnail:** `sofa_modern_carlton_grey.webp`
- **ID Generation:** Strip the extension to get the unique Database ID (`sofa_modern_carlton_grey`).

---

## 3. Blender Optimization Workflow

Lumiroom uses Google's `SceneView` (ARCore), which requires highly optimized `.glb` (glTF Binary) files to maintain 60 FPS on mobile devices.

### 3.1 Import Workflow
1. Open a new Blender project.
2. File > Import > FBX (`.fbx`).
3. Select the target furniture piece.

### 3.2 Optimization Rules
1. **Scale & Transforms:** 
   - Apply all transforms (`Ctrl + A` -> All Transforms). 
   - Ensure the model's pivot point (origin) is at the **bottom-center** (where it touches the floor). This is critical for AR anchoring.
   - Scale must be 1:1 in **meters**. A 2-meter long sofa must measure exactly 2.0 on the X-axis in Blender.
2. **Mesh Optimization:**
   - Merge vertices by distance (`M` in Edit Mode).
   - Target Polygon Count: **< 15,000 tris** for large furniture (Sofas, Beds), **< 5,000 tris** for small decor. Use the Decimate modifier if necessary.
3. **Materials & Textures:**
   - Use a single Principled BSDF material per object.
   - Bake textures if there are multiple overlapping materials.
   - Texture maps (Base Color, Normal, ORM) must be compressed to **1024x1024** or **512x512** for smaller items.

### 3.3 GLB Export
1. File > Export > glTF 2.0 (`.glb`).
2. **Settings:**
   - Format: `glTF Binary (.glb)`
   - Include: `Selected Objects`
   - Transform: `+Y Up` (Standard for ARCore)
   - Geometry: Apply Modifiers
   - Compression: Enable **Draco Compression** (Compression Level: 6) to heavily reduce file size.

---

## 4. Thumbnail Generation Workflow

Thumbnails are displayed in the Lumiroom Catalog UI.

1. **Camera Setup:** 
   - Add a Camera in Blender (`Shift + A` > Camera).
   - Position it at an isometric angle (e.g., Rotation X: 60°, Y: 0°, Z: 45°).
   - Set resolution to **512x512**.
   - Set Background to Transparent (Render Properties > Film > Transparent).
2. **Lighting:**
   - Use a basic Studio HDRI for consistent, soft lighting without harsh shadows.
3. **Render:**
   - Render Image (`F12`).
   - Save as `.webp` (or `.png`) with RGBA channels. WebP is preferred for Android bandwidth savings.

---

## 5. Catalog Registration (Metadata Schema)

To inject the asset into the app without code changes, upload the `.glb` and `.webp` to Firebase Storage, then add the following JSON structure to your Firestore `furniture_catalog` collection:

```json
{
  "id": "sofa_modern_carlton_grey",
  "name": "Carlton Modern Sofa",
  "category": "SOFA",
  "styleTag": "MODERN",
  "description": "A sleek, grey modern sofa perfect for minimalist living rooms.",
  "length": 2.1,
  "width": 0.9,
  "height": 0.85,
  "priceEstimate": 899.99,
  "modelUrl": "gs://lumiroom-project.appspot.com/models/sofa_modern_carlton_grey.glb",
  "thumbnailUrl": "gs://lumiroom-project.appspot.com/thumbnails/sofa_modern_carlton_grey.webp",
  "createdAt": 1718000000000,
  "updatedAt": 1718000000000
}
```

### 5.1 Remote Sync Behavior
When the app launches, the `SyncEngine` queries the Firestore `furniture_catalog` collection. It automatically downloads new entries, caches the `modelUrl` for offline AR viewing, and populates the UI dynamically. No application update is needed.
