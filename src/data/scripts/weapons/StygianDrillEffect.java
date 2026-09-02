package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class StygianDrillEffect implements EveryFrameWeaponEffectPlugin {
    
    private float contactTime = 0f;
    // Takes 3 seconds of continuous contact with stripped bare hull to reach maximum damage
    private static final float MAX_RAMP_TIME = 3f;
    
    // Ticks for spawning sparks, debris chunks, explosions, and arcs
    private final IntervalUtil sparkInterval = new IntervalUtil(0.02f, 0.04f);
    private final IntervalUtil debrisInterval = new IntervalUtil(0.18f, 0.30f);
    private final IntervalUtil explosionInterval = new IntervalUtil(0.10f, 0.16f);
    private final IntervalUtil arcInterval = new IntervalUtil(0.20f, 0.35f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        
        // Check if any active beam is making contact with stripped/exposed bare hull
        boolean isMakingContact = false;
        if (weapon.isFiring() && weapon.getBeams() != null && !weapon.getBeams().isEmpty()) {
            for (BeamAPI beam : weapon.getBeams()) {
                if (isContactingBareHull(beam)) {
                    isMakingContact = true;
                    break;
                }
            }
        }
        
        // Ramp up only when actively making contact with stripped bare hull
        if (isMakingContact) {
            contactTime += amount;
            if (contactTime > MAX_RAMP_TIME) {
                contactTime = MAX_RAMP_TIME;
            }
        } else {
            // Cool down when not contacting stripped bare hull
            contactTime -= amount * 2f; 
            if (contactTime < 0f) {
                contactTime = 0f;
            }
        }
        
        // Base damage is 1000 DPS. Scales up to 1800 DPS (1.0x -> 1.8x) upon sustained contact.
        float intensity = contactTime / MAX_RAMP_TIME;
        float damageMult = 1f + (0.8f * intensity);
        
        // Apply the damage multiplier
        weapon.getDamage().getModifier().modifyMult("stygian_drill_ramp", damageMult);
        
        // Advance VFX intervals
        sparkInterval.advance(amount);
        debrisInterval.advance(amount);
        explosionInterval.advance(amount);
        arcInterval.advance(amount);
        
        // Render beam body, core & impact tip effects
        if (weapon.getBeams() != null && !weapon.getBeams().isEmpty()) {
            for (BeamAPI beam : weapon.getBeams()) {
                // Focus beam from wide heavy plume (42 width) down to concentrated cutter (24 width) ONLY upon contact
                beam.setWidth(42f - (18f * intensity));
                
                // Beam Core: Starts warm incandescent red-orange (255, 180, 140), superheats to glowing fiery amber-white (255, 220, 160)
                Color core = new Color(
                    255, 
                    (int)(180 + 40 * intensity), 
                    (int)(140 + 20 * intensity), 
                    255
                );
                beam.setCoreColor(core);
                
                // Beam Fringe: Progresses from deep fiery red-orange (240, 50, 20) -> superheated amber-tinted red-orange (255, 105, 25)
                Color fringe = new Color(
                    (int)(240 + 15 * intensity),
                    (int)(50 + 55 * intensity),
                    (int)(20 + 5 * intensity),
                    (int)(220 + 35 * intensity)
                );
                beam.setFringeColor(fringe);
                
                // --- Beam Tip: Sparks, Debris & Impact VFX at Hit Location ---
                if (beam.getDamageTarget() != null) {
                    Vector2f hitLoc = beam.getTo();
                    Vector2f beamSource = beam.getFrom();
                    
                    // Calculate bounce angle (deflecting backwards and radially away from target surface)
                    float beamAngle = VectorUtils.getAngle(beamSource, hitLoc);
                    float reverseAngle = beamAngle + 180f;
                    
                    // 1. Shower of molten sparks / cutting slag flying off impact point
                    if (sparkInterval.intervalElapsed()) {
                        int sparkCount = 3 + (int)(5 * intensity);
                        for (int i = 0; i < sparkCount; i++) {
                            float sprayAngle = reverseAngle + ((float) Math.random() - 0.5f) * 140f;
                            float sparkSpeed = 80f + (float) Math.random() * (160f + 120f * intensity);
                            Vector2f sparkVel = MathUtils.getPointOnCircumference(new Vector2f(), sparkSpeed, sprayAngle);
                            
                            // High-speed cutting flecks vs glowing molten embers
                            boolean isHotFleck = Math.random() > 0.35;
                            if (isHotFleck) {
                                float size = 2.5f + (float) Math.random() * 3.5f * (1f + intensity);
                                float duration = 0.12f + (float) Math.random() * 0.25f;
                                Color hotCol = new Color(
                                    255,
                                    (int)(80 + 50 * intensity + (Math.random() * 20)),
                                    25,
                                    245
                                );
                                engine.addHitParticle(hitLoc, sparkVel, size, 1f, duration, hotCol);
                            } else {
                                float size = 4f + (float) Math.random() * 5f * (1f + intensity);
                                float duration = 0.25f + (float) Math.random() * 0.4f;
                                Color emberCol = new Color(
                                    (int)(240 + 15 * intensity),
                                    (int)(40 + 30 * intensity),
                                    15,
                                    210
                                );
                                engine.addSmoothParticle(hitLoc, sparkVel, size, 0.9f, duration, emberCol);
                            }
                        }
                        
                        // Radiant soft ambient halo at impact point
                        Color tipGlow = new Color(
                            (int)(245 + 10 * intensity),
                            (int)(45 + 40 * intensity),
                            20,
                            (int)(55 + 45 * intensity)
                        );
                        engine.addSmoothParticle(
                            hitLoc, new Vector2f(),
                            35f + 45f * intensity, 0.6f + 0.4f * intensity,
                            0.10f, tipGlow
                        );
                    }
                    
                    // 2. Physical Hull Plating & Armor Debris Peeling Off
                    if (debrisInterval.intervalElapsed()) {
                        ShipAPI targetShip = (beam.getDamageTarget() instanceof ShipAPI) ? (ShipAPI) beam.getDamageTarget() : null;
                        boolean hitsShields = targetShip != null && targetShip.getShield() != null && targetShip.getShield().isOn() && targetShip.getShield().isWithinArc(hitLoc);
                        
                        // Spawn physical chunks of metallic hull plating when tearing into armor or unshielded hull
                        if (!hitsShields) {
                            Vector2f targetVel = (targetShip != null) ? targetShip.getVelocity() : new Vector2f();
                            int numDebris = 1 + (int)(2 * intensity);
                            float minSpeed = 40f + 30f * intensity;
                            float maxSpeed = 100f + 80f * intensity;
                            float duration = 5f + 5f * intensity;
                            
                            engine.spawnDebrisSmall(hitLoc, targetVel, numDebris, reverseAngle, 120f, minSpeed, maxSpeed, duration);
                            
                            // At higher power, rip off larger structural plate pieces
                            if (intensity > 0.5f && Math.random() > 0.4) {
                                engine.spawnDebrisMedium(hitLoc, targetVel, 1, reverseAngle, 80f, minSpeed * 0.7f, maxSpeed * 0.7f, duration);
                            }
                        }
                    }
                    
                    // 3. Subtle impact pulse for camera rumble
                    if (explosionInterval.intervalElapsed()) {
                        Color expCol = new Color(
                            (int)(235 + 20 * intensity),
                            (int)(40 + 35 * intensity),
                            20,
                            110
                        );
                        engine.spawnExplosion(
                            hitLoc, new Vector2f(),
                            expCol,
                            25f + 25f * intensity,
                            0.12f
                        );
                    }
                    
                    // 4. Electrical discharge arcs at >85% intensity
                    if (intensity > 0.85f && beam.getDamageTarget() instanceof ShipAPI && arcInterval.intervalElapsed()) {
                        Color arcFringe = new Color(255, 90, 25, 210);
                        Color arcCore = new Color(255, 220, 170, 245);
                        engine.spawnEmpArc(
                            beam.getSource(), hitLoc,
                            beam.getDamageTarget(), beam.getDamageTarget(),
                            DamageType.ENERGY,
                            0f, // purely visual
                            0f, 
                            350f,
                            "tachyon_lance_emp_impact",
                            3.5f + 2f * intensity,
                            arcFringe,
                            arcCore
                        );
                    }
                }
            }
        }
        
        // Dynamic barrel glow: Red-orange to warm fiery glow
        if (weapon.getGlowSpriteAPI() != null) {
            weapon.getGlowSpriteAPI().setColor(new Color(
                255,
                (int)(35 + 55 * intensity),
                15,
                Math.min(255, (int)(110 + 145 * intensity))
            ));
        }
        
        // Combat UI Status text
        if (weapon.getChargeLevel() > 0f) {
            if (weapon.getShip() == Global.getCombatEngine().getPlayerShip()) {
                float baseDps = weapon.getDerivedStats().getDps();
                float currentDps = baseDps * damageMult;
                String status;
                if (isMakingContact) {
                    int bonusPct = (int)((damageMult - 1f) * 100f);
                    status = String.format("drilling bare hull: %d DPS (+%d%%)", (int) currentDps, bonusPct);
                } else if (weapon.isFiring() && weapon.getBeams() != null && !weapon.getBeams().isEmpty()) {
                    boolean hitShip = false;
                    for (BeamAPI beam : weapon.getBeams()) {
                        if (beam.getDamageTarget() instanceof ShipAPI) {
                            hitShip = true;
                            break;
                        }
                    }
                    status = hitShip ? String.format("contact: armor/shield (%d DPS)", (int) baseDps) : String.format("active (%d DPS)", (int) baseDps);
                } else {
                    status = String.format("ready (%d DPS)", (int) baseDps);
                }
                
                Global.getCombatEngine().maintainStatusForPlayerShip(
                    weapon,
                    weapon.getSpec().getTurretSpriteName(),
                    "Stygian Drill",
                    status,
                    false
                );
            }
        }
    }
    
    /**
     * Checks if the beam is actively making contact with stripped/exposed bare hull
     * (target is a ship, hit is not blocked by active shields, and armor at the hit cell is depleted).
     */
    private boolean isContactingBareHull(BeamAPI beam) {
        if (beam == null || !(beam.getDamageTarget() instanceof ShipAPI)) {
            return false;
        }
        
        ShipAPI target = (ShipAPI) beam.getDamageTarget();
        if (target.isPhased()) {
            return false;
        }
        
        Vector2f hitLoc = beam.getTo();
        if (hitLoc == null) {
            return false;
        }
        
        // Blocked by active shields
        if (target.getShield() != null && target.getShield().isOn() && target.getShield().isWithinArc(hitLoc)) {
            return false;
        }
        
        ArmorGridAPI armorGrid = target.getArmorGrid();
        if (armorGrid == null || armorGrid.getArmorRating() <= 0f) {
            return true;
        }
        
        int[] cell = armorGrid.getCellAtLocation(hitLoc);
        if (cell != null && cell.length >= 2) {
            int cellX = cell[0];
            int cellY = cell[1];
            float[][] grid = armorGrid.getGrid();
            if (grid != null && cellX >= 0 && cellX < grid.length && cellY >= 0 && cellY < grid[0].length) {
                float armorVal = armorGrid.getArmorValue(cellX, cellY);
                // Armor stripped at cell -> contacting bare hull
                return armorVal <= 1f || armorGrid.getArmorFraction(cellX, cellY) <= 0.01f;
            }
        }
        
        return false;
    }
}
