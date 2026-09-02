package data.shipsystems.ai;

import java.util.List;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class JunkPiratesInterdictorAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;
    private final IntervalUtil tracker = new IntervalUtil(0.25f, 0.45f);

    public static final float MAX_RANGE = 1000f;

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

        float fluxLevel = ship.getFluxTracker().getFluxLevel();
        ShipAPI bestTarget = selectBestInterdictTarget(target);

        if (bestTarget == null) {
            return;
        }

        float dist = Misc.getDistance(ship.getLocation(), bestTarget.getLocation());
        int charges = system.getAmmo();
        boolean isDefensive = flags != null && (flags.hasFlag(AIFlags.BACKING_OFF) || flags.hasFlag(AIFlags.RUN_QUICKLY) || fluxLevel > 0.65f);
        boolean isOffensive = flags != null && (flags.hasFlag(AIFlags.PURSUING) || flags.hasFlag(AIFlags.HARASS_MOVE_IN));

        // 1. Defensive Peel: Stop pursuers dead in their tracks if ship is in danger
        if (isDefensive && dist < 1000f) {
            useSystemOnTarget(bestTarget);
            return;
        }

        // 2. High-Value Strike: Target is high flux, venting, or isolated
        if (bestTarget.getFluxTracker().isOverloadedOrVenting() || bestTarget.getFluxTracker().getFluxLevel() > 0.70f) {
            useSystemOnTarget(bestTarget);
            return;
        }

        // 3. Offensive Engagement: If we have multiple charges, interdict to facilitate approach/drone kills
        if (charges > 1) {
            if (isOffensive || dist < 1000f || bestTarget.getHullSize() == HullSize.FRIGATE || bestTarget.getHullSize() == HullSize.DESTROYER) {
                useSystemOnTarget(bestTarget);
                return;
            }
        }

        // 4. Single Charge Preservation: Save final charge for major targets or close threats
        if (charges == 1) {
            if (dist < 1000f && (bestTarget.getHullSize() == HullSize.CRUISER || bestTarget.getHullSize() == HullSize.CAPITAL_SHIP)) {
                useSystemOnTarget(bestTarget);
                return;
            }
        }
    }

    private void useSystemOnTarget(ShipAPI target) {
        if (target != null && ship.getShipTarget() != target) {
            ship.setShipTarget(target);
        }
        ship.useSystem();
    }

    private ShipAPI selectBestInterdictTarget(ShipAPI currentTarget) {
        // First check current locked target
        if (isValidTarget(currentTarget)) {
            return currentTarget;
        }

        // Scan nearby enemy ships within range
        List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, MAX_RANGE);
        ShipAPI best = null;
        float bestScore = -1f;

        for (ShipAPI enemy : enemies) {
            if (!isValidTarget(enemy)) {
                continue;
            }

            float dist = Misc.getDistance(ship.getLocation(), enemy.getLocation());
            float score = 0f;

            // High flux / vulnerable target priority (+50)
            if (enemy.getFluxTracker().isOverloadedOrVenting() || enemy.getFluxTracker().getFluxLevel() > 0.6f) {
                score += 50f;
            }

            // Larger target priority
            switch (enemy.getHullSize()) {
                case CAPITAL_SHIP:
                    score += 40f;
                    break;
                case CRUISER:
                    score += 30f;
                    break;
                case DESTROYER:
                    score += 20f;
                    break;
                case FRIGATE:
                    score += 15f;
                    break;
                default:
                    score += 5f;
                    break;
            }

            // Proximity bonus (closer targets score slightly higher)
            score += (MAX_RANGE - dist) / MAX_RANGE * 20f;

            // Facing bonus (targets in front of our bow)
            float angleToEnemy = VectorUtils.getAngle(ship.getLocation(), enemy.getLocation());
            float angleDiff = Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToEnemy));
            if (angleDiff < 45f) {
                score += 15f;
            }

            if (score > bestScore) {
                bestScore = score;
                best = enemy;
            }
        }

        return best;
    }

    private boolean isValidTarget(ShipAPI target) {
        if (target == null || !target.isAlive() || target.isHulk() || target.isShuttlePod()) {
            return false;
        }
        if (target.getOwner() == ship.getOwner()) {
            return false;
        }
        float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
        if (dist > MAX_RANGE + target.getCollisionRadius()) {
            return false;
        }
        ShipEngineControllerAPI ec = target.getEngineController();
        if (ec != null && ec.isFlamedOut()) {
            return false; // Don't waste charges on ships that are already flamed out
        }
        return true;
    }
}
