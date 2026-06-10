# Activity Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Deployment Diagrams](DeploymentDiagrams.md)

---

## 1. Voice Command Resolution Activity
Details the internal logic of the fuzzy-matching NLP parser.

```mermaid
flowchart TD
    A([Start]) --> B[Receive Audio Transcript]
    B --> C[Lowercase & Sanitize text]
    
    C --> D{Contains 'place' or 'add'?}
    D -- yes --> E[Extract Noun Phrase]
    E --> F[Query Local Catalog]
    F --> G{Item Found?}
    G -- yes --> H[Emit PLACE Intent]
    G -- no --> I[Emit ERROR: Item not found]
    
    D -- no --> J{Contains 'rotate'?}
    J -- yes --> K[Extract Degrees]
    K --> L[Emit ROTATE Intent]
    
    J -- no --> M{Contains 'delete' or 'remove'?}
    M -- yes --> N[Emit DELETE Intent]
    
    M -- no --> O[Emit ERROR: Unrecognized command]
    
    H --> Z([Stop])
    I --> Z
    L --> Z
    N --> Z
    O --> Z
```

## 2. FMP Batch Processing Activity
Details the workflow of the Python automation script used by developers.

```mermaid
flowchart TD
    A([Start]) --> B[Scan /raw_fmp_files directory]
    B --> C{More FBX files?}
    C -- yes --> D[Load into Blender]
    D --> E[Reset Origin to Bottom-Center]
    E --> F[Apply Transformations]
    F --> G{Polygons > 50,000?}
    G -- yes --> H[Apply Decimate Modifier]
    G -- no --> I[Export as GLB]
    H --> I
    I --> J[Render 512x512 WebP Thumbnail]
    J --> C
    
    C -- no --> K[Upload batch to Firebase Storage]
    K --> Z([Stop])
```
