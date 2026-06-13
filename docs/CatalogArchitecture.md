# Catalog Architecture

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


The Lumiroom Catalog is designed to be completely data-driven and independent of hardcoded constants.

## Core Flow

1. **Local Database**: All catalog metadata is stored inside the Room SQLite `LumiroomDatabase` within the `furniture` table.
2. **ViewModel Injection**: `CatalogViewModel` hooks into `FurnitureRepository` to fetch streams of `Furniture` objects.
3. **Filtering & Sorting**: Users can filter furniture by their dynamically assigned Categories (e.g., Bathroom, Livingroom) which are derived automatically during the `ModelDiscovery` phase.
4. **Rendering**: `CatalogScreen` renders `FurnitureCard` components, displaying dynamic Indian Market pricing, derived titles, and the localized `.webp` thumbnail path.

## Synchronization

Because the catalog is generated dynamically upon the first boot of the application (or during destructive DB upgrades), any synchronization between the Home Page, Search indexing, and the AR/2D views is handled natively via Flow observers tracking the `furniture` table.
