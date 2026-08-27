package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

public class PackDummyIntegratedPointDefenseAI extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL && weapon.getType() != WeaponType.MISSILE) {
                weapon.setPD(true);
            }
        }
    }
}
