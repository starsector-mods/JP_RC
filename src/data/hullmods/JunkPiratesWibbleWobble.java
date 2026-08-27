/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
/**
 *
 * @author paul
 */
public class JunkPiratesWibbleWobble extends BaseHullMod {

        // base range in su for EMP arcs to missiles/ships
        public static final float ANGLE_MOD = 3f;
        public static final float TOP_SPEED_BASIS = 60f;
        public static final Vector2f BASE_HEADING = new Vector2f(0f ,1f);
        private float initialModuleFacing = 0f;
        private float forwardOrBackward = 0f;
        
//        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
//		stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
//		stats.getBreakProb().modifyMult(id, 0f);
//	}
        
	public String getDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + (int) MAX_SPEED_BONUS_PC + "%";
		//if (index == 1) return "" + (int) MAX_DEC_BONUS_PC + "%";
		//if (index == 2) return "" + (int) MAX_ACC_BONUS_PC + "%";
		//if (index == 3) return "" + (int) MAX_TURN_BONUS_PC + "%";
		//if (index == 1) return "" + (int) VENT_RATE_BONUS + "%";
		return null;
	}
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.isStationModule()) {
            ship.setHullSize(HullSize.CAPITAL_SHIP);
        }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }

        if (ship.isStationModule()) {
            if (Global.getCombatEngine().getTotalElapsedTime(false) > 1f) {
                if (ship.getHullSize() != HullSize.FRIGATE) {
                    ship.setHullSize(HullSize.FRIGATE);
                }
            }

            ship.setCollisionClass(com.fs.starfarer.api.combat.CollisionClass.SHIP);
            WeaponSlotAPI slot = ship.getStationSlot();
            //for (ShipAPI slot : face) {
            //    if (slot.equals(ship)) {
                    initialModuleFacing = slot.getAngle() + ship.getParentStation().getFacing();
                    
                    Vector2f speeed = ship.getParentStation().getVelocity();
                    
                    float speeeed = speeed.length() / TOP_SPEED_BASIS;
                    
                    float directionTravel = VectorUtils.getFacing(speeed);
                    float directionPointing = ship.getParentStation().getFacing();
                    
                    if (directionTravel > directionPointing) {
                        forwardOrBackward = 360 - directionTravel + directionPointing;
                    } else {
                        forwardOrBackward = directionPointing - directionTravel;
                    }
                    

                    double dribble = Math.toRadians(forwardOrBackward);
                    
                    float fuckMe = (float) Math.cos(dribble);
                    float newFacing = initialModuleFacing - (speeeed * ANGLE_MOD * fuckMe);
                    if (newFacing >= 360) {
                        newFacing -= 360;
                    }
                    ship.setFacing(newFacing);

                    

            //    }
            //}
        }
    }
}
