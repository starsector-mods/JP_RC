package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RidgebackDriveAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private final IntervalUtil tracker = new IntervalUtil(0.20f, 0.35f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);
        if (!tracker.intervalElapsed()) {
            return;
        }

        if (ship == null || ship.isHulk() || !ship.isAlive()) {
            return;
        }

        ShipSystemAPI system = ship.getSystem();
        if (system == null || system.isActive() || system.isCoolingDown()) {
            return;
        }

        FluxTrackerAPI flux = ship.getFluxTracker();
        float fluxLevel = flux.getFluxLevel();

        // Do not activate if about to overload (save for venting or emergency overload recovery)
        if (fluxLevel > 0.90f) {
            return;
        }

        boolean shouldUse = false;

        // 1. Check engagement status and enemy proximity
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 1200f);
        boolean inActiveFirefight = !nearbyEnemies.isEmpty() || ship.areAnyEnemiesInRange();

        // 2. Heat / Stamina Purge: If building flux (25% to 85%) in active combat, activate to purge heat via +40% dissipation
        if (fluxLevel >= 0.25f && inActiveFirefight) {
            shouldUse = true;
        }

        // 3. Lionhound Stalker: Harassing larger prey (Cruisers or Capitals)
        if (target != null && target.isAlive() && !target.isHulk()) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            boolean isLargePrey = (target.getHullSize() == HullSize.CRUISER || target.getHullSize() == HullSize.CAPITAL_SHIP);
            if (isLargePrey && dist <= 1100f) {
                shouldUse = true;
            }
        }

        // 4. Native Combat AI Flags (Pursuit, Harass Move In, Maneuvering)
        if (flags != null) {
            if (flags.hasFlag(AIFlags.PURSUING) || flags.hasFlag(AIFlags.HARASS_MOVE_IN)) {
                if (inActiveFirefight) {
                    shouldUse = true;
                }
            }
        }

        // 5. Evasive repositioning against incoming missile volleys or torpedoes
        List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, 600f);
        if (nearbyMissiles.size() >= 2 && fluxLevel <= 0.85f) {
            shouldUse = true;
        }

        // 6. Tactical retreat: boost dissipation and speed to disengage
        if (ship.isRetreating() && inActiveFirefight) {
            shouldUse = true;
        }

        if (shouldUse) {
            ship.useSystem();
        }
    }
}
