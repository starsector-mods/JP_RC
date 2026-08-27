package data.hullmods;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class BullseyeSuite extends BaseHullMod {
	public static final float MANEUVER_BONUS = 20f;
	private static final float CAPACITY_MULT = 1.05f;
	private static final float DISSIPATION_MULT = 1.05f;
	public static final float SMOD_PROJ_SPEED_BONUS = 15f;
	public static final float SMOD_WEAPON_TURN_BONUS = 25f;

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getDeceleration().modifyPercent(id, MANEUVER_BONUS);
		stats.getTurnAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getMaxTurnRate().modifyPercent(id, MANEUVER_BONUS);		// 5% better flux stats
		stats.getFluxCapacity().modifyMult(id, CAPACITY_MULT);
		stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT);

		if (isSMod(stats)) {
			stats.getProjectileSpeedMult().modifyPercent(id, SMOD_PROJ_SPEED_BONUS);
			stats.getWeaponTurnRateBonus().modifyPercent(id, SMOD_WEAPON_TURN_BONUS);
		}
	}

	@Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) SMOD_PROJ_SPEED_BONUS + "%";
		if (index == 1) return "" + (int) SMOD_WEAPON_TURN_BONUS + "%";
		return null;
	}

	@Override
	public boolean hasSModEffect() {
		return true;
	}
}

