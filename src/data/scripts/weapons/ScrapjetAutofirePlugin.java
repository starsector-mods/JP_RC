package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

public class ScrapjetAutofirePlugin implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) return;
        
        ShipAPI ship = weapon.getShip();
        
        // If weapon is disabled or cooling down, do nothing
        if (weapon.isDisabled() || weapon.getCooldownRemaining() > 0) return;
        
        // Only force fire if the group is actually set to autofire
        boolean isAutofiring = false;
        if (ship.getWeaponGroupFor(weapon) != null) {
            isAutofiring = ship.getWeaponGroupFor(weapon).isAutofiring();
            // Don't force fire if the player has this weapon group selected (let them click to fire)
            if (ship == engine.getPlayerShip() && ship.getWeaponGroupFor(weapon) == ship.getSelectedGroupAPI() && !isAutofiring) {
                return;
            }
        }
        
        // Only trigger custom AI if autofiring OR if it's an AI ship (AI ships naturally autofire everything)
        if (!isAutofiring && ship == engine.getPlayerShip()) return;
        
        // Custom autofire logic: If a valid enemy is in range and within tracking cone, force fire!
        boolean shouldFire = false;
        float range = weapon.getRange();
        float currAngle = weapon.getCurrAngle();
        float arcFacing = weapon.getArcFacing();
        float shipFacing = ship.getFacing();
        float arc = weapon.getArc();
        
        // Guided missiles have 120 deg/s turn rate and 8s flight time.
        // Hardpoints cannot rotate, so they need wide launch authority to fire at diagonal/flank targets.
        boolean isHardpoint = weapon.getSlot() != null && weapon.getSlot().isHardpoint();
        float maxAllowedAngleDiff = isHardpoint ? 140f : Math.max((arc / 2f) + 60f, 120f);
        
        for (ShipAPI enemy : engine.getShips()) {
            if (!enemy.isAlive() || enemy.getOwner() == ship.getOwner() || enemy.isPhased()) continue;
            
            float dist = Misc.getDistance(weapon.getLocation(), enemy.getLocation());
            // Account for target ship size
            if (dist <= range + enemy.getCollisionRadius()) {
                float angleToEnemy = Misc.getAngleInDegrees(weapon.getLocation(), enemy.getLocation());
                
                boolean inMountCone = Misc.getAngleDiff(arcFacing, angleToEnemy) <= maxAllowedAngleDiff;
                boolean inCurrCone = Misc.getAngleDiff(currAngle, angleToEnemy) <= maxAllowedAngleDiff;
                boolean inShipCone = Misc.getAngleDiff(shipFacing, angleToEnemy) <= 120f;
                
                if (inMountCone || inCurrCone || inShipCone) {
                    shouldFire = true;
                    break;
                }
            }
        }
        
        if (shouldFire) {
            weapon.setForceFireOneFrame(true);
        }
    }
}
