package data.shipsystems.scripts;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

public class JunkPiratesMasonRefractorStats extends BaseShipSystemScript {

    public static final float RANGE = 900f;
    public static final float DURATION = 3.5f;
    public static final Color SHIELD_COLOR = new Color(80, 210, 255, 195);
    public static final Color SHIELD_INNER_JITTER = new Color(40, 160, 255, 160);
    public static final Color SHIELD_OUTER_JITTER = new Color(0, 100, 255, 120);

    public static class TargetData {
        public ShipAPI target;
        public boolean triggered = false;
        public TargetData(ShipAPI target) {
            this.target = target;
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        final ShipAPI sourceShip = (ShipAPI) stats.getEntity();

        final String targetDataKey = sourceShip.getId() + "_mason_refractor_target";
        TargetData targetData = (TargetData) Global.getCombatEngine().getCustomData().get(targetDataKey);

        if (state == State.IN && targetData == null) {
            ShipAPI target = findTarget(sourceShip);
            targetData = new TargetData(target);
            Global.getCombatEngine().getCustomData().put(targetDataKey, targetData);
        } else if (state == State.IDLE && targetData != null) {
            Global.getCombatEngine().getCustomData().remove(targetDataKey);
        }

        if (targetData == null || targetData.target == null) {
            return;
        }

        final ShipAPI mainTarget = targetData.target;



        // Trigger the 360-degree shield prison when fully charged
        if (effectLevel >= 1f && !targetData.triggered) {
            targetData.triggered = true;

            // 1. Spawn a dummy drone to project the shield bubble, and disable their native defenses
            final ShieldAPI targetShield = mainTarget.getShield();
            if (targetShield != null && targetShield.isOn()) {
                targetShield.toggleOff();
            }
            if (mainTarget.getPhaseCloak() != null && mainTarget.getPhaseCloak().isActive()) {
                mainTarget.getPhaseCloak().forceState(SystemState.IDLE, 0f);
            }

            final ShipAPI dummy = Global.getCombatEngine().getFleetManager(sourceShip.getOwner()).spawnShipOrWing(
                "junk_pirates_mason_dummy", mainTarget.getLocation(), 0f
            );
            
            if (dummy == null) {
                Global.getCombatEngine().addFloatingText(mainTarget.getLocation(), "DUMMY SPAWN FAILED", 30f, Color.RED, mainTarget, 1f, 3f);
            }

            if (dummy != null) {
                dummy.setCollisionClass(CollisionClass.NONE);
                if (dummy.getShipAI() != null) {
                    dummy.setShipAI(null); // Prevent AI from toggling shield off
                }
                dummy.getMutableStats().getHullDamageTakenMult().modifyMult("dummy_invuln", 0f);
                dummy.getMutableStats().getArmorDamageTakenMult().modifyMult("dummy_invuln", 0f);
                dummy.getMutableStats().getShieldUnfoldRateMult().modifyMult("dummy_fast_unfold", 10000f);
                if (dummy.getShield() != null) {
                    dummy.getShield().setRadius(mainTarget.getCollisionRadius() + 100f);
                    dummy.getShield().setInnerColor(SHIELD_COLOR);
                    dummy.getShield().setRingColor(SHIELD_OUTER_JITTER);
                    dummy.getShield().setType(ShieldType.OMNI);
                    dummy.getShield().setArc(360f);
                    dummy.getShield().toggleOn();
                }
            }

            // Floaty text notification on the trapped ship
            Color floatyColor = Misc.setAlpha(SHIELD_COLOR, 255);
            mainTarget.getFluxTracker().showOverloadFloatyIfNeeded("Shield Prison Engaged", floatyColor, 4f, true);

            // 2. Damage mitigation listener (impervious outer shell isolates target from all external fire)
            final DamageTakenModifier prisonDamageListener = new DamageTakenModifier() {
                @Override
                public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                    if (target == mainTarget) {
                        ShipAPI damageSource = null;
                        if (param instanceof DamagingProjectileAPI) {
                            damageSource = ((DamagingProjectileAPI) param).getSource();
                        } else if (param instanceof BeamAPI) {
                            damageSource = ((BeamAPI) param).getSource();
                        }

                        // Outside fire is absorbed by the prison's outer shell
                        if (damageSource != null && damageSource != mainTarget) {
                            damage.getModifier().modifyMult("jp_mason_prison_shell", 0f);
                            return "jp_mason_prison_shell";
                        }
                    }
                    return null;
                }
            };
            mainTarget.addListener(prisonDamageListener);

            // 3. Optical compression pinch distortion using GraphicsLib
            final WaveDistortion wave;
            if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
                wave = new WaveDistortion(mainTarget.getLocation(), mainTarget.getVelocity());
                wave.setSize((mainTarget.getCollisionRadius() + 100f) * 1.4f);
                wave.setIntensity(100f);
                wave.flip(true); // Inward optical pinch / compression
                wave.setLifetime(DURATION);
                wave.fadeInIntensity(0.4f);
                wave.setAutoFadeIntensityTime(0.8f);
                DistortionShader.addDistortion(wave);
            } else {
                wave = null;
            }

            // 4. Register Combat Plugin for the 360-degree containment & internal deflection loop
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private float elapsed = 0f;
                private final Set<DamagingProjectileAPI> reflectedSet = new HashSet<>();
                private final Set<DamagingProjectileAPI> blockedOuterSet = new HashSet<>();

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    elapsed += amount;

                    // Expiration & Cleanup
                    if (elapsed >= DURATION || mainTarget == null || !mainTarget.isAlive()) {
                        if (dummy != null) {
                            Global.getCombatEngine().removeEntity(dummy);
                        }
                        if (wave != null) {
                            wave.fadeOutIntensity(0.2f);
                        }
                        if (mainTarget != null) {
                            mainTarget.removeListener(prisonDamageListener);
                            Color color = Misc.setAlpha(SHIELD_COLOR, 255);
                            mainTarget.getFluxTracker().showOverloadFloatyIfNeeded("Shield Prison Collapsed", color, 4f, true);
                        }
                        Global.getCombatEngine().removePlugin(this);
                        return;
                    }

                    // Keep target's native defenses disabled
                    if (targetShield != null && targetShield.isOn()) {
                        targetShield.toggleOff();
                    }
                    if (mainTarget.getPhaseCloak() != null && mainTarget.getPhaseCloak().isActive()) {
                        mainTarget.getPhaseCloak().forceState(SystemState.IDLE, 0f);
                    }

                    // Keep distortion wave centered on the target ship as it maneuvers
                    if (wave != null && mainTarget != null && mainTarget.isAlive()) {
                        wave.setLocation(mainTarget.getLocation());
                        wave.setVelocity(mainTarget.getVelocity());
                    }

                    // Smooth one-way shrinking shield radius (starts large, shrinks tightly to hull, never expands outwards)
                    float startRadius = mainTarget.getCollisionRadius() + 100f;
                    float endRadius = mainTarget.getCollisionRadius() + 15f;
                    float currentRadius = startRadius - ((startRadius - endRadius) * (elapsed / DURATION));
                    if (currentRadius < endRadius) {
                        currentRadius = endRadius;
                    }
                    Vector2f targetLoc = mainTarget.getLocation();

                    // Keep dummy attached and visually shrinking
                    if (dummy != null) {
                        dummy.getLocation().set(targetLoc);
                        dummy.getVelocity().set(mainTarget.getVelocity());
                        if (dummy.getShield() != null) {
                            if (!dummy.getShield().isOn()) {
                                dummy.getShield().toggleOn();
                            }
                            dummy.getShield().setRadius(currentRadius);
                        }
                        // Dummy takes no flux damage
                        dummy.getFluxTracker().setCurrFlux(0f);
                        dummy.getFluxTracker().setHardFlux(0f);
                    }

                    // Continuous energy crushing damage as the bubble shrinks (~525 total energy damage over 3.5s)
                    float damage = 150f * amount;
                    Global.getCombatEngine().applyDamage(mainTarget, targetLoc, damage, DamageType.ENERGY, 0f, false, false, sourceShip);

                    // Moderate flux compression (12% max flux per second, ~42% hard flux over 3.5s)
                    float flatFlux = mainTarget.getMaxFlux() * 0.12f * amount;
                    mainTarget.getFluxTracker().increaseFlux(flatFlux, true);
                    if (mainTarget.getFluxTracker().getCurrFlux() >= mainTarget.getMaxFlux() && !mainTarget.getFluxTracker().isOverloaded()) {
                        mainTarget.getFluxTracker().forceOverload(2.0f);
                    }

                    // Intermittent containment discharges and hull fragments
                    if (Math.random() < 3f * amount) { // Approx 3 times per second
                        float angle = (float) Math.random() * 360f;
                        float dist = (float) Math.random() * mainTarget.getCollisionRadius();
                        Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                        loc.scale(dist);
                        Vector2f.add(targetLoc, loc, loc);

                        // Fire explosion
                        Global.getCombatEngine().spawnExplosion(loc, mainTarget.getVelocity(), Color.ORANGE, 40f + 60f * (float)Math.random(), 0.6f + 0.6f * (float)Math.random());
                        if (Global.getSoundPlayer() != null) {
                            Global.getSoundPlayer().playSound("explosion_ship", 1f + (float)Math.random()*0.2f, 0.4f, loc, mainTarget.getVelocity());
                        }

                        // Eject hull fragments and sparks
                        int numFrags = (int)(Math.random() * 3) + 2;
                        for (int i = 0; i < numFrags; i++) {
                            float fragAngle = angle + (float)Math.random() * 90f - 45f;
                            Vector2f fragVel = Misc.getUnitVectorAtDegreeAngle(fragAngle);
                            fragVel.scale(80f + (float)Math.random() * 150f);
                            Vector2f.add(fragVel, mainTarget.getVelocity(), fragVel);
                            
                            // Dark chunky hull fragments
                            Global.getCombatEngine().addHitParticle(loc, fragVel, 6f + (float)Math.random() * 8f, 1f, 1f + (float)Math.random()*1.5f, Color.DARK_GRAY);
                            // Bright sparks
                            Global.getCombatEngine().addHitParticle(loc, fragVel, 3f + (float)Math.random() * 4f, 1f, 0.5f + (float)Math.random()*0.5f, Color.ORANGE);
                        }
                    }

                    // Suppress weapons on the trapped ship during containment
                    for (WeaponAPI w : mainTarget.getAllWeapons()) {
                        w.setRemainingCooldownTo(0.8f);
                    }

                    // A. Block Outside Projectiles
                    for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
                        if (proj == null) continue;
                        float dist = Misc.getDistance(proj.getLocation(), targetLoc);

                        // EXTERNAL SHIELDING: Outside hostile or friendly fire hitting the outer prison shell
                        if (proj.getSource() != mainTarget && !blockedOuterSet.contains(proj)) {
                            if (dist <= currentRadius + 20f && dist >= currentRadius - 20f) {
                                Vector2f vel = proj.getVelocity();
                                Vector2f toCenter = Vector2f.sub(targetLoc, proj.getLocation(), new Vector2f());
                                if (Vector2f.dot(vel, toCenter) > 0f) {
                                    blockedOuterSet.add(proj);
                                    Global.getCombatEngine().addHitParticle(proj.getLocation(), new Vector2f(), 40f, 1f, 0.12f, SHIELD_COLOR);
                                    Global.getCombatEngine().removeEntity(proj);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    protected ShipAPI findTarget(final ShipAPI ship) {
        FindShipFilter filter = new FindShipFilter() {
            public boolean matches(ShipAPI targetShip) {
                if (targetShip == null || !targetShip.isAlive() || targetShip.isFighter() || targetShip.getOwner() == ship.getOwner()) return false;
                if (targetShip.getHullSize() == HullSize.CAPITAL_SHIP) return false; // Capital ships are too large to trap
                return true;
            }
        };

        float range = RANGE;
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            if (dist > range || !filter.matches(target)) {
                target = null;
            }
        }

        if (target == null) {
            if (player) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.CRUISER, range, true, filter);
            } else {
                target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.CRUISER, range, true, filter);
            }
        }
        return target;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.IDLE || effectLevel <= 0f) return null;
        if (index == 0) {
            return new StatusData("mason refractor: target trapped in 360° containment prison", false);
        } else if (index == 1) {
            return new StatusData("target isolated and compressed", false);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship);
        if (target != null && target != ship) {
            return "READY";
        }
        if (target == null && ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (system.isActive()) return true;
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }
}
