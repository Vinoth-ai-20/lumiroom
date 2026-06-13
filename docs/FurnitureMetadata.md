# Furniture Metadata Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


This document describes the structure of `FurnitureEntity` within Lumiroom. 

## Entity Structure

When the dynamic Model Discovery script builds the catalog, it constructs rows in the Room database mapped as follows:

- **`id`**: Unique string generated directly from the filename (e.g. `bathroom_bathtub_01`).
- **`name`**: Title-cased presentation string (e.g. `Bathtub 01`).
- **`category`**: Top-level UI filter group (e.g. `Bathroom`).
- **`roomType`**: Synonymous with Category for the scope of analytics/filtering.
- **`description`**: Programmatically generated description based on type and category.
- **`width` / `depth` / `height`**: Approximated dimension envelope to prevent clipping during initial placement in AR/2D boundaries.
- **`priceEstimate`**: Deterministically resolved Double (represented in Indian Rupees, ₹).
- **`modelPath`**: Local Android Asset URI mapped to `/models`.
- **`thumbnailPath`**: Local Android Asset URI mapped to `/thumbnails`.
- **`style`**: Aesthetic style tagging (defaults to "Modern").

## Extending Meta
If a feature requires complex, manual meta-overrides, the `DatabaseSeeder` should be augmented to parse an optional companion `.json` file containing explicit overrides for the dynamic attributes of a given variant ID.
