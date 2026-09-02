package data.hullmods;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class SyndicateAspTradeMod extends BaseHullMod{
	public static final float MANEUVER_BONUS = 10f;
	private static final float CAPACITY_MULT = 1.15f;
	private static final float DISSIPATION_MULT = 0.90f;
	private static final float SHIELD_DAMAGE_MULT = 0.90f;
		
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getDeceleration().modifyPercent(id, MANEUVER_BONUS);
		stats.getTurnAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getMaxTurnRate().modifyPercent(id, MANEUVER_BONUS);
		// Corporate Flux Doctrine: +15% capacity, -10% dissipation
		stats.getFluxCapacity().modifyMult(id, CAPACITY_MULT);
		stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT);
		stats.getShieldDamageTakenMult().modifyMult(id, SHIELD_DAMAGE_MULT);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)((CAPACITY_MULT - 1f) * 100f) + "%";
		if (index == 1) return "" + (int)((1f - SHIELD_DAMAGE_MULT) * 100f) + "%";
		if (index == 2) return "" + (int)((1f - DISSIPATION_MULT) * 100f) + "%";
		return null;
	}

}
