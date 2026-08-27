# Junk Pirates Changelog

## [3.5.6] - 2026-08-20

### Weapon Slot & Launch Bay Standardization
- **Universal Slot Standardization:** Standardized all weapon slot and launch bay IDs across every `.ship`, `.variant`, and `.skin` file to clean zero-padded `WS 001` format.
- **Duplicate Slot Resolution:** Resolved legacy duplicate slot ID collisions without data loss, ensuring 100% legal JSON and preventing engine crashes.
- **Launch Bay Port Formations:** Converted single-port launch bays to multi-port triangular formations, completely eliminating the vanilla center-of-sprite launch fallback.
- **Skin & Station Variant Sync:** Updated all station modules, drones, and skin-derived variants across subdirectories to eliminate mission loading `NullPointerExceptions`.

### Scrapjet Missile Weapon Overhaul
- **Custom Autofire Scripting (`ScrapjetAutofirePlugin`):** Built custom Java plugin overriding vanilla AI hesitation to force launches when enemies enter tracking range.
- **Triple-Reference Launch Authority:** Implemented multi-reference angular checks (`arcFacing`, `currAngle`, `shipFacing`) supporting up to ±140° launch authority for hardpoints and diagonal mounts.
- **Flight & Tracking Tuning:** Increased projectile speed from 500 to **600**, tuned lifetime to **5.0s**, and boosted guidance turn rate to **120°/s** and turn acceleration to **300**.
- **Combat Rebalancing:**
  - Standard Scrapjet Damage: Rebalanced from 600 to **400** (Micro: 300 to **200**).
  - Scrapjet Missile Pod (Medium): Scaled to **10 OP**, **24 Ammo** (6 volleys of 4), **1,200 Range**.
  - Scrapjet Missile Rack (Small): Reconfigured to **8 OP**, **8 Ammo** (2 volleys of 4), **1,200 Range**.
  - AI Hints: Removed `STRIKE`, `USE_VS_FRIGATES`, and `GUIDED_POOR`; tagged with `DO_NOT_CONSERVE`.

### Classpath & Codebase Integrity
- Fixed classpath in `junk_pirates_welder.wpn` (`data.scripts.weapons.StygianDrillEffect`).
- Fixed classpath in `data/campaign/industries.csv` (`data.campaign.econ.impl.FamiliaHQ`).
- Implemented missing `PackDummyIntegratedPointDefenseAI.java` hullmod class.
