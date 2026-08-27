package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class PreMil extends BaseHullMod {

	private static final float PROFILE_INCREASE = 100f;
	//private static final float STRENGTH_DECREASE = 50f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSensorProfile().modifyPercent(id, PROFILE_INCREASE);
		//stats.getSensorStrength().modifyPercent(id, -STRENGTH_DECREASE);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) PROFILE_INCREASE;
		//if (index == 1) return "" + (int) STRENGTH_DECREASE;
		return null;
	}


}
