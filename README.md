# Junk Pirates

Junk Pirates is not the work of a single individual. The following people are the ones I can think of to directly credit, notwithstanding the years of valued non-specific contributions from the wider modding community.

## Credits

- **MesoTronik:** Custom Sounds, Quality Control, Flare code
- **HELMUT; Dark.Revenant; Avanitia:** Major balance feedback
- **Xenoargh:** Significant contributor to Pitbull Sprite
- **Versus The Ghost:** Musical Accompaniments
- **mendonca:** Most of the rest of the stuff, I think
- **NetworkPesci:** Just for being there
- **Histidine:** For providing an off-the-cuff comment to unlock 4 months of idle nurdling around an obscure issue with the procgen
- **King Alfonzo:** 0.5-2% of the new Ridgeback sprite
- **Vayra:** For the zany electrochaff and scrapjet missiles. For Kadur.
- **MShadowy:** Arcane mutterings that lead to the vector code appearing to work

## Notes

- Added Industrial Evolution Support
- Added New Beginnings Support
- ASP Syndicate fleet activity in systems.

## Changelog

### 3.5.6 Changelog

**Weapon Slot & Launch Bay Standardization:**
- **Universal Slot Standardization:** Standardized all weapon slot and launch bay IDs across every `.ship`, `.variant`, and `.skin` file to clean zero-padded `WS 001` format.
- **Duplicate Slot Resolution:** Resolved legacy duplicate slot ID collisions without data loss, ensuring 100% legal JSON and preventing engine crashes.
- **Launch Bay Port Formations:** Converted single-port launch bays to multi-port triangular formations, completely eliminating the vanilla center-of-sprite launch fallback.
- **Skin & Station Variant Sync:** Updated all station modules, drones, and skin-derived variants across subdirectories to eliminate mission loading `NullPointerExceptions`.

**Scrapjet Missile Weapon Overhaul:**
- **Custom Autofire Scripting (`ScrapjetAutofirePlugin`):** Built custom Java plugin overriding vanilla AI hesitation to force launches when enemies enter tracking range.
- **Triple-Reference Launch Authority:** Implemented multi-reference angular checks (`arcFacing`, `currAngle`, `shipFacing`) supporting up to ±140° launch authority for hardpoints and diagonal mounts.
- **Flight & Tracking Tuning:** Increased projectile speed from 500 to **600**, tuned lifetime to **5.0s**, and boosted guidance turn rate to **120°/s** and turn acceleration to **300**.
- **Combat Rebalancing:**
  - Standard Scrapjet Damage: Rebalanced from 600 to **400** (Micro: 300 to **200**).
  - Scrapjet Missile Pod (Medium): Scaled to **10 OP**, **24 Ammo** (6 volleys of 4), **1,200 Range**.
  - Scrapjet Missile Rack (Small): Reconfigured to **8 OP**, **8 Ammo** (2 volleys of 4), **1,200 Range**.
  - AI Hints: Removed `STRIKE`, `USE_VS_FRIGATES`, and `GUIDED_POOR`; tagged with `DO_NOT_CONSERVE`.

**Classpath & Codebase Integrity:**
- Fixed classpath in `junk_pirates_welder.wpn` (`data.scripts.weapons.StygianDrillEffect`).
- Fixed classpath in `data/campaign/industries.csv` (`data.campaign.econ.impl.FamiliaHQ`).
- Implemented missing `PackDummyIntegratedPointDefenseAI.java` hullmod class.

### 3.5.5 Changelog

**Variant & Ship Budget Overhaul:**
- **OP Budget Standardization:** Audited all 193 ship variants; trimmed 45 over-budget variants down to exact hull capacity without breaking loadout identities.
- **Under-OP Variants Upgraded:** Topped off 22 under-budget variants with flux vents and capacitors to ensure AI fleets deploy with full combat capability.
- **Installed Standard Hullmods:** Equipped under-budget ships with optimal standard hullmods:
  - `syndicate_asp_kingcobra_Standard`: Added *Hardened Shields* (`hardenedshieldemitter`).
  - `junk_pirates_boxenstein_blue_Support`: Added *Heavy Armor* (`heavyarmor`), elevating armor to 1,450.
  - `junk_pirates_kraken_CS`: Added *Flux Distributor* (`fluxdistributor`) for +100 dissipation.
  - `syndicate_asp_gigantophis_Standard`: Added *Stabilized Shields* (`stabilizedshieldemitter`).
  - `syndicate_asp_gigantophis_p_Standard`: Added *Recovery Shuttles* (`recovery_shuttles`).
  - `pack_bullykutta_dogbox_Standard`: Added *ECCM Package* (`eccm`).

**Graphics & Runtime Fixes:**
- **Phase Engine Glow Restored:** Restored phase cloak glow textures and shaders across all mod hulls, preventing runtime missing texture crashes.
- **Portrait Restored:** Restored `junk_pirates_portrait_f_3.png` (along with `f_5` and `m_8`) to `junk_pirates.faction` for player character creation and NPC fleet commanders.

**Weapon Systems & Ballistic Calibration:**
- **Grape Family Overhaul:**
  - **Grape Cannon (Large):** Configured to a 2-shot burst $\times$ 18 submunition pellets with 150 Fragmentation damage per pellet (5,400 raw Hull burst), standardized to 15 OP and 28 Ammo.
  - **Grape Launcher (Medium):** Configured to a 2-shot burst $\times$ 14 submunition pellets with 150 Fragmentation damage per pellet (4,200 raw Hull burst), standardized to 9 OP and 14 Ammo.
  - **Grapeshot (Small):** Configured to a 1-shot burst $\times$ 14 submunition pellets with 150 Fragmentation damage per pellet (2,100 raw Hull burst), standardized to 5 OP and 5 Ammo.
  - **Variant Rebalancing:** Re-audited and trimmed 8 ship variants equipping Grape weapons to maintain 100% legal OP budgets.

### 3.5.4 Changelog

**Release 0.98a Readiness & Fixes:**
- **Balancing:** Standardized OP on all scavenged and pirate hull variants (-10% penalty applied) to comply with new standards.
- **Intel Integration:** Updated `AspCourierDepartureIntel` to display dynamic cargo inventories based on campaign conditions.
- **Bug Fixes:**
  - Removed duplicate `junk_pirates_cleat_Bomber` variant file that caused hard crashes on startup.
  - Corrected absolute sprite paths (`mods/JP_RC/graphics/...`) in `.ship` files to relative paths (`graphics/...`) to prevent invisible ships and crashes.
  - Purged trailing commas from Nexerelin JSON configurations (`pack.json`) to prevent strict parser crashes.
  - Added missing `syndicate_asp_familia` to `mod_factions.csv` to ensure Nexerelin loads it properly.

**New Additions:**
- Added the Raven-class destroyer (`junk_pirates_raven`) with custom standard variants and default roles.
- Added new test missions for mod testing and validation.
- Added `JP_NexIntegration` for safer and cleaner handling of optional Nexerelin dependencies.

**Code and Script Polish:**
- Replaced string equality operators (`==`) with `.equals()` across all Java scripts to prevent potential NullPointerExceptions.
- Standardized internal campaign memory keys with a consistent `$` prefix.
- Integrated commodity validation within the faction fleet AI.
- Refined ship hull data and faction-specific variant configurations.
- Removed the `.version` file and disabled version checker support.
- Excluded compiler-generated files (`sources.txt`) and build artifacts (`jars/`) from git tracking.

### 352 Changelog

**Junk Pirates Explorers:**
- Sit around planet thinking about what to do;
- Some go off and cause trouble
- Some go off to decivilized planets and have a party.
- Can be switched off.

**Intel added:**
- ASP Couriers variously
- Hit Fleets; should you attract the unwanted attention of the Familia
- Explorers; if you are friendly with the Junk Pirates you can keep up with the various things going on.

**General Changes:**
- As Familia are more prominent; have been balanced with better (combat) ships; better and more variants.
- Few balance tweaks including Goat, Gigantophis.
- Better variants generally (thanks particularly due to Vayra for the tournament builds which I adapted a little)
- Deco weapons; little aesthetic touches on some ships. Boxer, Boxenstein particularly.
- Lots of small aesthetic improvements in various ways over the sprites. Plenty more to do, but incremental improvements continue.
- PACK MESH - new built-in hullmod on bulk of pack fleet. Supports mixed fleets; mixed hulls.
