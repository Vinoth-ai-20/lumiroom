# Activity Diagrams

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Deployment Diagrams](DeploymentDiagrams.md)

---

## 1. Voice Command Resolution Activity
Details the internal logic of the fuzzy-matching NLP parser.

```mermaid
activityDiagram
    start
    :Receive Audio Transcript;
    :Lowercase & Sanitize text;
    
    if (Contains "place" or "add") then (yes)
        :Extract Noun Phrase (e.g. "modern sofa");
        :Query Local Catalog;
        if (Item Found?) then (yes)
            :Emit PLACE Intent;
        else (no)
            :Emit ERROR: "Item not found";
        endif
    else if (Contains "rotate") then (yes)
        :Extract Degrees (e.g. "90");
        :Emit ROTATE Intent;
    else if (Contains "delete" or "remove") then (yes)
        :Emit DELETE Intent;
    else (Unknown)
        :Emit ERROR: "Unrecognized command";
    endif
    stop
```

## 2. FMP Batch Processing Activity
Details the workflow of the Python automation script used by developers.

```mermaid
activityDiagram
    start
    :Scan /raw_fmp_files directory;
    while (More FBX files?) is (yes)
        :Load into Blender (bpy);
        :Reset Origin to Bottom-Center;
        :Apply Transformations;
        if (Polygons > 50,000?) then (yes)
            :Apply Decimate Modifier;
        else (no)
        endif
        :Export as GLB (Draco Compressed);
        :Render 512x512 WebP Thumbnail;
    endwhile (no)
    :Upload batch to Firebase Storage;
    stop
```
