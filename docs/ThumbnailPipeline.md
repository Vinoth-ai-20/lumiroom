# Thumbnail Pipeline

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


Lumiroom mandates a 1:1 parity between 3D Models and their respective 2D imagery.

## Resolution and Format
- Format: `WEBP`
- Recommended Dimensions: `512x512` or `1024x1024` for high density screens.
- Naming: The `thumbnail` must exactly mirror its `model` counterpart. E.g. `bathroom_bathtub_01.glb` must be paired with `bathroom_bathtub_01.webp`.

## Display Strategy
When the database seeds, the `FurnitureEntity.thumbnailPath` is hard-linked to the predicted Android Asset Uri: `file:///android_asset/thumbnails/{filename}.webp`.

The UI layer (such as `FurnitureCard`) utilizes `Coil`'s `AsyncImage` to efficiently lazy-load these assets directly from disk over standard HTTP fetches.
