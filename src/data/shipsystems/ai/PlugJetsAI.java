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
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class PlugJetsAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private final IntervalUtil tracker = new IntervalUtil(0.35f, 0.6f);

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);
        Vector2f shipLoc = ship.getLocation();

        if (tracker.intervalElapsed()) {
            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }
            FluxTrackerAPI fluxMonitor = ship.getFluxTracker();
            float fluxLevel = fluxMonitor.getFluxLevel();
            boolean shouldUseSystem = false;
            
            float missileRadius = 350f;
            float shipRadius = 700f;
            float projRadius = 300f;
            
            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, shipRadius);
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
            
            // Engine flameout recovery under fire
            if (ship.getEngineController().isFlamedOut() && 
                    (ship.getHitpoints() < 300 || nearbyEnemies.size() > 2 || nearbyMissiles.size() > 2 || nearbyBullets.size() > 2)) {
                shouldUseSystem = true;
            }

            if (ship.getSystem().getAmmo() > 1) { 
                // With multiple charges available:
                // 1. Close distance when out of weapon range to engage targets
                // 2. Chaff defensive deployment against incoming missiles
                // 3. Fast tactical disengage if retreating
                if (!ship.areAnyEnemiesInRange() || 
                    nearbyMissiles.size() > 2 ||
                    ship.isRetreating() ||
                    (target != null && target.isRetreating())) {
                    shouldUseSystem = true;
                }
            } else { 
                // On the final charge, reserve strictly for emergencies:
                if (fluxLevel > 0.90f && nearbyEnemies.size() > 0 && nearbyBullets.size() > 0 ||
                    fluxLevel > 0.70f && nearbyEnemies.size() > 1 && nearbyMissiles.size() > 3 ||
                    fluxLevel > 0.40f && nearbyMissiles.size() > 5 ||
                    (ship.isRetreating() && fluxLevel > 0.2f)) {
                    shouldUseSystem = true;
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
