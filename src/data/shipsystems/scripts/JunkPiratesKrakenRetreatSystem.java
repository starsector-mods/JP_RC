/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author paul sort of
 * Based heavily on the Shadowyards Woop Drive (it wasn't as simple as I thought it might be to put negative reverse ...
 */
public class JunkPiratesKrakenRetreatSystem extends BaseShipSystemScript {

    private int flaresLaunched = 0;
    private float timestamp = 0f;
    public static final float TIME_BETWEEN_FLARES = 0.1f;
    public static final int MAX_FLARES = 4;
    public static final String ELECTRO_FLARE_WEAPON_ID = "junk_pirates_electrochafflauncher";
    private static final Set<String> FLARE_SLOT_IDS = new HashSet<>(8);
    public static final Color EXPLOSION_COLOR_RED = Color.red;
    public static final Color EXPLOSION_COLOR_YELLOW = Color.yellow;
    public static final float EXPLOSION_DURATION = 1.2f;
    public static final float SMOKE_DURATION = 2.4f;
    public static final float SMOKE_EXTRA_VEL = 10f;
    
    //private static final String DATA_KEY = "junk_pirates_kraken_retreat";
    
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
            CombatEngineAPI combatEngine = Global.getCombatEngine();

            if (combatEngine.isPaused()) {
                return;
            }

            if (!(stats.getEntity() instanceof ShipAPI)) {
                return;
            }
            ShipAPI ship = (ShipAPI) stats.getEntity();
            //So we need to get the ships facing, then apply the thrust in reverse
            
            //target a vector directly behind the ship
            Vector2f dir;
            Vector2f point = new Vector2f(-50f, 0f);
            VectorUtils.rotate(point, ship.getFacing(), point);
            Vector2f.add(point, ship.getLocation(), point);
            
            dir = (Vector2f) VectorUtils.getDirectionalVector(ship.getLocation(), point).scale(50f);
            Vector2f.add(ship.getVelocity(), dir, ship.getVelocity());
            
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			float speed = ship.getVelocity().length();
			if (speed > 0.001f) {
				ship.getVelocity().normalise();
				ship.getVelocity().scale(stats.getMaxSpeed().getModifiedValue());
			}
		} else {
			stats.getMaxSpeed().modifyFlat(id, 120f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 80f * effectLevel);
			float speed = ship.getVelocity().length();
			if (speed <= 0.5f) {
				//point the ships vector behind it
				ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), dir)).scale(stats.getMaxSpeed().getModifiedValue());
			} else {
				ship.getVelocity().normalise();
				ship.getVelocity().scale(stats.getMaxSpeed().getModifiedValue());
			}
		}
                
            if (stats.getEntity() instanceof ShipAPI && Boolean.FALSE.booleanValue()) {
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
                
            if (timestamp == 0f)
            {
                timestamp = Global.getCombatEngine().getTotalElapsedTime(false);
            }
            float time = Global.getCombatEngine().getTotalElapsedTime(false) - timestamp;

            if ((time >= flaresLaunched * TIME_BETWEEN_FLARES) && (flaresLaunched < MAX_FLARES))
//            if (flaresLaunched < MAX_FLARES)
            {
                flaresLaunched++;

                Global.getSoundPlayer().playSound("hit_heavy", 1f, 1f, ship.getLocation(), ship.getVelocity());
                Vector2f shipVel = ship.getVelocity();
                Vector2f smokeVel = new Vector2f(shipVel);
                VectorUtils.resize(smokeVel, SMOKE_EXTRA_VEL, smokeVel);
                for (WeaponAPI weapon : ship.getAllWeapons())
                {
                    WeaponSlotAPI slot = weapon.getSlot();
                    if (FLARE_SLOT_IDS.contains(slot.getId()))
                    {
                        Global.getCombatEngine().spawnProjectile(ship, weapon, ELECTRO_FLARE_WEAPON_ID, weapon.getLocation(),
                        weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-15f, 15f), shipVel);
                        float redExplosionSize = MathUtils.getRandomNumberInRange(10, 10 * flaresLaunched);
                        float yellowExplosionSize = MathUtils.getRandomNumberInRange(10, 5 * flaresLaunched);
                        if (flaresLaunched == 1) {
                            Global.getCombatEngine().spawnExplosion(weapon.getLocation(), shipVel, EXPLOSION_COLOR_RED, redExplosionSize, EXPLOSION_DURATION);
                            Global.getCombatEngine().spawnExplosion(weapon.getLocation(), shipVel, EXPLOSION_COLOR_YELLOW, yellowExplosionSize, EXPLOSION_DURATION);
                            Global.getCombatEngine().addSmokeParticle(weapon.getLocation(), smokeVel, redExplosionSize * 1.5f, 0.4f, SMOKE_DURATION, Color.gray);
                        } else {
                            Global.getCombatEngine().addSmokeParticle(weapon.getLocation(), shipVel, redExplosionSize * 0.6f * flaresLaunched, 0.3f, SMOKE_DURATION, Color.gray);
                        }
                    } //ship.getVelocity()
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
                timestamp = 0f;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Mass Ejected", false);
		} else if (index == 1) {
			return new StatusData("Full Reverse Speed", false);
		}
		return null;
	}
   
}
