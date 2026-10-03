package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;

public class JunkPiratesDamperHullMod extends BaseHullMod {

    public static final float PASSIVE_REDUCTION = 0.15f; // 15% passive reduction

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply the 10% passive reduction
        float mult = 1f - PASSIVE_REDUCTION;
        stats.getArmorDamageTakenMult().modifyMult(id, mult);
        stats.getHullDamageTakenMult().modifyMult(id, mult);
        stats.getEmpDamageTakenMult().modifyMult(id, mult);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        
        // Passive jitter effect (made brighter to be clearly visible)
        java.awt.Color jitterColor = new java.awt.Color(255, 100, 50, 100);
        ship.setJitterUnder(ship, jitterColor, 1f, 10, 4f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)(PASSIVE_REDUCTION * 100f) + "%";
        if (index == 1) return "" + (int)(data.shipsystems.scripts.JunkPiratesDamperStats.DAMAGE_MULT * 100f) + "%"; // Refers to the ship system's active state
        return null;
    }
}
