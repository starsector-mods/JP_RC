package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.Iterator;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class KrakenRetreatSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private final IntervalUtil tracker = new IntervalUtil(0.35f, 0.6f);

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);
        Vector2f shipLoc = ship.getLocation();

        if (tracker.intervalElapsed()) {
            // Can we even use the system right now?
            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }
            FluxTrackerAPI fluxMonitor = ship.getFluxTracker();
            float fluxLevel = fluxMonitor.getFluxLevel();
            boolean shouldUseSystem = false;
            
            float missileRadius = 350f;
            float shipRadius = 700f;
            float projRadius = 300f;
            float noPressureRadius = 1200f;
            float hitPoints = ship.getHitpoints() / ship.getMaxHitpoints();
            
            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, shipRadius);
            List<ShipAPI> notSoNearEnemies = AIUtils.getNearbyEnemies(ship, noPressureRadius);
            List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, missileRadius);
            List<DamagingProjectileAPI> nearbyBullets = CombatUtils.getProjectilesWithinRange(shipLoc, projRadius);

            /* Filter to just enemy bullets */
            Iterator<DamagingProjectileAPI> iter = nearbyBullets.iterator();
            while (iter.hasNext()) {
                DamagingProjectileAPI nearbyBullet = iter.next();
                if ((nearbyBullet.getOwner() == 100) || (nearbyBullet.getOwner() == ship.getOwner())) {
                    iter.remove();
                }
            }
            
            // Threat evaluation
            if (    fluxLevel > 0.85f && nearbyEnemies.size() > 0 && nearbyBullets.size() > 0 || // combined threat; high flux
                    fluxLevel > 0.85f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 0 || // combined threat; high flux
                    fluxLevel > 0.8f && nearbyEnemies.size() > 3 || // getting crowded; high flux
                    fluxLevel > 0.92f && nearbyBullets.size() > 0 || // threat; very high flux
                    fluxLevel > 0.92f && nearbyMissiles.size() > 0 || // threat; very high flux
                    fluxLevel > 0.8f && hitPoints <= 0.5f && notSoNearEnemies.size() > 0 || // in peril; try anything
                    fluxLevel > 0.7f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 5 || // bigger threat medium high flux
                    fluxLevel > 0.4f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 10) // heavy missile threat
            {
                shouldUseSystem = true;
            }
            
            // Directional check: MERMAN applies backward thrust.
            // Ensure ship's bow is facing the threat so reverse thrust moves the ship away from danger.
            if (shouldUseSystem) {
                ShipAPI threat = target;
                if (threat == null && !nearbyEnemies.isEmpty()) {
                    threat = nearbyEnemies.get(0);
                } else if (threat == null && !notSoNearEnemies.isEmpty()) {
                    threat = notSoNearEnemies.get(0);
                }
                
                if (threat != null) {
                    float angleToThreat = VectorUtils.getAngle(shipLoc, threat.getLocation());
                    float angleDiff = Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToThreat));
                    // If threat is behind the ship (>90 degrees), reversing would push us straight into them!
                    if (angleDiff > 90f) {
                        shouldUseSystem = false;
                    }
                }
            }
                
            // If system is inactive and should be active, enable it
            // If system is active and shouldn't be, disable it
            if (ship.getSystem().isActive() ^ shouldUseSystem) {
                ship.useSystem();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }
}
