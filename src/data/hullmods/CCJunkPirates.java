package data.hullmods;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class CCJunkPirates extends BaseHullMod{
	public static final float PEAK_BONUS_PERCENT = 10f; // Junk Pirates are better at keeping their craft flying. Bonus to runtime and reduced degradation.
	public static final float DEGRADE_REDUCTION_PERCENT = 10f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getPeakCRDuration().modifyPercent(id, PEAK_BONUS_PERCENT);
		stats.getCRLossPerSecondPercent().modifyMult(id, 1f - DEGRADE_REDUCTION_PERCENT / 100f);
	}
        
        public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) PEAK_BONUS_PERCENT + "%";
		if (index == 1) return "" + (int) DEGRADE_REDUCTION_PERCENT + "%";
		return null;
	}
}