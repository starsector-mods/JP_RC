package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class JunkPiratesMasonRefractorAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;
    private final IntervalUtil tracker = new IntervalUtil(0.25f, 0.40f);

    public static final float RANGE = 900f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);
        if (!tracker.intervalElapsed()) {
            return;
        }

        if (ship == null || !ship.isAlive() || system == null || !AIUtils.canUseSystemThisFrame(ship)) {
            return;
        }

        if (system.isOutOfAmmo() || system.isActive() || system.isCoolingDown()) {
            return;
        }

        // Avoid firing if ship is already high on flux (costs 1500 flux)
        if (ship.getFluxTracker().getFluxLevel() > 0.75f) {
            return;
        }

        // Find the most dangerous active enemy within range
        ShipAPI bestTarget = null;
        float highestThreatScore = 0f;
        Vector2f shipLoc = ship.getLocation();

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy == null || !enemy.isAlive() || enemy.isFighter() || enemy.isDrone() || enemy.getOwner() == ship.getOwner()) {
                continue;
            }

            // Capitals cannot be trapped
            if (enemy.isCapital()) {
                continue;
            }

            float dist = Misc.getDistance(shipLoc, enemy.getLocation());
            if (dist > RANGE) {
                continue;
            }

            // Prioritize high-DP cruiser and destroyer brawlers
            float threat = enemy.getHullSpec().getFleetPoints();
            if (enemy.isCruiser()) threat *= 2.0f;
            else if (enemy.isDestroyer()) threat *= 1.4f;

            // Target ships that are not overloaded and have flux headroom to shoot
            if (enemy.getFluxTracker().isOverloaded()) {
                threat *= 0.2f;
            } else if (enemy.getFluxTracker().getFluxLevel() < 0.65f) {
                threat *= 1.5f;
            }

            if (threat > highestThreatScore) {
                highestThreatScore = threat;
                bestTarget = enemy;
            }
        }

        if (bestTarget != null && highestThreatScore >= 10f) {
            ship.setShipTarget(bestTarget);
            ship.useSystem();
        }
    }
}
