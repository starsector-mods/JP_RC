package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.Global;

public class JunkPiratesSpineretteBaseFix extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Set the base ship to a small hull size in the refit screen so modules take click priority
        //ship.setHullSize(HullSize.FIGHTER);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }
        // Revert to CAPITAL_SHIP in combat so AI and stats work correctly
        if (ship.getHullSize() != HullSize.CAPITAL_SHIP) {
            ship.setHullSize(HullSize.CAPITAL_SHIP);
        }
    }
}
