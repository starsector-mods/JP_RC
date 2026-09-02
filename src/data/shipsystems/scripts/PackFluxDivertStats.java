package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class PackFluxDivertStats extends BaseShipSystemScript {

	public static final float SHIELD_DAMAGE_REDUCTION_PERCENT = 40f;
	public static final float SHIELD_TURN_BONUS_PERCENT = 20f;
	public static final float SPEED_PENALTY_PERCENT = 30f;
	public static final float MANEUVER_PENALTY_PERCENT = 50f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		stats.getMaxSpeed().modifyMult(id, 1f - (SPEED_PENALTY_PERCENT * 0.01f) * effectLevel);
		stats.getAcceleration().modifyMult(id, 1f - (MANEUVER_PENALTY_PERCENT * 0.01f) * effectLevel);
		stats.getDeceleration().modifyMult(id, 1f - (MANEUVER_PENALTY_PERCENT * 0.01f) * effectLevel);
		stats.getTurnAcceleration().modifyMult(id, 1f - (MANEUVER_PENALTY_PERCENT * 0.01f) * effectLevel);
		stats.getMaxTurnRate().modifyMult(id, 1f - (MANEUVER_PENALTY_PERCENT * 0.01f) * effectLevel);
		stats.getShieldTurnRateMult().modifyMult(id, 1f + (SHIELD_TURN_BONUS_PERCENT * 0.01f) * effectLevel);
		stats.getShieldDamageTakenMult().modifyMult(id, 1f - (SHIELD_DAMAGE_REDUCTION_PERCENT * 0.01f) * effectLevel);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getShieldTurnRateMult().unmodify(id);
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("shield damage taken -" + (int) Math.round(SHIELD_DAMAGE_REDUCTION_PERCENT * effectLevel) + "%", false);
		} else if (index == 1) {
			return new StatusData("shield turn rate +" + (int) Math.round(SHIELD_TURN_BONUS_PERCENT * effectLevel) + "%", false);
		} else if (index == 2) {
			return new StatusData("speed -" + (int) Math.round(SPEED_PENALTY_PERCENT * effectLevel) + "%, agility -" + (int) Math.round(MANEUVER_PENALTY_PERCENT * effectLevel) + "%", true);
		}
		return null;
	}
}
