package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class OverclockedCB extends BaseHullMod {

	private static final float SUPPLY_USE_MULT = 1.40f;
	private static final float PEAK_MULT = 0.8f;
	//public static final float DEGRADE_REDUCTION_PERCENT = 50f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);
		stats.getPeakCRDuration().modifyMult(id, PEAK_MULT);
		//stats.getCRLossPerSecondPercent().modifyPercent(id, -DEGRADE_REDUCTION_PERCENT);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)((SUPPLY_USE_MULT - 1f) * 100f);
		if (index == 1) return "" + (int)((1f - PEAK_MULT) * 100f);
		//if (index == 2) return "" + (int) DEGRADE_REDUCTION_PERCENT + "%";
		return null;
	}
	
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && (ship.getHullSpec().getNoCRLossTime() < 10000 || ship.getHullSpec().getCRLossPerSecond() > 0); 
	}

}
