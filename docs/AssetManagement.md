# Asset Management Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


Lumiroom implements a dynamic, headless asset management pipeline. Instead of relying on hardcoded catalog lists, the application performs a runtime asset discovery sequence during database initialization.

## Directory Structure

All 3D models and their corresponding 2D thumbnails are bundled natively within the application's `/assets` directory.

- `app/src/main/assets/models/*.glb`
- `app/src/main/assets/thumbnails/*.webp`

## Naming Convention

Assets MUST follow the strict naming convention to be discovered and categorized correctly:
`{room_category}_{furniture_type}_{variant}.glb`

**Examples:**
- `bathroom_bathtub_01.glb`
- `livingroom_sofa_02.glb`

## Pipeline Flow

1. **Discovery**: `DatabaseSeeder.kt` queries `Context.assets.list("models")`.
2. **Parsing**: The filename is parsed to extract the Category, Type, and Variant string.
3. **Metadata Enrichment**: A synthetic title, description, and dimension profile is assigned.
4. **Pricing Engine**: The `PricingStrategy` determines the market value in Indian Rupees (₹).
5. **Database Injection**: A new `FurnitureEntity` is constructed and persisted to the local Room Database.

## Adding New Assets

To add new furniture to Lumiroom, simply place the `.glb` file inside the `models/` folder and the identically named `.webp` file inside the `thumbnails/` folder. Rebuild the application or trigger a database reset to instantly populate the catalog.
