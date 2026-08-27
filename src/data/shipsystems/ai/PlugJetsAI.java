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
public class PlugJetsAI implements ShipSystemAIScript {

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
            FluxTrackerAPI fluxMonitor = ship.getFluxTracker(); // mainly an issue for the tangerine; but other stuff might need to know
            float fluxLevel = fluxMonitor.getFluxLevel();
            
            // if ("junk_pirates_tangerine".equals(ship.getHullSpec().getHullId())) { // if we are installed on the tangerine, there are a couple of extra things we need to be aware of
            
            boolean shouldUseSystem = false;
            
            float missileRadius;
            float shipRadius;
            float projRadius;
            
            missileRadius = 350f;
            shipRadius = 700f;
            projRadius = 300f;
            
            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, shipRadius);
//we're checking out if there are numerous enemies near us; might need to know
            List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, missileRadius);
//Need to see whether we think we should use the flare system
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
            
            if (ship.getEngineController().isFlamedOut() && 
                    (ship.getHitpoints() < 300 || nearbyEnemies.size() > 2 || nearbyMissiles.size() > 2 || nearbyBullets.size() > 2)) {
                shouldUseSystem = true;
            }

            if ("junk_pirates_tangerine".equals(ship.getHullSpec().getHullId())) { //  the tangerine is a different kettle of fish
                if (ship.getSystem().getAmmo() > 1) { // we want to consider how we behave when we have 2 charges vs 1
                    // Tangerine wants to go like fuck; needs a reckless officer. But it wants to hold off a charge for when it overloads / flamesout.
                    if ( !ship.areAnyEnemiesInRange() || nearbyMissiles.size() > 2 ||
                            fluxLevel > 0.95f && nearbyEnemies.size() > 0 && nearbyBullets.size() > 0 || //dump electrochaff for shits and giggles
                            ship.isRetreating() || // if we have 2 charges and are retreating, lets go
                            ship.areAnyEnemiesInRange() && fluxLevel < 0.2f) { // if we are fucking about; let's drop it
                        shouldUseSystem = true;
                    }
                } else { // we have 1 or less charges
                    if (fluxLevel > 0.95f && nearbyEnemies.size() > 0 && nearbyBullets.size() > 0 || // threat
                            fluxLevel > 0.7f && nearbyEnemies.size() > 1 && nearbyMissiles.size() > 3 || // bigger threat
                            fluxLevel > 0.4f && nearbyMissiles.size() > 5 || // heavy missile threat
                            ship.isRetreating() && fluxLevel > 0.2f) // we want to get the fuck out of dodge and we are under pressure
                    {
                        shouldUseSystem = true;
                    }
                }
            } else { // ship is not the tangerine
                if (ship.getSystem().getAmmo() > 1) { // we want to consider how we behave when we have 2 charges vs 1
                    if ( !ship.areAnyEnemiesInRange() || nearbyMissiles.size() > 2 ||
                            ship.isRetreating() || // if we have 2 charges and are retreating, lets go
                            ship.areAnyEnemiesInRange() && fluxLevel < 0.2f) { // if we are fucking about; let's drop it
                        shouldUseSystem = true;
                    }
                } else { // we have 1 or less charges
                    if (nearbyMissiles.size() > 5 ||
                            ship.isRetreating() && fluxLevel > 0.2f ) // we want to get the fuck out of dodge and we are under pressure) // heavy missile threat
                    {
                        shouldUseSystem = true;
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
