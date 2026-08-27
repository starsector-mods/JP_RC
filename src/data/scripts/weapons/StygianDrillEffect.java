package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import java.awt.Color;

public class StygianDrillEffect implements EveryFrameWeaponEffectPlugin {
    
    private float fireTime = 0f;
    // It takes 3 seconds of continuous firing to reach maximum damage
    private static final float MAX_RAMP_TIME = 3f; 
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        
        if (weapon.isFiring()) {
            fireTime += amount;
            if (fireTime > MAX_RAMP_TIME) {
                fireTime = MAX_RAMP_TIME;
            }
        } else {
            // Cool down twice as fast when not firing
            fireTime -= amount * 2f; 
            if (fireTime < 0f) {
                fireTime = 0f;
            }
        }
        
        // Base damage is 1000 DPS. We want it to reach 1800 DPS.
        // So the multiplier goes from 1.0x to 1.8x.
        float intensity = fireTime / MAX_RAMP_TIME;
        float damageMult = 1f + (0.8f * intensity);
        
        // Apply the damage multiplier
        weapon.getDamage().getModifier().modifyMult("stygian_drill_ramp", damageMult);
        
        // Make the beam visually more intense as it ramps up
        if (weapon.getBeams() != null && !weapon.getBeams().isEmpty()) {
            for (BeamAPI beam : weapon.getBeams()) {
                // Increase width from 30 up to 50
                beam.setWidth(30f + (20f * intensity));
                
                // Shift core color to brighter white/yellow
                Color core = new Color(
                    255, 
                    Math.min(255, (int)(225 + 30 * intensity)), 
                    Math.min(255, (int)(215 + 40 * intensity)), 
                    255
                );
                beam.setCoreColor(core);
                
                // Shift fringe color to a brighter, angrier orange
                Color fringe = new Color(
                    Math.min(255, (int)(155 + 100 * intensity)),
                    Math.min(255, (int)(75 + 50 * intensity)),
                    Math.min(255, (int)(25 + 25 * intensity)),
                    255
                );
                beam.setFringeColor(fringe);
            }
        }
    }
}
