package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import org.lazywizard.lazylib.combat.CombatUtils;

/**
 * Ridgeback Protocol (Lionhound Stalker Matrix)
 * 
 * Inspired by the Rhodesian Ridgeback:
 * - Dorsal Ridge Thermal Shunt: +40% Dissipation, -25% Weapon Flux Cost (unmatched stamina, zero overheat trap)
 * - Predator Harasser Agility: +35% Turn Rate, +20% Acceleration, +15% Top Speed (agile circling around larger prey)
 * - Hunting Pressure: +25% Ballistic & Energy Rate of Fire, +30% Weapon Turn Rate, -30% Recoil
 * - Drone Synergy: Overdrives the built-in FELIX Escort Drone (+35% speed & fire rate)
 * - Visuals: Clean weapon mount glow and subtle engine flare (no hull jitter or visual clutter)
 */
public class PackRidgebackDriveStats extends BaseShipSystemScript {

    public static final Object KEY_JITTER = new Object();

    public static final float DISSIPATION_BONUS_PERCENT = 40f;
    public static final float WEAPON_FLUX_REDUCTION = 25f;

    public static final float SPEED_BONUS_PERCENT = 15f;
    public static final float ACCEL_BONUS_PERCENT = 20f;
    public static final float TURN_BONUS_PERCENT = 35f;

    public static final float ROF_BONUS_PERCENT = 15f;
    public static final float WEAPON_TURN_BONUS = 30f;
    public static final float RECOIL_REDUCTION = 30f;

    public static final float DRONE_SPEED_BONUS = 35f;
    public static final float DRONE_ROF_BONUS = 35f;

    // Clean, authentic weapon mount glow and engine accent
    private static final Color WEAPON_ACCENT = new Color(255, 170, 45, 255);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        // 1. Dorsal Ridge Thermal Shunt (Stamina & Heat Tolerance)
        stats.getFluxDissipation().modifyPercent(id, DISSIPATION_BONUS_PERCENT * effectLevel);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));

        // 2. Harasser Mobility (Circling, perimeter control, dodging)
        stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS_PERCENT * effectLevel);
        stats.getAcceleration().modifyPercent(id, ACCEL_BONUS_PERCENT * effectLevel);
        stats.getDeceleration().modifyPercent(id, ACCEL_BONUS_PERCENT * effectLevel);
        stats.getMaxTurnRate().modifyPercent(id, TURN_BONUS_PERCENT * effectLevel);
        stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS_PERCENT * effectLevel);

        // 3. Hunting Fire Pressure (Guns & Target Tracking)
        stats.getBallisticRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getEnergyRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getWeaponTurnRateBonus().modifyPercent(id, WEAPON_TURN_BONUS * effectLevel);
        stats.getMaxRecoilMult().modifyMult(id, 1f - (RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilPerShotMult().modifyMult(id, 1f - (RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilDecayMult().modifyPercent(id, RECOIL_REDUCTION * effectLevel);

        // 4. Overdrive Built-in FELIX Escort Drones and Escort Wings
        List<ShipAPI> nearbyFighters = CombatUtils.getShipsWithinRange(ship.getLocation(), 3000f);
        for (ShipAPI drone : nearbyFighters) {
            if (drone.isFighter() && drone.getWing() != null && drone.getWing().getSourceShip() == ship) {
                if (drone.isHulk() || !drone.isAlive()) continue;
                MutableShipStatsAPI dStats = drone.getMutableStats();
                dStats.getMaxSpeed().modifyPercent(id, DRONE_SPEED_BONUS * effectLevel);
                dStats.getAcceleration().modifyPercent(id, DRONE_SPEED_BONUS * effectLevel);
                dStats.getEnergyRoFMult().modifyPercent(id, DRONE_ROF_BONUS * effectLevel);
                dStats.getBallisticRoFMult().modifyPercent(id, DRONE_ROF_BONUS * effectLevel);
                if (effectLevel > 0) {
                    drone.setWeaponGlow(effectLevel, WEAPON_ACCENT, EnumSet.allOf(WeaponType.class));
                    if (drone.getEngineController() != null) {
                        drone.getEngineController().extendFlame(KEY_JITTER, 1.25f * effectLevel, 1.25f * effectLevel, 1.25f * effectLevel);
                    }
                }
            }
        }

        // 5. Visual FX: Weapon mount glow and clean engine flare only (no hull jitter or banner clutter)
        if (effectLevel > 0) {
            ship.setWeaponGlow(effectLevel, WEAPON_ACCENT, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
            if (ship.getEngineController() != null) {
                ship.getEngineController().extendFlame(KEY_JITTER, 1.2f * effectLevel, 1.2f * effectLevel, 1.2f * effectLevel);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getFluxDissipation().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getWeaponTurnRateBonus().unmodify(id);
        stats.getMaxRecoilMult().unmodify(id);
        stats.getRecoilPerShotMult().unmodify(id);
        stats.getRecoilDecayMult().unmodify(id);

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            List<ShipAPI> fighters = CombatUtils.getShipsWithinRange(ship.getLocation(), 4000f);
            for (ShipAPI drone : fighters) {
                if (drone.isFighter() && drone.getWing() != null && drone.getWing().getSourceShip() == ship) {
                    drone.getMutableStats().getMaxSpeed().unmodify(id);
                    drone.getMutableStats().getAcceleration().unmodify(id);
                    drone.getMutableStats().getEnergyRoFMult().unmodify(id);
                    drone.getMutableStats().getBallisticRoFMult().unmodify(id);
                }
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("ridgeback protocol: +" + (int) DISSIPATION_BONUS_PERCENT + "% flux dissipation, -" + (int) WEAPON_FLUX_REDUCTION + "% weapon flux", false);
        } else if (index == 1) {
            return new StatusData("+" + (int) TURN_BONUS_PERCENT + "% turn rate, +" + (int) SPEED_BONUS_PERCENT + "% speed, +" + (int) ROF_BONUS_PERCENT + "% fire rate", false);
        } else if (index == 2) {
            return new StatusData("drone escort overcharged (+" + (int) DRONE_SPEED_BONUS + "% speed & RoF)", false);
        }
        return null;
    }
}
