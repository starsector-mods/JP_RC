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
 * Triforce Overdrive (Tri-Feed Overdrive / Ridgeback Protocol unified)
 * 
 * Unifies the tripartite weapon feed overdrive with dorsal thermal shunts and agility:
 * - 1. Tripartite Firepower: +20% Ballistic, Energy, and Missile Rate of Fire
 * - 2. Heat & Weapon Flux Reduction: -25% Ballistic & Energy Weapon Flux Cost per shot
 * - 3. Thermal Shunt: +40% Flux Dissipation
 * - 4. Harasser Agility: +15% Speed, +20% Accel/Decel, +35% Turn Rate & Turn Accel
 * - 5. Target Tracking & Handling: +30% Weapon Turn Rate, -30% Recoil
 * - 6. Escort Drone Overdrive: Overdrives built-in escort drones/wings (e.g. FELIX) with +35% speed & RoF
 * - 7. Visuals: Mount glow across all 3 weapon disciplines and engine flare
 */
public class PackTriOverchargeStats extends BaseShipSystemScript {

    public static final Object KEY_JITTER = new Object();

    // 1. Tripartite Firepower
    public static final float ROF_BONUS_PERCENT = 20f;
    public static final float MISSILE_ROF_PERCENT = 20f;
    public static final float WEAPON_FLUX_REDUCTION = 25f;

    // 2. Thermal Shunt
    public static final float DISSIPATION_BONUS_PERCENT = 40f;

    // 3. Mobility & Agility
    public static final float SPEED_BONUS_PERCENT = 15f;
    public static final float ACCEL_BONUS_PERCENT = 20f;
    public static final float TURN_BONUS_PERCENT = 35f;

    // 4. Weapon Handling
    public static final float WEAPON_TURN_BONUS = 30f;
    public static final float RECOIL_REDUCTION = 30f;

    // 5. Escort Drone Overdrive
    public static final float DRONE_SPEED_BONUS = 35f;
    public static final float DRONE_ROF_BONUS = 35f;

    // Amber-orange weapon glow
    public static final Color WEAPON_GLOW = new Color(255, 185, 40, 255);

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

        // 1. Tripartite Weapon Acceleration across Ballistic, Energy, and Missile
        stats.getBallisticRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getEnergyRoFMult().modifyPercent(id, ROF_BONUS_PERCENT * effectLevel);
        stats.getMissileRoFMult().modifyPercent(id, MISSILE_ROF_PERCENT * effectLevel);

        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - (WEAPON_FLUX_REDUCTION * 0.01f * effectLevel));

        // 2. Weapon Handling & Recoil
        stats.getWeaponTurnRateBonus().modifyPercent(id, WEAPON_TURN_BONUS * effectLevel);
        stats.getMaxRecoilMult().modifyMult(id, 1f - (RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilPerShotMult().modifyMult(id, 1f - (RECOIL_REDUCTION * 0.01f * effectLevel));
        stats.getRecoilDecayMult().modifyPercent(id, RECOIL_REDUCTION * effectLevel);

        // 3. Thermal Shunt
        stats.getFluxDissipation().modifyPercent(id, DISSIPATION_BONUS_PERCENT * effectLevel);

        // 4. Harasser Agility
        stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS_PERCENT * effectLevel);
        stats.getAcceleration().modifyPercent(id, ACCEL_BONUS_PERCENT * effectLevel);
        stats.getDeceleration().modifyPercent(id, ACCEL_BONUS_PERCENT * effectLevel);
        stats.getMaxTurnRate().modifyPercent(id, TURN_BONUS_PERCENT * effectLevel);
        stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS_PERCENT * effectLevel);

        // 5. Escort Drone / Wing Overdrive
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
                    drone.setWeaponGlow(effectLevel, WEAPON_GLOW, EnumSet.allOf(WeaponType.class));
                    if (drone.getEngineController() != null) {
                        drone.getEngineController().extendFlame(KEY_JITTER, 1.25f * effectLevel, 1.25f * effectLevel, 1.25f * effectLevel);
                    }
                }
            }
        }

        // 6. Visual FX: Weapon mount glow across all three mount types and engine flare
        if (effectLevel > 0) {
            ship.setWeaponGlow(
                    effectLevel,
                    WEAPON_GLOW,
                    EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY, WeaponType.MISSILE)
            );
            if (ship.getEngineController() != null) {
                ship.getEngineController().extendFlame(KEY_JITTER, 1.2f * effectLevel, 1.2f * effectLevel, 1.2f * effectLevel);
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

        stats.getWeaponTurnRateBonus().unmodify(id);
        stats.getMaxRecoilMult().unmodify(id);
        stats.getRecoilPerShotMult().unmodify(id);
        stats.getRecoilDecayMult().unmodify(id);

        stats.getFluxDissipation().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

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
        if (state == State.IDLE || effectLevel <= 0f) return null;
        if (index == 0) {
            return new StatusData("triforce overdrive: +" + (int) ROF_BONUS_PERCENT + "% fire rate, +" + (int) DISSIPATION_BONUS_PERCENT + "% dissipation", false);
        } else if (index == 1) {
            return new StatusData("-" + (int) WEAPON_FLUX_REDUCTION + "% weapon flux, +" + (int) SPEED_BONUS_PERCENT + "% speed & agility", false);
        } else if (index == 2) {
            return new StatusData("improved weapon tracking & recoil dampening", false);
        }
        return null;
    }
}
