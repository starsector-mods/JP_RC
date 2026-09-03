# Junk Pirates Changelog

## [3.6.1] - 2026-09-03

**LunaLib Settings Integration**
- **In-Game Settings UI:** Integrated native LunaLib settings menu support via `data/config/LunaSettings.csv` with 3 curated categories (Core Toggles, Fleets & Encounters, Procedural Generation) to dynamically toggle ASP, PACK, and Junk Pirates systems, courier/hit squads, fleet frequency/density multipliers, and procgen parameters.
- **Graceful Fallback:** Added safe reflection-isolated loader (`JunkPiratesLunaConfig.java`) that preserves baseline defaults from `mendoncaModSettings.json` if LunaLib is not installed, preventing class loader crashes.

**Campaign Fleets, World Economy, & AI Overhaul**
- **Fleet Spawning & Listeners Restored:** Fixed integer truncation bug in `SyndicateAspFleetManager` that previously suppressed all ASP Courier fleet spawns on standard sector generations. Registered transient listeners on new game across all fleet managers (`SyndicateAspFleetManager`, `SyndicateAspHitSquadFleetManager`, `JunkPiratesExplorerFleetManager`) so hit squads, bounty escalations, and fleet respawns activate without requiring a save reload.
- **Assignment AI Crash & Timeout Fixes:** Fixed game-crashing `NullPointerException` on `PATROL_SYSTEM` with null targets in `JunkPiratesAnarchistAssignmentAI`. Fixed self-targeting despawn calls and replaced premature 3/8-day timeouts with persistent 1000-day journey assignments for cross-sector couriers and explorers.
- **Intel Lifecycle & Memory Leaks:** Implemented automatic 60-day lifecycle cleanup for `SyndicateAspCourierDepartureIntel` and `JunkExplorerDecisionIntel`, preventing unbounded memory leaks and map UI clutter. Fixed potential CTD in courier map arrow rendering when customer factions are null.
- **ASP Courier Economy & Scavenging:** Corrected credit chip cargo calculation (`(creds / 10)` -> 0) so courier fleets actually carry valuable ASP credit chips. Fixed `TAG_HEAVYINDUSTRY` query in explorer fleet bonus calculation and added fallback for scavenge expeditions targeting uninhabited systems.
- **Spinerette Boss Encounter & Megastructure:** Directly instantiated the Spinerette flagship in `JunkPiratesLostTechSalvageGen` to guarantee the Automata Cloud boss encounter triggers. Added `"non_expiring"` tag in `custom_entities.json` and reset `$salvaged` flags in `SpineretteRespawnManager` to preserve the 60-day reboot cycle. Rebalanced astronomical salvage drops (10,000 -> 3,300 volume).
- **World, System Orbits & Economy:** Fixed Hope Rings terrain collision mis-anchored to the star in `Brehinni.java`. Fixed Petra Relay angle typo (2250° -> 225°) and removed invalid gas giant farming in `Canis.java`. Added Military Submarket to Paddington and removed fleet stacking exploit on Fireworks Factory (24 patrol fleets). Added `SurveyLevel.FULL` and `population_X` condition generation in `AddMarketPlace.java`.
- **Dialogue Softlocks & Parsing:** Added `cutCommLink` exit option to 13 hostile comm rules in `rules.csv` to prevent dialog lockups. Added `$entity.` prefix to ASP courier conditions. Exploded unparsed literal `"OR"` text strings in Junk Pirate Explorer comm greetings into distinct randomized rules.
- **Procgen Anarchist Themes:** Fixed inverted rim constellation sorting and captured station generation rolls in `JunkPiratesAnarchistThemeGenerator`.

**Campaign, Blueprint Drops, & Nexerelin Integration**
- **Blueprint Package Salvage Restored:** Registered all 6 blueprint packages into vanilla salvage pools (`blueprints`, `rare_tech`). Fixed outdated `junk_pirates_bp` and `pack_bp` tags in procedural drop groups (`junk_anarchy1`, `junk_anarchy2`, `junk_scrap2`).
- **13 PACK Skins Unlocked:** Migrated 13 PACK variant skins from legacy `pack_bp` to `junk_pirates_packprime_bp` / `junk_pirates_pack_bp`, ensuring they unlock properly when learning PACK blueprint packages.
- **ASP Syndicate Fleet Identity:** Fixed priority ships tag leak in `syndicate_asp.faction` that caused ASP fleets to spawn vanilla midline/hightech ships instead of ASP hulls. Fixed `knownFighters` syntax bug that prevented learning the Death Rattle wing.
- **Fleet Role Realignment:** Moved Tangerine (Filthbag) frigate from destroyer to frigate role. Removed Spinerette boss station from wandering capital combat fleets in `default_ship_roles.json`. Added missing variants to role pools.
- **Nexerelin Familia Start Enabled:** Added `"startingFaction": true` to `syndicate_asp_familia.json`, making the custom Familia starting scenario accessible in character creation.
- **Mod Integrations & Mining:** Added missing core ships to Industrial Evolution printing/reverse-engineering whitelists (`syndicate_asp_cerberus`, `hammerhead`, `mercury`, `vigilance`, `junk_pirates_raven`). Configured custom mining fleets and vengeance fleets for P.A.C.K. and ASP Syndicate.

**Engine Limits, Hullmods, & Exploit Fixes**
- **Engine OP/Cap Hardcaps:** Fixed critical `pack_bulldog_bullseye_Bullseye.variant` exceeding engine hard caps (53 capacitors/52 vents on a 50 limit). Removed illegal Safety Overrides from cruiser variants.
- **Hullmod Exploits Removed:** Set `hidden: TRUE` on `pack_overclocked_ca`, `junk_pirates_premil`, `syndicate_asp_mod`, and Commissioned Crew hullmods to prevent 0 OP free installations on arbitrary player ships.
- **Scrap Damper Exploit:** Fixed multiplicative damage stacking bug in `JunkPiratesDamperStats.java` so active + passive mitigation scales properly to exactly 50% net damage reduction rather than 57.5%.

**Weapons, Performance, & VFX Tuning**
- **Affenpinscher Beam:** Removed immortal missile HP freeze. Inversely scaled tumble chance with missile durability (`Math.min(0.60f, 150f / maxHp)`). Heavy torpedoes take normal damage and cannot be permastunned.
- **Stygian Drill Performance:** Reined in physical debris spawn rates (0.02s -> 0.08s) and particle lifetime (5-10s -> 1-2s) to prevent extreme framerate collapse during prolonged firing. Range reduced to 800.
- **Weapon Rebalancing:** Rebalanced Cutlass (600 range, 75 dmg), Typewriter (25 dmg, 20 energy, 2-8 spread), Micro Scrapjet MLRS (1150 range, 200 dmg, 0.1 regen), Scatter PD (400 range, 40 dmg), Viper Pulse Cannon (900 range, 2.2s chargedown), and Grape Cannons (reduced submunitions). Fixed 100% Chaff spawn bug on Scrapjets.

**Fighter Wings, Empty Decks & Fleet Synchronization**
- **Flight Decks Filled:** Assigned proper fighter wings to all 8 empty built-in carrier bays across Reaper, Bulldog, Kraken, Goat, GFB, and King Cobra variants.
- **Wing Tuning:** Rebalanced OP costs and durability for Oblonsky (9 OP, 12s refit), Ivan (9 OP, 14s refit), Cleat (8 OP, 450 HP, 65 Armor), Spike (7 OP), and Splinter (8 OP).
- **DP/FP Synchronization:** Synchronized `fleet pts` and `supplies/rec` across 40 hulls to ensure campaign autoresolve matches combat Deployment Points (e.g. Kraken 35/35, Bullseye 36/36, Reaper 32/32). Normalized crippling 0.8 shield upkeeps down to 0.35 on Bully Kutta and Orcenstein.



**P.A.C.K. Systems & Balance Tuning**
- **Custom System Limiter:** Nerfed *Tri-Feed Overcharge* and *Ridgeback Protocol* RoF/Flux modifiers from an unstable 25% to a mathematically stable 15%.
- **Clean System Visuals:** Completely stripped floating text banners, hull jitter, and screen-clutter artifacts from Tri-Feed Overcharge, Ridgeback Protocol, and Alpha Call in favor of clean weapon mount glows and tactical reticles.
- **Deployment Costs:** Increased Deployment Points by +2 for the *Ridgeback* (9 DP), *Ridgeback X* (11/12 DP), and *Komondor* (14 DP) to accurately reflect their custom system combat lethality.
- **Bulldog Point Defense:** Converted inner mounts (WS 009, WS 010) on the *Bulldog* and *Bulldog (BE)* to built-in `Affenpinscher` PD beams. Adjusted both mounts to a neutral 0-degree forward resting angle with full 360-degree tracking arcs.
- **Lore Expansion:** Deployed in-universe P.A.C.K. Engineering Directive patch-notes and overhauled P.A.C.K. ship and system lore in `descriptions.csv`.

**ASP Syndicate Corporate Doctrine**
- **Global Fleet Capability:** Integrated the "Corporate Flux Grid" doctrine directly into the global `ASP Syndicate Sponsored` hullmod. The entire ASP fleet now natively receives **+15% Flux Capacity** but suffers a **-10% Flux Dissipation** malus to solidify their slow-venting, high-capacity shield-tank identity.
- **ASP Cerberus Shielding:** Granted a 90-degree frontal shield (0.8 efficiency) directly to the `syndicate_asp_cerberus.skin` file to capitalize on its expanded flux pool.

**Comprehensive OP Loadout Audit**
- **Illegal Variant Cleansing:** Ran a mathematical script sweep across all `.variant` files to detect and fix illegal loadouts. Stripped excess vents/capacitors from over-budget variants (*Shar Pei*, *Pitbull*, *Tangerine*, *Langoustine*) to ensure they fit their hull OP limits, preventing in-game autofit bugs.
- **Under-OP Variant Maximization:** Fixed severely under-budget AI variants that were leaving 40-100 OP unspent. Fully maximized the *ASP Kingcobra*, *ASP Gigantophis*, *P.A.C.K. Spinone*, and *P.A.C.K. Bully Kutta* by adding premium hullmods (Heavy Armor, Expanded Deck Crews, Targeting Units) and maxing out their flux grids.


## [3.6.0] - 2026-08-30

**Comprehensive Fleet Balance & Systemic Overhaul**
- **Double Fighter Bay Bug Elimination:** Resolved the engine-level fighter bay duplication glitch across all hulls with built-in wings (*The Reaper*, *Bulldog*, *GFB*, *Goat*, *Bedlington*, *Kraken*, *Ridgeback*, *Ridgeback X*, *Labrador*, *King Cobra*). Assignable bays in `ship_data.csv` now correctly reflect intended extra decks without spawning duplicate ghost wings.
- **Fighter Wing Cost & Durability Balancing:**
  - *Oblonsky Interceptor:* Re-costed from 0 OP to **5 OP** (8s refit).
  - *Hood Drone:* Rebalanced from 4 OP to **8 OP**, wing count from 5 to **4**, refit to **9s**.
  - *Mitya Bomber:* Cost reduced from 18 OP to **15 OP**, HP increased from 100 to **250**.
  - *Cleat Bomber:* Standardized to **10 OP**, HP buffed from 100 to **200**.
  - *Insult Drone:* Removed `no_weapon_flux` so 3x Phase Beams properly respect flux capacity and dissipate between bursts.
- **Boxenstein (Blue) Exploit Fix:** Removed free built-in Safety Overrides from 1,150-armor cruiser-tier hull in `junk_pirates_boxenstein_blue.skin`. Replaced with *Auxiliary Thrusters* & *Insulated Engine*; normalized Boxenstein stats to 95 OP and 850 Armor.
- **Cruiser Burst Jets Tuning:** Reduced flat speed burst in `JunkPiratesCruiserBurstJets.java` from +200 flat speed to a balanced **+80 flat speed**.

**Turbot & Heavy Interdiction Suite Overhaul**
- **Heavy Interdiction Suite:** Complete mechanical and lore overhaul in `JunkPiratesInterdictorStats.java`. System now siphons 50% of user's flux, discharging 25% as soft flux and 25% as hard flux directly onto target (24s cooldown, 10% CR per activation).
- **Interdictor Housing Hullmod:** Added built-in `JunkPiratesInterdictorHousing.java` hullmod capping maximum CR at 85%.
- **Turbot Destroyer Stats:** Overhauled base stats to 3,000 HP, 500 Armor, 5,800 Max Flux, 70 OP, 85 Speed, 110 Accel, 25 Turn Rate, and 210s PPT.
- **Rich Lore Expansion:** Expanded full qualitative lore in `descriptions.csv` detailing the technical footprint and heavy heatsink housings of the Interdictor suite.

**Top Overpowered Ships Balanced (100% Mount Integrity Preserved)**
- **Raven:** Re-costed to **24 DP**, Max Flux to 8,000, Dissipation to 520, OP to 115; removed free built-in ITU to respect cruiser OP budgeting.
- **Orcenstein:** Re-costed to **28 DP**; reduced frontal shield arc from 300° to **210°**.
- **Magpie:** Re-costed to **24 DP**, Dissipation to 550; tuned built-in PICA damage from 350 to **260/shot** (380 flux).
- **Tangerine & Tangerine (Blue):** Re-costed to **10 DP** (Base) / **11 DP** (Blue), Armor to 275, Dissipation to 240/260. Capped Metastable Drive speed bonus to **+35%** and stripped board-wiping overload EMP nuke; set Blue Zapper zap interval to **2.5s** and Haymaker cooldown to **3.0s**.
- **Shar Pei (CB):** Re-costed to **14 DP**; swapped High Energy Focus for active Flare Launcher to prevent instant 10-mount alpha deletion upon unphasing.
- **Komondor:** Re-costed to **12 DP**, Dissipation to 275; adjusted shield to **140° Front** (1.0 eff).
- **Spinone:** Re-costed to **22 DP**, HP to 6,500, Armor to 650. Capped *Trilateral Augmentation* duration to **8s** (14s cooldown) with **+25%** missile damage ceiling; reduced Affenpinscher PD from 450 to **250 DPS**.
- **Bulldog (BE):** Re-costed to **48 DP** to balance its 3 converging Large Ballistics and Accelerated Ammo Feeder.
- **King Cobra & Familia:** Re-costed to **42 DP** (Base, 3 assignable + 1 built-in wing) and **52 DP** (Familia, 3 Large Missiles).
- **The Reaper & Orca:** Buffed Reaper to **18,000 HP**, **1,400 Armor**, **210 OP**, **36 DP**, and **15%** passive damper reduction. Buffed Orca to **13,500 HP**, **1,150 Armor**, **15,000 Flux**, **650 Diss**, **220 OP**, **38 DP** across 4 assignable decks.

**Underpowered Ships Buffed & Destroyer Normalization**
- **Stoat A & B:** Dropped to **2 DP**, buffed to **25 OP**, **1,200 Max Flux**, **120 Cargo**, and **40 Accel** with updated variants.
- **Hammer:** Dissipation buffed from 90 to **160**, Max Flux to **2,200**, OP to **45 OP** (5 DP).
- **Wirefox (SH):** Re-costed to **4 DP**, Dissipation to **160**, Max Flux to **1,800**.
- **Hognose:** Re-costed to **8 DP**, OP buffed to **70 OP**, Armor to **375**, Speed to **75**.
- **Clam:** Re-costed to **5 DP** (affordable 360° shield escort).
- **Mastiff:** Dissipation buffed to **500**, Shield Efficiency to **0.95** (15 DP).
- **Copperhead:** Reclassified from Cruiser to **DESTROYER** in `syndicate_asp_copperhead.ship` (10 DP, 85 OP, 250 Diss).
- **Langoustine & Satsuma:** Normalized Dissipation to **350 / 320** and speeds to **105 / 95** (13 DP / 11 DP).
- **Schnauzer:** Normalized base Dissipation from 310 to **220**.
- **Jackdaw:** Reclassified as **Light Cruiser** (16 DP, 120 OP, 500 Diss, 65 Speed, 140° Omni Shield).
- **Octopus:** Re-costed to **12 DP**, main hull buffed to **4,500 HP**, **650 Armor**, **3,200 Flux**, **220 Diss**; Shield Module buffed to **3,500 HP** and **7,500 Flux**.



**Major Overhaul & Engine Modernization**
- **Java 17 Migration:** Completely recompiled the entire codebase for strict Java 17 bytecode compatibility with Starsector 0.98a.
- **100% Save-Game Safety:** Injected `readResolve()` methods into every transient `EveryFrameScript`, fleet assignment AI, and intel plugin. The mod can now be installed or updated mid-playthrough without silently corrupting save files.
- **Crash Prevention & Enum Sanitization:** Scrubbed `weapon_data.csv` and weapon scripts of invalid `AIHints` (e.g. invalid `BEAM` enum) to prevent engine startup crashes.
- **Rules Engine Sanitization:** Scrubbed `rules.csv` of all dangling commas, malformed options, and missing trigger columns that previously caused `IndexOutOfBoundsException` failures.

**Stygian Drill & The Reaper Overhaul**
- **Stygian Drill Combat Rework:** The Reaper's built-in spinal drill now continuously scales output from **1,000 up to 1,800 Fragmentation DPS** over 3.0 seconds of sustained fire.
- **Cinematic Beam VFX Progression:** The drill's beam focuses from a wide industrial plume into a piercing red-to-orange thermal cutter with a brilliant solar white-yellow core and dynamic firing SFX loop.
- **Physical Hull Debris & Ricochet Sparks:** Continuous drilling now generates directional spark deflection showers and actively tears physical chunks of metallic armor/hull plating off unshielded targets.
- **Dual System Setup on The Reaper:** Integrated right-click **Scrap Damper** defense system (50% active damage reduction with 15% passive integrity field) alongside primary **Heavy VIE Plug Jets** granting **+40 forward speed** burst and defensive cryo-chaff flares.
- **ASP Viper Pulse Cannon:** Replaced Gigantophis (F) TPC with a custom built-in heavy energy weapon featuring high-impact kinetic shield suppression and composite SFX.
- **Dynamic Combat UI:** Added live damage status text overlay (`Damage: [CURRENT] / 1800 DPS`) and rich Codex/Refit breakdown cards.

**Economy, Campaign & Fleet AI Fixes**
- **Fleet Logistics Restoration:** Fixed critical fleet manager bugs where ASP Hit Squads and Junk Pirate Explorers spawned with 0 fuel/cargo tankers, preventing fleets from running out of fuel and suffering CR death in hyperspace.
- **Faction Ship Knowledge Restored:** Added the missing *Tangerine* frigate to the Junk Pirates faction and the *Bulldog* battleship to the P.A.C.K. faction `knownShips` and blueprint tables.
- **Blueprint Package Tags Fixed:** Corrected all broken `junk_pirates_bp` and `pack_bp` tags across `ship_data.csv` to ensure all custom ships are legitimately discoverable via blueprint packages.
- **Market Defenses Injected:** Added ground defenses to *Ear Burns* and orbital station defenses to *Bear's Pit* to prevent undefended market raids.
- **Familia HQ Money Exploit:** Fixed base upkeep in `industries.csv` from 90 to 75,000 credits to balance mid-game syndicate economies.
- **Underworld Dialogue Polish:** Eliminated all placeholder strings (`WHAT`, `INITIALISE`) across Courier, Hit Squad, and Explorer fleet assignment AI scripts.
- **Portrait Corrections:** Resolved gender-swapped portrait lists in `pack.faction`.

**Ship & Weapon Balance Sweep**
- **Under-OP Variant Optimization:** Swept all `.variant` files and injected standard QoL hullmods (*Augmented Drive Field*, *Flux Distributors*, *Heavy Armor*, *Hardened Shields*) into 24 under-budget ship variants.
- **DP Re-Scaling:** The ASP King Cobra (40 DP), The Reaper (38 DP), Boxenstein (14 DP), and Boxer (12 DP) were appropriately re-costed to match their extreme armor and fighter capacities.
- **Kinetic DPS Tuning:** Validated medium kinetics (`pack_ripsaw` and `syndicate_asp_typewriter`) to remain strictly within the 300 DPS threshold.
- **Fighter OP Balancing:** The Cleat Strike Bomber (8 OP), Spike Heavy Fighter (10 OP), and Levin Strike Fighter (10 OP) were brought strictly into line with vanilla paradigms.
- **Zero-Cooldown Exploits Removed:** Injected a 3-second hard cooldown into `ship_systems.csv` for Phase Drones, TNT Drones, Ion Drones, and the P.A.C.K. `pack_flux_divert` shield toggle to prevent macro-flicker exploits.
- **Bomber Speed Hack Fixed:** Removed the flat +70 max speed and +50 acceleration buffs from the Trilateral Augmentation script that was breaking standard bomber engagement logic.
- **Light Excimer:** Nerfed energy DPS from 250 down to 100 to fit its 4 OP small-slot footprint.

**Lore & Visual Polish**
- **Complete Lore Overhaul:** Every single piece of equipment, ship hull, faction, and industry now has rich, immersive lore descriptions that capture the chaotic underworld vibe.
- **Explicit Fighter Roles:** Fighter wings now feature explicitly named roles (e.g. "Constrictor Support Fighter") for absolute clarity in the refit screen.
- **Cinematic Engine Systems:** The *Plug Jets* and *Kraken Retreat* ship systems have been completely refactored with forward-lock propulsion, massive chaff cones, and Cryoflamer particle trails.


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
