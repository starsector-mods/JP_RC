/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.util.HashSet;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;

/**
 *
 * @author paul sort of
 */
public class JunkPiratesCruiserBurstJets extends BaseShipSystemScript {

    private int flaresLaunched = 0;
    //private float timestamp = 0f;
    public static final float TIME_BETWEEN_FLARES = 0.45f;
    public static final int MAX_FLARES = 3;
    public static final String ELECTRO_FLARE_WEAPON_ID = "junk_pirates_electrochafflauncher";
    private static final Set<String> FLARE_SLOT_IDS = new HashSet<>(8);
    
    static
    {
        FLARE_SLOT_IDS.add("FLARE1");
        FLARE_SLOT_IDS.add("FLARE2");
        FLARE_SLOT_IDS.add("FLARE3");
        FLARE_SLOT_IDS.add("FLARE4");
        FLARE_SLOT_IDS.add("FLARE5");
        FLARE_SLOT_IDS.add("FLARE6");
        FLARE_SLOT_IDS.add("FLARE7");
        FLARE_SLOT_IDS.add("FLARE8");
    }
     
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
            if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 240f);
			stats.getAcceleration().modifyPercent(id, 400f * effectLevel);
			stats.getDeceleration().modifyPercent(id, 400f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, 100f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, 250f * effectLevel);
			stats.getMaxTurnRate().modifyFlat(id, 85f);
			stats.getMaxTurnRate().modifyPercent(id, 250f);
		}
		
                
                
		if (stats.getEntity() instanceof ShipAPI && Boolean.FALSE.booleanValue()) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			String key = ship.getId() + "_" + id;
			Object test = Global.getCombatEngine().getCustomData().get(key);
			if (state == State.IN) {
				if (test == null && effectLevel > 0.2f) {
					Global.getCombatEngine().getCustomData().put(key, new Object());
					ship.getEngineController().getExtendLengthFraction().advance(1f);
					for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
						if (engine.isSystemActivated()) {
							ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
						}
					}
				}
			} else {
				Global.getCombatEngine().getCustomData().remove(key);
			}
		}
                
//            if (timestamp == 0f)
//            {
//                timestamp = Global.getCombatEngine().getTotalElapsedTime(false);
//            }
//            float time = Global.getCombatEngine().getTotalElapsedTime(false) - timestamp;

//            if ((time >= flaresLaunched * TIME_BETWEEN_FLARES) && (flaresLaunched < MAX_FLARES))
            if (flaresLaunched < MAX_FLARES)
            {
                flaresLaunched++;
                ShipAPI ship = (ShipAPI) stats.getEntity();
                if (ship == null)
                {
                    return;
                }

                Global.getSoundPlayer().playSound("system_flare_launcher_active", 1f, 1f, ship.getLocation(), ship.getVelocity());
                for (WeaponAPI weapon : ship.getAllWeapons())
                {
                    WeaponSlotAPI slot = weapon.getSlot();
                    if (FLARE_SLOT_IDS.contains(slot.getId()))
                    {
                        Global.getCombatEngine().spawnProjectile(ship, weapon, ELECTRO_FLARE_WEAPON_ID, weapon.getLocation(),
                        weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-15f, 15f), ship.getVelocity());
                    }
                }
            }
        }
            
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
                
                flaresLaunched = 0;
                //timestamp = 0f;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("cryo-burst active", false);
		} else if (index == 1) {
			return new StatusData("+240 top speed", false);
		}
		return null;
	}
   
}
