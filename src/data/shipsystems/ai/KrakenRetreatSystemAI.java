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

/**
 *
 * @author paul
 */
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
            FluxTrackerAPI fluxMonitor = ship.getFluxTracker(); // Allows us to set different parameters for hitting the button
            float fluxLevel = fluxMonitor.getFluxLevel();
            //float hardFluxLevel = fluxMonitor.getHardFlux() / fluxMonitor.getMaxFlux(); // Not interested for Kraken
            //we might think about skewing the decisions against hitpoints?        
            boolean shouldUseSystem = false;
            
            float missileRadius;
            float shipRadius;
            float projRadius;
            float noPressureRadius;
            float hitPoints = ship.getHitpoints() / ship.getMaxHitpoints();
            
            missileRadius = 350f;
            shipRadius = 700f;
            projRadius = 300f;
            noPressureRadius = 1200f;
            
            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, shipRadius);
            List<ShipAPI> notSoNearEnemies = AIUtils.getNearbyEnemies(ship, noPressureRadius);
//we're checking out if there are numerous enemies near us; might need to know
            List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, missileRadius);
//Need to see whether we think we should use it as a flare system
            List<DamagingProjectileAPI> nearbyBullets = CombatUtils.getProjectilesWithinRange(shipLoc, projRadius);
//Is shit going down?

            /* Filter to just enemy bullets */
            Iterator<DamagingProjectileAPI> iter = nearbyBullets.iterator();
            while (iter.hasNext()) {
                DamagingProjectileAPI nearbyBullet = iter.next();
                if ((nearbyBullet.getOwner() == 100) || (nearbyBullet.getOwner() == ship.getOwner())) {
                    iter.remove();
                }
            }
            
            // to do create a threat matrix on numbers bullets / ships / projectiles etc. to better nuance the below
            if (    fluxLevel > 0.85f && nearbyEnemies.size() > 0 && nearbyBullets.size() > 0 || // combined threat; high flux
                    fluxLevel > 0.85f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 0 || // combined threat; high flux
                    fluxLevel > 0.8f && nearbyEnemies.size() > 3 || // getting crowded; high flux
                    fluxLevel > 0.92f && nearbyBullets.size() > 0 || // threat; very high flux
                    fluxLevel > 0.92f && nearbyMissiles.size() > 0 || // threat; very high flux
                    fluxLevel > 0.8f && hitPoints <= 0.5f && notSoNearEnemies.size() > 0|| // in peril; try anything
                    fluxLevel > 0.7f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 5 || // bigger threat medium high flux
                    fluxLevel > 0.4f && nearbyEnemies.size() > 0 && nearbyMissiles.size() > 10) // heavy missile threat
            {
                shouldUseSystem = true;
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
