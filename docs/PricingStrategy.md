# Pricing Strategy

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


Lumiroom implements a localized Indian Market Pricing Strategy utilizing Indian Rupees (₹).

## Pricing Algorithm
Because the catalog is generated dynamically, storing fixed prices inside a configuration file is obsolete. The `DatabaseSeeder` incorporates an intelligent interpolation algorithm to predict and assign a realistic price based on the parsed variant tree.

## Market Bounds
The logic enforces the following thresholds per item variant:

- **Bathroom**
  - Bathtub: ₹15,000 – ₹80,000
  - Toilet: ₹5,000 – ₹25,000
  - Vanity: ₹8,000 – ₹40,000
- **Living Room**
  - Sofa: ₹15,000 – ₹1,20,000
  - Coffee Table: ₹4,000 – ₹30,000
  - TV Unit: ₹8,000 – ₹60,000
- **Bedroom**
  - Bed: ₹12,000 – ₹1,50,000
  - Wardrobe: ₹10,000 – ₹1,00,000
  - Nightstand: ₹2,000 – ₹15,000
- **Dining Room**
  - Dining Table: ₹10,000 – ₹80,000
  - Dining Chair: ₹2,000 – ₹15,000
- **Office**
  - Desk: ₹5,000 – ₹40,000
  - Office Chair: ₹4,000 – ₹30,000

## Deterministic Generation
To prevent furniture prices from changing completely upon every database reset, the seeder binds the Random number generator to the `.hashCode()` of the specific variant ID. This guarantees natural variance while remaining purely deterministic. Prices are also rounded to the nearest ₹100 for visual clarity.
