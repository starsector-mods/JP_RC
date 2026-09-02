package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class PackAlphaCallStats extends BaseShipSystemScript {

    public static final Object KEY_JITTER = new Object();
    public static final Object KEY_TARGET_JITTER = new Object();

    public static final float AURA_RANGE = 1500f;
    public static final float TARGET_MAX_RANGE = 2000f;

    public static final float LEAD_DISSIPATION_PERCENT = 15f;
    public static final float LEAD_RECOIL_REDUCTION = 30f;

    public static final float ALLY_SPEED_PERCENT = 20f;
    public static final float ALLY_ACCEL_PERCENT = 25f;
    public static final float ALLY_ROF_PERCENT = 15f;
    public static final float ALLY_DISSIPATION_PERCENT = 15f;
    public static final float ALLY_FIGHTER_SPEED_PERCENT = 25f;

    public static final float TARGET_WEAPON_TURN_PENALTY = 20f;
    public static final float TARGET_SHIELD_TURN_PENALTY = 10f;

    // Visual Palette (Softened for clean, non-blinding tactical effects)
    private static final Color GOLD_FLASH = new Color(255, 210, 120, 90);
    private static final Color GOLD_PULSE = new Color(255, 180, 50, 100);
    private static final Color WEAPON_ACCENT = new Color(255, 160, 45, 45); // Subtle, low-intensity weapon slot accent
    private static final Color JITTER_COLOR = new Color(255, 175, 45, 60);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 120, 15, 80);
    private static final Color ARC_FRINGE = new Color(255, 150, 30, 140);
    private static final Color ARC_CORE = new Color(255, 240, 190, 180);

    // Quarry (Designated Target) Visuals
    private static final Color TARGET_JITTER_COLOR = new Color(255, 55, 25, 130);
    private static final Color TARGET_UNDER_COLOR = new Color(255, 30, 10, 170);
    private static final Color TARGET_SPARK = new Color(255, 85, 35, 220);
    private static final Color TARGET_RING = new Color(255, 60, 20, 160);

    private final IntervalUtil arcInterval = new IntervalUtil(0.60f, 0.85f);
    private final IntervalUtil pulseInterval = new IntervalUtil(2.2f, 2.8f);
    private final IntervalUtil sparkInterval = new IntervalUtil(0.30f, 0.50f);
    private boolean waveSpawned = false;
    private boolean targetTagged = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        final String targetKey = ship.getId() + "_alpha_call_target";

        // Acquire or resolve quarry
        ShipAPI quarry = null;
        Object storedTarget = engine.getCustomData().get(targetKey);
        if (storedTarget instanceof ShipAPI && ((ShipAPI) storedTarget).isAlive()) {
            quarry = (ShipAPI) storedTarget;
        } else {
            quarry = findQuarry(ship);
            if (quarry != null && state != State.IDLE) {
                engine.getCustomData().put(targetKey, quarry);
            }
        }

        // 1. Initial Soft Sonar Shockwave Pulse
        if (state == State.IN && !waveSpawned) {
            waveSpawned = true;
            spawnExpandingSonarWave(engine, ship.getLocation());

            // If quarry is present on activation, ping the quarry with a subtle target reticle flash
            if (quarry != null && !quarry.isHulk()) {
                spawnQuarryPing(engine, quarry);
            }
        } else if (state == State.OUT || state == State.IDLE) {
            waveSpawned = false;
            targetTagged = false;
            engine.getCustomData().remove(targetKey);
        }

        // 2. Ambient Radar Pulse every ~1.3 seconds while active
        pulseInterval.advance(engine.getElapsedInLastFrame());
        if (pulseInterval.intervalElapsed() && effectLevel >= 0.8f) {
            spawnSubtlePulse(engine, ship.getLocation());
        }

        // 3. Buff the Lead Ship (The Alpha)
        stats.getFluxDissipation().modifyPercent(id, LEAD_DISSIPATION_PERCENT * effectLevel);
        stats.getMaxRecoilMult().modifyMult(id, 1f - (LEAD_RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilPerShotMult().modifyMult(id, 1f - (LEAD_RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilDecayMult().modifyPercent(id, LEAD_RECOIL_REDUCTION * effectLevel);

        if (effectLevel > 0) {
            // Clean, understated weapon mount glow and subtle engine flare
            ship.setWeaponGlow(effectLevel, WEAPON_ACCENT, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
            if (ship.getEngineController() != null) {
                ship.getEngineController().extendFlame(KEY_JITTER, 1.2f * effectLevel, 1.2f * effectLevel, 1.2f * effectLevel);
            }
        }

        // 4. Telemetry Arcs Interval
        arcInterval.advance(engine.getElapsedInLastFrame());
        boolean triggerArcs = arcInterval.intervalElapsed() && effectLevel >= 0.4f;

        // 5. Buff Allied Escorts in the 1500 su coordination aura
        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(ship.getLocation(), AURA_RANGE);
        for (ShipAPI ally : nearbyShips) {
            if (ally == ship || ally.isHulk() || ally.getOwner() != ship.getOwner()) {
                continue;
            }

            MutableShipStatsAPI aStats = ally.getMutableStats();

            if (ally.isFighter()) {
                aStats.getMaxSpeed().modifyPercent(id, ALLY_FIGHTER_SPEED_PERCENT * effectLevel);
                aStats.getAcceleration().modifyPercent(id, ALLY_FIGHTER_SPEED_PERCENT * effectLevel);
                if (effectLevel > 0) {
                    ally.setWeaponGlow(effectLevel, WEAPON_ACCENT, EnumSet.allOf(WeaponType.class));
                    if (ally.getEngineController() != null) {
                        ally.getEngineController().extendFlame(KEY_JITTER, 1.25f * effectLevel, 1.25f * effectLevel, 1.25f * effectLevel);
                    }
                }
            } else {
                aStats.getMaxSpeed().modifyPercent(id, ALLY_SPEED_PERCENT * effectLevel);
                aStats.getAcceleration().modifyPercent(id, ALLY_ACCEL_PERCENT * effectLevel);
                aStats.getDeceleration().modifyPercent(id, ALLY_ACCEL_PERCENT * effectLevel);
                aStats.getTurnAcceleration().modifyPercent(id, ALLY_ACCEL_PERCENT * effectLevel);
                aStats.getMaxTurnRate().modifyPercent(id, ALLY_ACCEL_PERCENT * effectLevel);

                aStats.getBallisticRoFMult().modifyPercent(id, ALLY_ROF_PERCENT * effectLevel);
                aStats.getEnergyRoFMult().modifyPercent(id, ALLY_ROF_PERCENT * effectLevel);
                aStats.getFluxDissipation().modifyPercent(id, ALLY_DISSIPATION_PERCENT * effectLevel);

                if (effectLevel > 0) {
                    ally.setWeaponGlow(effectLevel, WEAPON_ACCENT, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
                    if (ally.getEngineController() != null) {
                        ally.getEngineController().extendFlame(KEY_JITTER, 1.25f * effectLevel, 1.25f * effectLevel, 1.25f * effectLevel);
                    }
                }

                // Fire subtle visual telemetry link arc
                if (triggerArcs) {
                    engine.spawnEmpArcVisual(
                            ship.getLocation(),
                            ship,
                            ally.getLocation(),
                            ally,
                            8f,
                            ARC_FRINGE,
                            ARC_CORE
                    );
                }
            }
        }

        // 6. Electronic Jamming & Disruption on the Designated Quarry
        if (quarry != null && !quarry.isHulk() && quarry.getOwner() != ship.getOwner()) {
            MutableShipStatsAPI tStats = quarry.getMutableStats();
            tStats.getWeaponTurnRateBonus().modifyMult(id, 1f - (TARGET_WEAPON_TURN_PENALTY * 0.01f * effectLevel));
            tStats.getShieldTurnRateMult().modifyMult(id, 1f - (TARGET_SHIELD_TURN_PENALTY * 0.01f * effectLevel));

            if (effectLevel > 0) {
                // Subtle tactical ring indicator on quarry
                engine.addHitParticle(
                        quarry.getLocation(),
                        quarry.getVelocity(),
                        quarry.getCollisionRadius() * 1.5f,
                        0.25f * effectLevel,
                        0.06f,
                        TARGET_RING
                );

                // Jamming spark particles
                sparkInterval.advance(engine.getElapsedInLastFrame());
                if (sparkInterval.intervalElapsed()) {
                    Vector2f sparkLoc = MathUtils.getRandomPointInCircle(quarry.getLocation(), quarry.getCollisionRadius() * 0.85f);
                    engine.addHitParticle(
                            sparkLoc,
                            quarry.getVelocity(),
                            20f + (float) Math.random() * 10f,
                            0.6f,
                            0.18f,
                            TARGET_SPARK
                    );
                }
            }
        }
    }

    protected ShipAPI findQuarry(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        // 1. Direct target lock
        ShipAPI target = ship.getShipTarget();
        if (target != null && target.isAlive() && target.getOwner() != ship.getOwner() && !target.isHulk()) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            if (dist <= TARGET_MAX_RANGE) {
                return target;
            }
        }

        // 2. Player mouse target or closest enemy to cursor
        boolean isPlayer = (ship == engine.getPlayerShip());
        if (isPlayer && ship.getMouseTarget() != null) {
            target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, TARGET_MAX_RANGE, true);
            if (target != null && target.isAlive() && !target.isHulk()) {
                return target;
            }
        }

        // 3. AI maneuver target
        if (!isPlayer && ship.getAIFlags() != null) {
            Object aiTarget = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
            if (aiTarget instanceof ShipAPI) {
                ShipAPI candidate = (ShipAPI) aiTarget;
                if (candidate.isAlive() && candidate.getOwner() != ship.getOwner() && !candidate.isHulk()) {
                    float dist = Misc.getDistance(ship.getLocation(), candidate.getLocation());
                    if (dist <= TARGET_MAX_RANGE) {
                        return candidate;
                    }
                }
            }
        }

        // 4. Closest enemy to flagship within range
        target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, TARGET_MAX_RANGE, true);
        if (target != null && target.isAlive() && !target.isHulk()) {
            return target;
        }

        return null;
    }

    private void spawnExpandingSonarWave(CombatEngineAPI engine, Vector2f center) {
        // Multi-layered subtle central flash
        engine.addHitParticle(center, new Vector2f(), 240f, 0.5f, 0.25f, GOLD_FLASH);
        engine.addSmoothParticle(center, new Vector2f(), 400f, 0.4f, 0.45f, GOLD_PULSE);

        // Concentric expanding shockwave blips (36 directional radar nodes)
        int particleCount = 36;
        float angleStep = 360f / particleCount;
        for (int i = 0; i < particleCount; i++) {
            float angle = i * angleStep;
            Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
            vel.scale(1350f);

            engine.addSmoothParticle(
                    center,
                    vel,
                    22f + (float) Math.random() * 6f,
                    0.6f,
                    1.15f,
                    GOLD_PULSE
            );
        }
    }

    private void spawnSubtlePulse(CombatEngineAPI engine, Vector2f center) {
        engine.addSmoothParticle(center, new Vector2f(), 200f, 0.3f, 0.5f, GOLD_PULSE);
        int particleCount = 16;
        float angleStep = 360f / particleCount;
        for (int i = 0; i < particleCount; i++) {
            float angle = i * angleStep;
            Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
            vel.scale(950f);
            engine.addSmoothParticle(center, vel, 14f, 0.4f, 0.7f, GOLD_PULSE);
        }
    }

    private void spawnQuarryPing(CombatEngineAPI engine, ShipAPI quarry) {
        engine.addHitParticle(quarry.getLocation(), quarry.getVelocity(), quarry.getCollisionRadius() * 2.2f, 1.2f, 0.4f, TARGET_RING);
        engine.addSmoothParticle(quarry.getLocation(), quarry.getVelocity(), quarry.getCollisionRadius() * 3.0f, 0.8f, 0.6f, TARGET_SPARK);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        waveSpawned = false;
        targetTagged = false;

        // Clean up Lead Ship
        stats.getFluxDissipation().unmodify(id);
        stats.getMaxRecoilMult().unmodify(id);
        stats.getRecoilPerShotMult().unmodify(id);
        stats.getRecoilDecayMult().unmodify(id);

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        engine.getCustomData().remove(ship.getId() + "_alpha_call_target");

        // Clean up all ships in combat
        for (ShipAPI other : engine.getShips()) {
            if (other == ship) continue;
            MutableShipStatsAPI oStats = other.getMutableStats();

            oStats.getMaxSpeed().unmodify(id);
            oStats.getAcceleration().unmodify(id);
            oStats.getDeceleration().unmodify(id);
            oStats.getTurnAcceleration().unmodify(id);
            oStats.getMaxTurnRate().unmodify(id);
            oStats.getBallisticRoFMult().unmodify(id);
            oStats.getEnergyRoFMult().unmodify(id);
            oStats.getFluxDissipation().unmodify(id);
            oStats.getWeaponTurnRateBonus().unmodify(id);
            oStats.getShieldTurnRateMult().unmodify(id);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("alpha command: +" + (int) ALLY_ROF_PERCENT + "% fleet fire rate & dissipation", false);
        } else if (index == 1) {
            return new StatusData("escorts +" + (int) ALLY_SPEED_PERCENT + "% speed & maneuvering", false);
        } else if (index == 2) {
            return new StatusData("quarry weapon tracking -" + (int) TARGET_WEAPON_TURN_PENALTY + "%", false);
        }
        return null;
    }
}
