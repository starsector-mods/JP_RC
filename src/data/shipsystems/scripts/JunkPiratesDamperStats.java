package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class JunkPiratesDamperStats extends BaseShipSystemScript {

	// UI display for 50% damage reduction (since the hullmod handles the actual stats)
	public static final float DAMAGE_MULT = 0.5f; 
	
	protected Object STATUSKEY1 = new Object();

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float targetNetMult = 1f - (1f - DAMAGE_MULT) * effectLevel;
		float activeMult = targetNetMult / (1f - data.hullmods.JunkPiratesDamperHullMod.PASSIVE_REDUCTION);
		stats.getArmorDamageTakenMult().modifyMult(id, activeMult);
		stats.getHullDamageTakenMult().modifyMult(id, activeMult);
		stats.getEmpDamageTakenMult().modifyMult(id, activeMult);
		
		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				ShipSystemAPI system = ship.getPhaseCloak();
				if (system == null || !"junk_pirates_damper".equals(system.getId())) {
					system = ship.getSystem();
				}
				if (system != null && "junk_pirates_damper".equals(system.getId())) {
					float percent = (1f - targetNetMult) * 100f;
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
						system.getSpecAPI().getIconSpriteName(), system.getDisplayName(),
						"reducing incoming damage by " + (int) Math.round(percent) + "%", false);
				}
			}
		}
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
		
		if (index == 0) {
			return new StatusData("reducing incoming damage by " + (int) Math.round(percent) + "%", false);
		}
		return null;
	}
}
