/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.util.HashSet;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.Misc;

/**
 *
 * @author paul sort of
 */
public class JunkPiratesBurstJets extends BaseShipSystemScript {

    private int flaresLaunched = 0;
    private float timestamp = 0f;
    public static final float TIME_BETWEEN_FLARES = 0.70f;
    public static final int MAX_FLARES = 5;
    public static final String ELECTRO_FLARE_WEAPON_ID = "junk_pirates_electrochafflauncher";
    public static final float SPEED_BONUS = 100f;
    public static final float ACCEL_BONUS = 150f;
    public static final float TURN_ACCEL_BONUS = 100f;
    public static final float TURN_RATE_BONUS = 50f;
     
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats == null) {
			return;
		}
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
			stats.getAcceleration().modifyPercent(id, ACCEL_BONUS * effectLevel);
			stats.getDeceleration().modifyPercent(id, ACCEL_BONUS * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);
			stats.getMaxTurnRate().modifyPercent(id, TURN_RATE_BONUS * effectLevel);
		}

		if (!(stats.getEntity() instanceof ShipAPI)) {
			return;
		}
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (state == ShipSystemStatsScript.State.IN || state == ShipSystemStatsScript.State.ACTIVE) {
			ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
		}
		if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
			return;
		}

		if (timestamp == 0f) {
			timestamp = Global.getCombatEngine().getTotalElapsedTime(false);
		}
		float time = Global.getCombatEngine().getTotalElapsedTime(false) - timestamp;

		if ((time >= flaresLaunched * TIME_BETWEEN_FLARES) && (flaresLaunched < MAX_FLARES)) {
			flaresLaunched++;

			if (Global.getSoundPlayer() != null && ship.getLocation() != null && ship.getVelocity() != null) {
				Global.getSoundPlayer().playSound("system_flare_launcher_active", 1f, 1f, ship.getLocation(), ship.getVelocity());
			}
			java.util.List<ShipEngineAPI> engines = null;
			if (ship.getEngineController() != null) {
				engines = ship.getEngineController().getShipEngines();
			}
			if (engines != null && !engines.isEmpty()) {
				int count = (engines.size() >= 2) ? 2 : 1;
				for (int i = 0; i < count; i++) {
					ShipEngineAPI engine = engines.get((flaresLaunched + i) % engines.size());
					if (engine != null && engine.getEngineSlot() != null && engine.getLocation() != null) {
						float nozzleAngle = Misc.normalizeAngle(ship.getFacing() + engine.getEngineSlot().getAngle() + MathUtils.getRandomNumberInRange(-8f, 8f));
						Vector2f spawnLoc = MathUtils.getPointOnCircumference(engine.getLocation(), 4f, nozzleAngle);
						Global.getCombatEngine().spawnProjectile(ship, null, ELECTRO_FLARE_WEAPON_ID, spawnLoc,
								nozzleAngle, ship.getVelocity());
					}
				}
			} else if (ship.getLocation() != null) {
				float spawnAngle = Misc.normalizeAngle(ship.getFacing() + 180f + MathUtils.getRandomNumberInRange(-15f, 15f));
				Vector2f spawnLoc = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.6f, spawnAngle);
				Global.getCombatEngine().spawnProjectile(ship, null, ELECTRO_FLARE_WEAPON_ID, spawnLoc,
						spawnAngle, ship.getVelocity());
			}
		}
	}
            
	public void unapply(MutableShipStatsAPI stats, String id) {
		if (stats != null) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
		}
                
		flaresLaunched = 0;
		timestamp = 0f;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("sustained mobility boost", false);
		} else if (index == 1) {
			if (state == State.OUT) return null;
			return new StatusData("+" + (int) SPEED_BONUS + " top speed", false);
		}
		return null;
	}
   
}
