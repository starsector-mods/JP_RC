package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class OverclockedRH extends BaseHullMod {

	private static final float SUPPLY_USE_MULT = 1.30f;
	//public static final float PEAK_BONUS_PERCENT = -10f;
	//public static final float DEGRADE_REDUCTION_PERCENT = 50f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);
		//stats.getPeakCRDuration().modifyPercent(id, PEAK_BONUS_PERCENT);
		//stats.getCRLossPerSecondPercent().modifyPercent(id, -DEGRADE_REDUCTION_PERCENT);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)((SUPPLY_USE_MULT - 1f) * 100f);
		//if (index == 1) return "" + (int) PEAK_BONUS_PERCENT + "%";
		//if (index == 2) return "" + (int) DEGRADE_REDUCTION_PERCENT + "%";
		return null;
	}
	
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && (ship.getHullSpec().getNoCRLossTime() < 10000 || ship.getHullSpec().getCRLossPerSecond() > 0); 
	}

}
