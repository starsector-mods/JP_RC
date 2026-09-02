/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.shipsystems.scripts;

/**
 *
 * @author paul
 */
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class PackKefluskerStats extends BaseShipSystemScript {

	public static final float SPEED_BONUS = 260f;
	public static final float ACCEL_BONUS = 320f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS * effectLevel);
			stats.getAcceleration().modifyFlat(id, ACCEL_BONUS * effectLevel);
			//stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			if (state == State.OUT) return null;
			return new StatusData("keflusker boost: +" + (int)(SPEED_BONUS * effectLevel) + " top speed", false);
		}
		return null;
	}
}
