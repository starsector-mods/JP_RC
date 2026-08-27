package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class JunkPiratesDamperStats extends BaseShipSystemScript {

	// UI display for 50% damage reduction (since the hullmod handles the actual stats)
	public static final float DAMAGE_MULT = 0.5f; 

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float mult = 1f - (1f - DAMAGE_MULT) * effectLevel;
		stats.getArmorDamageTakenMult().modifyMult(id, mult);
		stats.getHullDamageTakenMult().modifyMult(id, mult);
		stats.getEmpDamageTakenMult().modifyMult(id, mult);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		effectLevel = Math.max(0f, effectLevel);
		float mult = 1f - (1f - DAMAGE_MULT) * effectLevel;
		float percent = (1f - mult) * 100f;
		
		if (index == 0 && percent > 0) {
			return new StatusData("reducing incoming damage by " + (int) percent + "%", false);
		}
		return null;
	}
}
