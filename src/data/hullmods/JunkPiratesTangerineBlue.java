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
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
/**
 *
 * @author paul
 */
public class JunkPiratesTangerineBlue extends BaseHullMod {

        public static final float ZAP_FREQUENCY = 0.9f; // in seconds - controls lag and ultimately power of hullmod
	
        private float timestamp = 0f;
        
        private static final Color FRINGE_COLOR = new Color(190, 135, 150, 225);
        private static final Color CORE_COLOR = new Color(190, 135, 150, 225);
        
        public static final String ELECTROCHAFF_PROJ_BASE_ID = "junk_pirates_electrochaff_copy";
        public static final float EMP_ARC_RANGE = 750f;
    
        public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }
        String timeKey = "tb_timestamp_" + ship.getId();
        Object stored = Global.getCombatEngine().getCustomData().get(timeKey);
        float currentElapsedTime = Global.getCombatEngine().getTotalElapsedTime(false);
        if (stored == null) {
            Global.getCombatEngine().getCustomData().put(timeKey, currentElapsedTime);
            return;
        }
        float lastTime = (float) stored;
        float time = currentElapsedTime - lastTime;
        
        if (time >= ZAP_FREQUENCY) {
            Global.getCombatEngine().getCustomData().put(timeKey, currentElapsedTime);
            
            FluxTrackerAPI fluxTracker = ship.getFluxTracker();
            float fluxLevel = (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux()) * 100f;

                if (MathUtils.getRandomNumberInRange(0, 100) < fluxLevel) {
                    
                        CombatEntityAPI target;
                        WeightedRandomPicker<MissileAPI> ctargets = new WeightedRandomPicker<>();
                        WeightedRandomPicker<MissileAPI> mtargets = new WeightedRandomPicker<>();
                        WeightedRandomPicker<ShipAPI> stargets = new WeightedRandomPicker<>();
                        List<MissileAPI> proj = CombatUtils.getMissilesWithinRange(ship.getLocation(), EMP_ARC_RANGE);
                        List<CombatEntityAPI> chaffed = new ArrayList<>();
                        
                        int chaffCount = 0;
                        for (MissileAPI c : proj) {

                            if (ELECTROCHAFF_PROJ_BASE_ID.equals(c.getProjectileSpecId())) {
                                ctargets.add(c);
                                chaffCount++;
                            }
                        }

                        if (chaffCount > 0) {
                                target = ctargets.pickAndRemove();

                                if (target == null) {
                                    return;
                                } else {

                                Global.getCombatEngine().spawnEmpArc(ship,
                                ship.getLocation(),
                                ship,
                                target,
                                DamageType.ENERGY,
                                200f,
                                40f,
                                6969420f,
                                "tachyon_lance_emp_impact",
                                15f,
                                FRINGE_COLOR,
                                CORE_COLOR);

                                chaffed.add(target);

                                }
                            }
                        
            List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), EMP_ARC_RANGE);
            List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(), EMP_ARC_RANGE);

            int mcount = 0;
            for (MissileAPI m : missiles) {

                if (m.getOwner() != ship.getOwner()) {
                    mtargets.add(m);
                    mcount++;
                }
            }
            int scount = 0;
            for (ShipAPI s : ships) {
                if (s.getOwner() != ship.getOwner()) {
                    stargets.add(s);
                    scount++;
                }
            }
            int targetMaxCount = mcount + scount;

            float chaffdam = 450;
            float chaffemp = 250;
            
            if(!chaffed.isEmpty()) {
                CombatEntityAPI x = chaffed.get(0);
                if (MathUtils.getRandomNumberInRange(0, targetMaxCount) <= scount) {
                    target = stargets.pick();
                } else {
                    target = mtargets.pick();
                }
                
                if (target == null) {
                    return;
                } else {
                    
                Global.getCombatEngine().spawnEmpArc(ship,
                x.getLocation(),
                x,
                target,
                DamageType.ENERGY,
                chaffdam,
                chaffemp,
                6969420f,
                "tachyon_lance_emp_impact",
                15f,
                FRINGE_COLOR,
                CORE_COLOR);
                }
            } else {
            
                    float dam = 100;
                    float emp = 180;


                    if (MathUtils.getRandomNumberInRange(0, targetMaxCount) <= scount) {
                        target = stargets.pick();
                    } else {
                        target = mtargets.pick();
                    }

                    if (target == null) {
                        return;
                    } else {

                    Global.getCombatEngine().spawnEmpArc(ship,
                    ship.getLocation(),
                    ship,
                    target,
                    DamageType.ENERGY,
                    dam,
                    emp,
                    6969420f,
                    "tachyon_lance_emp_impact",
                    15f,
                    FRINGE_COLOR,
                    CORE_COLOR);
                    }
                        
                }   

            }
        }
    }
    
}
