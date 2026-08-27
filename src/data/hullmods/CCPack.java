package data.hullmods;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class CCPack extends BaseHullMod{
	public static final float MANEUVER_BONUS = 10f; //PACK are slightly more manouverable and slightly harder to detect
        public static final float SENSOR_PROFILE = 75f; //percentage, as a modifyMult
		
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getDeceleration().modifyPercent(id, MANEUVER_BONUS);
		stats.getTurnAcceleration().modifyPercent(id, MANEUVER_BONUS * 2f);
		stats.getMaxTurnRate().modifyPercent(id, MANEUVER_BONUS);
                stats.getSensorProfile().modifyMult(id, (SENSOR_PROFILE * .01f));}
        
        public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) SENSOR_PROFILE + "%";

		return null;
	}
}