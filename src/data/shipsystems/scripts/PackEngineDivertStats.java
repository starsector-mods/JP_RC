package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class PackEngineDivertStats extends BaseShipSystemScript {

	public static final float MOBILITY_BONUS_PERCENT = 60f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float mult = 1f + (MOBILITY_BONUS_PERCENT * 0.01f) * effectLevel;
		stats.getMaxSpeed().modifyMult(id, mult);
		stats.getAcceleration().modifyMult(id, mult);
		stats.getDeceleration().modifyMult(id, mult);
		stats.getTurnAcceleration().modifyMult(id, mult);
		stats.getMaxTurnRate().modifyMult(id, mult);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("divert to engines: +" + (int) Math.round(MOBILITY_BONUS_PERCENT * effectLevel) + "% speed & agility", false);
		} else if (index == 1) {
			return new StatusData("shield inoperative", true);
		}
		return null;
	}
}
