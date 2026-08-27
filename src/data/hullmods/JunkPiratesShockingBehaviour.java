/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
/**
 *
 * @author paul
 */
public class JunkPiratesShockingBehaviour extends BaseHullMod {
        
        public static final Set<String> ZAP_SLOT_IDS = new HashSet<>(8);
        public static final float ZAP_FREQUENCY = 0.5f;
    
        private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);
        
        static {
                BLOCKED_HULLMODS.add("swp_extrememods");
            }
        
        static
        {
            ZAP_SLOT_IDS.add("ZAP1");
            ZAP_SLOT_IDS.add("ZAP2");
            ZAP_SLOT_IDS.add("ZAP3");
            ZAP_SLOT_IDS.add("ZAP4");
            ZAP_SLOT_IDS.add("ZAP5");
            ZAP_SLOT_IDS.add("ZAP6");
            ZAP_SLOT_IDS.add("ZAP7");
            ZAP_SLOT_IDS.add("ZAP8");
        }
	
        private float timestamp = 0f;
        
        private static final Color FRINGE_COLOR = new Color(190, 135, 150, 225);
        private static final Color CORE_COLOR = new Color(190, 135, 150, 225);
             
//	public String getDescriptionParam(int index, HullSize hullSize) {
//		if (index == 0) return "" + (int) MAX_SPEED_BONUS_PC + "%";
//		//if (index == 1) return "" + (int) MAX_DEC_BONUS_PC + "%";
//		//if (index == 2) return "" + (int) MAX_ACC_BONUS_PC + "%";
//		//if (index == 3) return "" + (int) MAX_TURN_BONUS_PC + "%";
//		return null;
//	}
    
        public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }
        if (ship.getParentStation() == null) {
            return;
        }
        String timeKey = "shocking_timestamp_" + ship.getId();
        Object stored = Global.getCombatEngine().getCustomData().get(timeKey);
        float currentElapsedTime = Global.getCombatEngine().getTotalElapsedTime(false);
        if (stored == null) {
            Global.getCombatEngine().getCustomData().put(timeKey, currentElapsedTime);
            return;
        }
        float lastTime = (float) stored;
        float time = currentElapsedTime - lastTime;
        
        if (time >= ZAP_FREQUENCY) {

            FluxTrackerAPI fluxTracker = ship.getFluxTracker();
            float fluxLevel = (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux()) * 100f;

            if (MathUtils.getRandomNumberInRange(0, 100) < fluxLevel) {

                    Global.getCombatEngine().spawnEmpArc(ship,
                    ship.getLocation(),
                    ship,
                    ship.getParentStation(),
                    DamageType.ENERGY,
                    0f,
                    35f,
                    6969420f,
                    "junk_pirates_shocking_behavour",
                    15f,
                    FRINGE_COLOR,
                    CORE_COLOR);

                    }
                Global.getCombatEngine().getCustomData().put(timeKey, currentElapsedTime);
                }
            }
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
            }
        }
    }
        
        
    }
