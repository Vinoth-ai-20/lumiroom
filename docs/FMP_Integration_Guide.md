# Furniture Mega Pack (FMP) Integration Guide

This guide provides a step-by-step workflow for taking assets from the **Furniture Mega Pack Free (FMP)** and properly formatting them for the Lumiroom AR Application.

Because FMP is structured primarily for Unity Engine (containing `.meta` files, `Prefabs`, and `Scenes`), we must manually extract the raw `.fbx` meshes and textures, reassemble them in Blender, and export them to ARCore-compatible `.glb` formats.

---

## 1. Understanding the FMP Structure

You only need to interact with two folders from the FMP pack:
1. `FBX/`: Contains the raw 3D mesh data (e.g., `FBX/Sofa/Sofa01.fbx`).
2. `Textures/`: Contains the image maps for coloring and detailing the meshes.

> Note: You can safely ignore `Materials/`, `Prefabs/`, `Scenes/`, and all `.meta` files. These are Unity-specific and cannot be used directly in Android/ARCore.

---

## 2. Step-by-Step Blender Conversion

To make an FMP asset usable in Lumiroom, it must be converted to a **glTF Binary (.glb)** file.

### Step 2.1: Import the FBX
1. Open a new project in **Blender**.
2. Delete the default cube.
3. Go to **File > Import > FBX (.fbx)**.
4. Navigate to your FMP folder: `E:\FMP\FBX\<Category>\<ModelName>.fbx` (e.g., `Sofa01.fbx`) and import it.

### Step 2.2: Fix Scaling and Rotation
Unity exports often come into Blender at the wrong scale or rotation. ARCore requires 1:1 real-world scaling in meters.
1. Select the imported furniture.
2. Press `N` to open the Transform panel.
3. Ensure the **Scale** makes sense in meters (e.g., a Sofa should be roughly 2.0m wide). If it is 100x too big, scale it down by `0.01`.
4. Ensure the pivot point (Origin) is exactly at the bottom center where the furniture touches the floor.
5. Press `Ctrl + A` and select **All Transforms** to bake the scale and rotation.

### Step 2.3: Reconnect Textures
Unity FBX files often lose their texture links when imported into Blender.
1. Open the **Shader Editor** in Blender.
2. Select the furniture's material.
3. Locate the corresponding textures in the `FMP/Textures/<Category>/` folder.
4. Drag and drop the textures into the Shader Editor and connect them to the `Principled BSDF` node:
   - **Albedo/Diffuse map** -> `Base Color`
   - **Normal map** -> `Normal Map Node` -> `Normal`
   - **Metallic map** -> `Metallic`
   - **Smoothness map** -> *Invert Node* -> `Roughness` (Unity uses Smoothness, glTF uses Roughness).

> Tip: If the textures are 2K or 4K, scale them down to 1024x1024 to save memory and ensure 60FPS on mobile AR.

### Step 2.4: Export to GLB
1. Go to **File > Export > glTF 2.0 (.glb)**.
2. In the export settings on the right:
   - **Format:** `glTF Binary (.glb)`
   - **Include:** Check `Selected Objects`
   - **Transform:** `+Y Up`
   - **Geometry:** Check `Apply Modifiers`
   - **Compression:** Check `Draco Compress` (Set compression level to 6).
3. Save the file using the Lumiroom naming convention: `category_style_name.glb` (e.g., `sofa_modern_01.glb`).

---

## 3. Generate Thumbnail

Lumiroom's catalog requires a `512x512` preview image.
1. In Blender, add a Camera and position it at a 45-degree isometric angle to the furniture.
2. Set the background to Transparent (`Render Properties > Film > Check Transparent`).
3. Render the image (`F12`) and save it as `sofa_modern_01.webp`.

---

## 4. Lumiroom Registration

Once you have your `sofa_modern_01.glb` and `sofa_modern_01.webp`, upload them to Lumiroom's Firebase environment.

### Step 4.1: Upload Assets
1. Upload the `.glb` file to your Firebase Storage bucket under `/models/`.
2. Upload the `.webp` file to your Firebase Storage bucket under `/thumbnails/`.

### Step 4.2: Add to Firestore Database
Create a new document in your `furniture_catalog` Firestore collection with the following metadata:

```json
{
  "id": "sofa_modern_01",
  "name": "FMP Modern Sofa 01",
  "category": "SOFA",
  "styleTag": "MODERN",
  "description": "A stylish modern sofa from the Furniture Mega Pack.",
  "length": 2.2,
  "width": 0.9,
  "height": 0.85,
  "priceEstimate": 599.00,
  "modelUrl": "gs://<your-firebase-bucket>/models/sofa_modern_01.glb",
  "thumbnailUrl": "gs://<your-firebase-bucket>/thumbnails/sofa_modern_01.webp",
  "createdAt": <current_timestamp>,
  "updatedAt": <current_timestamp>
}
```

**Done!** The next time you open the Lumiroom application, the app will dynamically fetch the new FMP sofa, cache its GLB file, and make it immediately available for AR placement. No app updates required!
