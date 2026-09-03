package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class JunkPiratesElectroChaffAndJunkjetPlugin extends BaseEveryFrameCombatPlugin {

    public static final boolean JP_DEBUG = false;

    // base range in su for EMP arcs to missiles/ships
    public static final float EMP_ARC_RANGE = 350f;

    // spread arc in degrees (width, not radius) for spawned chaff
    public static final float CHAFF_SPAWN_ARC = 130f;

    // colors for EMP arcs (should ideally match the shock beam and other weapon/proj stuff)
    private static final Color FRINGE_COLOR = new Color(135, 190, 150, 225);
    private static final Color CORE_COLOR = new Color(185, 255, 200, 255);

    public static final String ELECTROCHAFF_PROJ_BASE_ID = "junk_pirates_electrochaff";
    public static final String ELECTROCHAFF_LAUNCHER_ID = "junk_pirates_electrochafflauncher_copy";

    public static final String SCRAPCANNON_PROJ_ID = "junk_pirates_scrapcannon_shot";
    public static final String SCRAPNAILER_PROJ_ID = "junk_pirates_scrapnailer_shot";

    public static final String JUNKJET_PROJ_BASE_ID = "junk_pirates_junkjet_missile";
    public static final String JUNKJET_MICRO_PROJ_BASE_ID = "junk_pirates_micro_junkjet_missile";
    public static final String JUNKJET_WPN_COPY_PREFIX = "junk_pirates_junkjet_copy";
    public static final String JUNKJET_MICRO_WPN_COPY_PREFIX = "junk_pirates_micro_junkjet_copy";

    public static final String KEY = "junk_pirates_electroChaffPlugin";

    private static final HashMap<String, Float> JUNKJET_SPECS = new HashMap<>();
    private static HashMap<String, Integer> WEAPON_CHAFF_DATA = new HashMap<>();

    static {
        JUNKJET_SPECS.put("frag", 0.6969f); // warhead type, spawn weight inside spawn cycle
        JUNKJET_SPECS.put("explosive", 0.666f); // warhead type, spawn weight inside spawn cycle
        JUNKJET_SPECS.put("kinetic", 1.917f); // warhead type, spawn weight inside spawn cycle
        JUNKJET_SPECS.put("energy", 1.312f); // warhead type, spawn weight inside spawn cycle
        JUNKJET_SPECS.put("emp", 0.420f); // warhead type, spawn weight inside spawn cycle
    }
    private WeightedRandomPicker<String> specs = new WeightedRandomPicker<>();

    private CombatEngineAPI engine;
    public static final Logger log = Global.getLogger(JunkPiratesElectroChaffAndJunkjetPlugin.class);

    // these three are used to store stuff dynamically and need to be cleared
    private static HashMap<WeaponAPI, WeightedRandomPicker<Integer>> CHAFF_SPAWN_ROLLERS = new HashMap<>();
    private static Set<MissileAPI> CHAFF = new HashSet<>();
    private static Set<MissileAPI> CHAFF_SPENT = new HashSet<>();

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getCustomData().put(KEY, this);
        CHAFF_SPAWN_ROLLERS.clear();
        CHAFF.clear();
        CHAFF_SPENT.clear();
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        // triggers our fading chaff to EMP nearby missiles and add itself to the "spent" list
        // and removes spent chaff from the "active" list
        for (MissileAPI missile : CHAFF) {
            if (missile.isFading() && !missile.getEngineController().isFlamedOut()) {
                if (JP_DEBUG) {
                    log.info("chaff expiring, triggering arc for missiles");
                }
                expendChaff(missile, null, false);
            }
        }
        for (MissileAPI missile : CHAFF_SPENT) {
            if (CHAFF.contains(missile)) {
                if (JP_DEBUG) {
                    log.info("found spent chaff in chaff list, removing");
                }
                CHAFF.remove(missile);
            }
        }

        // replaces scrapjet missiles and fake electrochaff
        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        int size = projectiles.size();
        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI proj = projectiles.get(i);

            if (engine.isEntityInPlay(proj) && (ELECTROCHAFF_PROJ_BASE_ID.equals(proj.getProjectileSpecId()) || JUNKJET_PROJ_BASE_ID.equals(proj.getProjectileSpecId()) || JUNKJET_MICRO_PROJ_BASE_ID.equals(proj.getProjectileSpecId()))) {

                // setup stuff
                ShipAPI source = proj.getSource();
                WeaponAPI weapon = proj.getWeapon();
                Vector2f loc = proj.getLocation();
                float angle = proj.getFacing();
                Vector2f shipVelocity = (source != null) ? source.getVelocity() : new Vector2f();

                // replaces fake chaff from launcher shipsystems with real electrochaff
                if (ELECTROCHAFF_PROJ_BASE_ID.equals(proj.getProjectileSpecId())) {

                    engine.removeEntity(proj);
                    MissileAPI chaff = (MissileAPI) engine.spawnProjectile(source, weapon, ELECTROCHAFF_LAUNCHER_ID, loc, angle, shipVelocity);
                    CHAFF.add(chaff);

                    if (JP_DEBUG) {
                        log.info("replaced fake chaff particle from launcher with real chaff");
                    }
                }

                // replaces base scrapjet missiles with semirandom warheads
                if (JUNKJET_PROJ_BASE_ID.equals(proj.getProjectileSpecId())) {

                    // pick a "random" warhead
                    // j/k they're only semirandom they always cycle
                    // this was necessary otherwise you got two in a row of the same type way too often
                    // kinetic is weighted highest because you want it to come out first
                    // and emp is lowest because you want it to come out rarely and last
                    // pretty sure this list is shared across all instances of the weapon
                    // so if you fill a ship or even worse, a fleet, with them you might get some silliness
                    // especially if you're fighting another fleet that has one/some
                    // oh well
                    if (specs.isEmpty()) {
                        if (JP_DEBUG) {
                            log.info("scrapjet missile pick list was empty, refilling");
                        }
                        for (java.util.Map.Entry<String, Float> entry : JUNKJET_SPECS.entrySet()) {
                            specs.add(entry.getKey(), entry.getValue());
                        }
                    }
                    String pickedSpec = specs.pickAndRemove();
                    String spec = JUNKJET_WPN_COPY_PREFIX + "_" + pickedSpec;

                    // replace the missile with the picked warhead
                    engine.removeEntity(proj);
                    engine.spawnProjectile(source, weapon, spec, loc, angle, shipVelocity);
                    if (JP_DEBUG) {
                        log.info(String.format("replaced scrapjet warhead with [%s]", pickedSpec));
                    }
                }
                
                if (JUNKJET_MICRO_PROJ_BASE_ID.equals(proj.getProjectileSpecId())) {

                    // pick a "random" warhead
                    // j/k they're only semirandom they always cycle
                    // this was necessary otherwise you got two in a row of the same type way too often
                    // kinetic is weighted highest because you want it to come out first
                    // and emp is lowest because you want it to come out rarely and last
                    // pretty sure this list is shared across all instances of the weapon
                    // so if you fill a ship or even worse, a fleet, with them you might get some silliness
                    // especially if you're fighting another fleet that has one/some
                    // oh well
                    if (specs.isEmpty()) {
                        if (JP_DEBUG) {
                            log.info("scrapjet missile pick list was empty, refilling");
                        }
                        for (java.util.Map.Entry<String, Float> entry : JUNKJET_SPECS.entrySet()) {
                            specs.add(entry.getKey(), entry.getValue());
                        }
                    }
                    String pickedSpec = specs.pickAndRemove();
                    String spec = JUNKJET_MICRO_WPN_COPY_PREFIX + "_" + pickedSpec;

                    // replace the missile with the picked warhead
                    engine.removeEntity(proj);
                    engine.spawnProjectile(source, weapon, spec, loc, angle, shipVelocity);
                    if (JP_DEBUG) {
                        log.info(String.format("replaced scrapjet warhead with [%s]", pickedSpec));
                    }
                }
                
                
            }
        }
    }

    // returns the list of active chaff
    public static Set<MissileAPI> getChaff() {
        return Collections.unmodifiableSet(CHAFF);
    }

    // returns the list of spent chaff
    public static Set<MissileAPI> getChaffSpent() {
        return Collections.unmodifiableSet(CHAFF_SPENT);
    }

    // spawns a new chaff particle and adds it to the active list
    public static int spawnChaff(String projectile, WeaponAPI actualWeapon, Vector2f point, CombatEntityAPI target, boolean shieldHit) {

        if (target == null || actualWeapon == null || point == null) {
            if (JP_DEBUG) {
                log.info("target, weapon, or point passed to plugin was null, returning 0");
            }
            return 0;
        }

        Vector2f loc = point;
        String weaponId = actualWeapon.getId();

        // get the % spawn chance
        // first check if we have one saved
        Integer baseSpawnChance = WEAPON_CHAFF_DATA.get(weaponId + "_spawnChance");
        if (JP_DEBUG) {
            log.info(String.format("spawnChance for weapon [%s] is [%s] percent", weaponId, baseSpawnChance));
        }
        // if not, load one from the primary highlights in weapon_data.csv
        if (baseSpawnChance == null) {
            if (JP_DEBUG) {
                log.info(String.format("didn't find a spawnChance for [%s] in the list, loading from weapon_data.csv", weaponId));
            }
            String weaponData = actualWeapon.getSpec().getCustomPrimaryHL();
            int splitIndex = (weaponData != null) ? weaponData.indexOf("%") : -1;
            String subString;
            if (splitIndex > 0) {
                int start = Math.max(0, splitIndex - 2);
                subString = weaponData.substring(start, splitIndex).trim();
            } else {
                // if we don't find a percentage, default to 25%
                subString = "25";
            }
            baseSpawnChance = Integer.valueOf(subString);
            WEAPON_CHAFF_DATA.put(weaponId + "_spawnChance", baseSpawnChance);
            if (JP_DEBUG) {
                log.info(String.format("loaded spawnChance [%s] percent for [%s] from weapon_data.csv, saving it to the list", baseSpawnChance, weaponId));
            }
        }

        // normalize <spawn chance> over <rolls> for more reliability
        int rolls = 20;
        int spawns = (baseSpawnChance <= 0) ? 0 : Math.round((rolls * baseSpawnChance) / 100f);

        // get the chaff min/max
        // first check if we have some saved
        Integer min = WEAPON_CHAFF_DATA.get(weaponId + "_chaffMin");
        Integer max = WEAPON_CHAFF_DATA.get(weaponId + "_chaffMax");
        if (JP_DEBUG) {
            log.info(String.format("min/max chaff for [%s] is [%s]-[%s]", weaponId, min, max));
        }
        // if not, load one from the primary highlights in weapon_data.csv
        // note this will break with multi-digit chaff min/max values
        // but you wouldn't do that... would you?
        if (max == null) {
            if (JP_DEBUG) {
                log.info(String.format("didn't find a min/max chaff for [%s] in the list, loading from weapon_data.csv", weaponId));
            }
            String weaponData = actualWeapon.getSpec().getCustomPrimaryHL();
            int splitIndex = weaponData.indexOf("Electrostatic Chaff");
            if (JP_DEBUG) {
                log.info(String.format("loaded splitIndex [%s]", splitIndex));
            }
            String subString = null;
            if (splitIndex > 0) {
                subString = weaponData.substring(splitIndex - 4, splitIndex).trim();
            }
            if (subString != null && !subString.isEmpty()) {
                try {
                    min = Integer.valueOf(subString.substring(0, 1));
                    max = Integer.valueOf(subString.substring(subString.length() - 1, subString.length()));
                } catch (Exception ex) {
                    min = 1;
                    max = 1;
                }
            } else {
                min = 1;
                max = 1;
            }
            if (JP_DEBUG) {
                log.info(String.format("loaded subString [%s], min = [%s], max = [%s]", subString, min, max));
            }
            // if we fucked up and its way out of bounds, use 1-1
            if (min > 9 || max < 1) {
                min = 1;
                max = 1;
            }
            WEAPON_CHAFF_DATA.put(weaponId + "_chaffMin", min);
            WEAPON_CHAFF_DATA.put(weaponId + "_chaffMax", max);
            if (JP_DEBUG) {
                log.info(String.format("loaded min/max chaff [%s]-[%s] for [%s], saving it", min, max, weaponId));
            }
        }

        // we're going to use a random distribution of <spawns> results within a cycle of <rolls>, to give a more even spread
        // i.e. if you use 20 rolls and a weapon with 15% spawnChance, you're guaranteed exactly 3 spawns out of 20 hits
        // but WHICH 3 is still random
        WeightedRandomPicker<Integer> chaffSpawnRoller = CHAFF_SPAWN_ROLLERS.get(actualWeapon);
        if (chaffSpawnRoller == null) {
            chaffSpawnRoller = new WeightedRandomPicker<>();
            if (JP_DEBUG) {
                log.info(String.format("starting a new roller for weapon [%s]", actualWeapon));
            }
        }
        if (chaffSpawnRoller.isEmpty()) {
            if (JP_DEBUG) {
                log.info(String.format("roller for weapon [%s] was empty, filling it now", actualWeapon));
            }
            // first we place the successes
            for (int i = 0; i < spawns; i++) {
                int spawn = MathUtils.getRandomNumberInRange(min, max);
                chaffSpawnRoller.add(spawn);
                if (JP_DEBUG) {
                    log.info(String.format("added [success: spawn %s chaff] to roller for weapon [%s]", spawn, actualWeapon));
                }
            }
            // now we place the failures
            if (rolls - spawns > 0) {
                for (int i = 0; i < rolls - spawns; i++) {
                    chaffSpawnRoller.add(0);
                    if (JP_DEBUG) {
                        log.info(String.format("added [failure: spawn 0 chaff] to roller for weapon [%s]", actualWeapon));
                    }
                }
            }
        }
        // now we roll
        int numSpawned = chaffSpawnRoller.pickAndRemove();

        // roller SHOULD be saved automatically b/c it's just an object reference?
        // but we need this here anyway to save it if we made a new one
        CHAFF_SPAWN_ROLLERS.put(actualWeapon, chaffSpawnRoller);

        if (JP_DEBUG) {
            log.info(String.format("plugin spawning [%s] chaff at [%s], called by [%s] hitting [%s]", numSpawned, point, projectile, target));
            for (int num : chaffSpawnRoller.getItems()) {
                log.info(String.format("Roll remaining in roller: [%s]", num));
            }
        }

        if (numSpawned > 0) {
            for (int i = 0; i < numSpawned; i++) {
                // setup stuff (roll for random direction of particle "spread")
                float baseAngle = Misc.getAngleInDegrees(point, actualWeapon.getLocation()) - (CHAFF_SPAWN_ARC / 2f);
                float angle = (float) (baseAngle + (Math.random() * CHAFF_SPAWN_ARC));
                loc = point;

                // move chaff spawn point outside of ship/shield bounds so it doesn't hit the ship/shields and disappear
                // this stumped me for fucking HOURS before i figured it out btw
                boolean collision = CollisionUtils.isPointWithinBounds(loc, target);
                if (shieldHit) {
                    collision = CollisionUtils.isPointWithinCollisionCircle(loc, target);
                }
                float moveDist = 10f;
                int maxIterations = 25;
                while (collision && maxIterations-- > 0) {
                    if (JP_DEBUG) {
                        log.info(String.format("loc [%s] is within [%s] bounds, moving [%s] su at [%s] degree angle", loc, target, moveDist, angle));
                    }
                    loc = MathUtils.getPointOnCircumference(loc, moveDist, angle);
                    collision = CollisionUtils.isPointWithinBounds(loc, target);
                    if (shieldHit) {
                        collision = CollisionUtils.isPointWithinCollisionCircle(loc, target);
                    }
                }

                MissileAPI chaff = (MissileAPI) Global.getCombatEngine().spawnProjectile(actualWeapon.getShip(), null, ELECTROCHAFF_LAUNCHER_ID, loc, angle, target.getVelocity());
                CHAFF.add(chaff);
            }

            if (JP_DEBUG) {
                log.info(String.format("spawned [%s] chaff at [%s], adding to chaff list", numSpawned, loc));
            }
        }

        return numSpawned;
    }

    // "spends" the chaff particle, removing it from the active list and adding it to the "spent" list
    // triggers an EMP arc dealing EMP and Energy damage as set in weapon_data.csv under the chaff projectile
    // targets a ship or, if a null target is passed, a nearby enemy missile (if available)
    public static void expendChaff(MissileAPI chaff, CombatEntityAPI targetShip, boolean piercedShield) {
        if (JP_DEBUG) {
            log.info(String.format("plugin trying to expend chaff [%s] for EMP arc to target [%s]", chaff, targetShip == null ? "missile" : targetShip));
        }
        CombatEntityAPI target;

        if (targetShip != null) {
            target = targetShip;
        } else {
            WeightedRandomPicker<MissileAPI> targets = new WeightedRandomPicker<>();
            List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(chaff.getLocation(), EMP_ARC_RANGE);
            for (MissileAPI m : missiles) {
                if (m.getOwner() != chaff.getOwner() && !m.isFlare() && !m.isDecoyFlare() && !m.isFizzling() && !m.isFading()) {
                    targets.add(m);
                }
            }
            target = targets.pick();
            if (JP_DEBUG) {
                log.info(String.format("no ship target, picking missile target [%s] for chaff arc", target));
            }
        }

        if (target == null) {
            if (JP_DEBUG) {
                log.info("no target found");
            }
        } else {

            if (chaff == null || CHAFF_SPENT.contains(chaff)) {
                if (JP_DEBUG) {
                    log.info("chaff was null or spent, returning");
                }
                return;
            }

            float dam = chaff.getDamageAmount();
            float emp = chaff.getEmpAmount();

            // actually spawn the EMP arc
            if (piercedShield) {
                Global.getCombatEngine().spawnEmpArcPierceShields(chaff.getSource(),
                        chaff.getLocation(),
                        chaff,
                        target,
                        DamageType.ENERGY,
                        dam,
                        emp,
                        6969420f,
                        "tachyon_lance_emp_impact",
                        15f,
                        FRINGE_COLOR,
                        CORE_COLOR);
            } else {
                Global.getCombatEngine().spawnEmpArc(chaff.getSource(),
                        chaff.getLocation(),
                        chaff,
                        target,
                        DamageType.ENERGY,
                        dam,
                        emp,
                        6969420f,
                        "tachyon_lance_emp_impact",
                        15f,
                        FRINGE_COLOR,
                        CORE_COLOR);
            }

            if (JP_DEBUG) {
                log.info(String.format("chaff [%s] did EMP arc for [%s/%s] Energy/EMP to target [%s]", chaff, dam, emp, target));
            }
        }

        if (!CHAFF_SPENT.contains(chaff)) {

            if (JP_DEBUG) {
                log.info(String.format("adding chaff [%s] to spent chaff list", chaff));
            }
            CHAFF_SPENT.add(chaff);
            chaff.flameOut();
        }
    }
}
