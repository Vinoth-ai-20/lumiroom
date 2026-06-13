<p align="center">
  <img src="../Lumiroom-logo-alpha.png" width="120"/>
</p>

# Asset Pipeline Documentation

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).
>
> **AR Synchronization & AppCheck Update (v1.1.2):**
> Fixed Firebase AppCheck quirk where Android caches an overriding random debug token instead of parsing `strings.xml`. Resolved Filament `ModelLoader` swallowing `FileNotFoundException` by properly parsing native `file:///android_asset/` URIs without redundant prefixing.


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.1.2  
**Date:** 2026-06-13  

[⬅ Back to README](../README.md) | [Next: FMP Integration Guide](FMP_Integration_Guide.md)

---

## 1. Introduction

High-quality 3D assets are central to the Lumiroom AR experience. The Asset Pipeline defines the automated and manual processes for importing, optimizing, compressing, and caching 3D models from the Furniture Mega Pack (FMP) into the Android application.

## 2. Asset Pipeline Workflow

```mermaid
flowchart TD
    A([Start]) --> B[Designer creates FBX/OBJ in Blender]
    B --> C[Export to glTF]
    C --> D[Texture Compression: KTX2 / Basis Universal]
    D --> E[Mesh Decimation: Draco Compression]
    E --> F[Export as binary GLB]
    F --> G[Generate 512x512 WebP Thumbnail]
    G --> H[Upload to Firebase Storage]
    H --> I[Update Firestore Metadata: Name, Bounds, Category]
    I --> J([Stop])
```

## 3. Optimization Strategy

### 3.1 Mesh Optimization

- **Polygon Count**: Strict limit of 50,000 polygons per model to ensure 30+ FPS rendering on mobile devices.
- **Draco Compression**: Applied to all `.glb` models to drastically reduce file size before network transmission.

### 3.2 Texture Pipeline

- All textures are baked into BaseColor, Normal, and ORM (Occlusion/Roughness/Metallic) maps.
- **Resolution Limit**: 1024x1024 maximum texture resolution.
- **Format**: Textures are compressed using KTX2 formatting to minimize GPU memory footprint during AR rendering.

## 4. Caching and Loading Strategy

### 4.1 Local File Caching

```mermaid
stateDiagram-v2
    [*] --> CheckLocalCache
    CheckLocalCache --> LoadFromDisk : File exists
    CheckLocalCache --> DownSloadFromFirebase : File missing
    DownloadFromFirebase --> SaveToDisk
    SaveToDisk --> LoadFromDisk
    LoadFromDisk --> RenderInAR
```

### 4.2 Memory Management

- Models are loaded asynchronously via Kotlin Coroutines.
- If memory pressure is detected (`onTrimMemory`), unused cached models in RAM are explicitly released by SceneView.

## 5. Naming Conventions

All assets must follow the `Category_Brand_ModelName.glb` structure (e.g., `Sofa_Ikea_Kivik.glb`).


## Asset-Driven Catalog Architecture (v12)
The furniture catalog is now completely dynamically generated from local assets.
No manual registration, hardcoded arrays, or JSON seeding is required.
During the application startup (specifically database creation), the system automatically scans the assets/models/ and assets/thumbnails/ directories.
It uses the naming convention roomType_category_variant.glb (e.g. bathroom_bathtub_01.glb) to dynamically generate metadata, categories, pricing, and tags. This forms the single source of truth for the entire application catalog, powering search, filters, and AR persistence.
