package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.EnumSet;

/**
 * Tri-Feed Overcharge
 * 
 * Works like vanilla Accelerated Ammo Feeder (AAF), extended to Ballistic, Energy, and Missile mounts:
 * - +25% Ballistic & Energy Rate of Fire
 * - -20% Ballistic & Energy Weapon Flux Cost per shot (100% Flux-Neutral)
 * - +15% Missile Rate of Fire
 * - Pure AAF-style weapon mount glow on active mounts (no hull jitter)
 */
public class PackTriOverchargeStats extends BaseShipSystemScript {

    public static final float ROF_BONUS_PERCENT = 15f;
    public static final float MISSILE_ROF_PERCENT = 15f;
    public static final float WEAPON_FLUX_REDUCTION = 15f;

    // Vanilla AAF weapon mount glow color (vibrant amber-orange)
    public static final Color WEAPON_GLOW = new Color(255, 185, 40, 255);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        // 1. AAF-Style Flux-Neutral Weapon Acceleration across all 3 disciplines
        // (1.15 RoF * 0.85 Weapon Flux = 0.97 Net Flux Rate - virtually flux neutral)
        stats.getBallisticRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getEnergyRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getMissileRoFMult().modifyPercent(id, MISSILE_ROF_PERCENT * effectLevel);

        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));

        // 2. Visual FX: Mount glow only, exactly like vanilla AAF
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (effectLevel > 0) {
                ship.setWeaponGlow(
                        effectLevel,
                        WEAPON_GLOW,
                        EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY, WeaponType.MISSILE)
                );
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("tri-overcharge: +" + (int) ROF_BONUS_PERCENT + "% weapon fire rate", false);
        } else if (index == 1) {
            return new StatusData("-" + (int) WEAPON_FLUX_REDUCTION + "% weapon flux cost (flux-neutral)", false);
        }
        return null;
    }
}
