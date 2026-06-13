# Model Discovery Engine

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


Lumiroom eliminates the need for hardcoded JSON configuration lists by utilizing a Model Discovery Engine within the `DatabaseSeeder.kt`.

## Process Overview

1. **Asset Interrogation**: The App Context queries the `assets/models/` directory for an array of available files.
2. **Filtering**: Only `.glb` files are parsed, ignoring any malformed or unrelated file types.
3. **Lexical Parsing**: The engine splits the filename delimiter `_` to infer exact taxonomy.
   - Index 0: Base Category
   - Index 1: Furniture classification
   - Index 2: Variant ID

By relying entirely on the native OS file listing mechanisms, adding 100 new furniture models to the application requires zero code changes.
