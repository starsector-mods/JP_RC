package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;

public class SyndicateAspVigilanceRestrictor extends BaseHullMod implements WeaponOPCostModifier {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.addListener(this);
    }

    @Override
    public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
        if (weapon.getSize() == WeaponSize.SMALL) {
            return 9999;
        }
        return currCost;
    }
}
