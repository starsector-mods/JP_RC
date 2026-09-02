/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
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
public class JunkPiratesTangerine extends BaseHullMod {

        // base range in su for EMP arcs to missiles/ships
        public static final float EMP_ARC_RANGE = 750f;
	public static final float FLUX_RESISTANCE = 50f;
	//public static final float DISSIPATION_BONUS = 10f;
	public static final float VENT_RATE_BONUS = 25f;
        public static final float MAX_SPEED_BONUS_PC = 35f;
        public static final float MAX_DEC_BONUS_PC = 40f;
        public static final float MAX_ACC_BONUS_PC = 40f;
        public static final float MAX_TURN_BONUS_PC = 30f;
        @SuppressWarnings("unused")
                private float timestamp = 0f;
        @SuppressWarnings("unused")
                private int numberBursts = 0;
        public static final float TIME_BETWEEN_BURST = 0.45f;
        
        public static String JPT_ICON = "graphics/icons/tactical/metaStableDriveField.png";
        public static String JPT_ID = "JunkPiratesTangerine";
        public static String JPTCC_ID = "JunkPiratesChaffCatcher";
        public static String JPT_NAME = "Metastable Drive Field";
        public static String JPTCC_NAME = "CHAFF CATCHER";
        
        
        public static final String ELECTROCHAFF_PROJ_BASE_ID = "junk_pirates_electrochaff_copy";
	
        private static final Color FRINGE_COLOR = new Color(135, 190, 150, 225);
        private static final Color CORE_COLOR = new Color(135, 190, 150, 225);
        
        public static final float SMOD_OVERLOAD_REDUCTION = 25f;
        public static final float SMOD_EMP_RESISTANCE = 30f;

        @Override
        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
		stats.getBreakProb().modifyMult(id, 0f);
		if (isSMod(stats)) {
			stats.getOverloadTimeMod().modifyMult(id, (100f - SMOD_OVERLOAD_REDUCTION) / 100f);
			stats.getEmpDamageTakenMult().modifyMult(id, (100f - SMOD_EMP_RESISTANCE) / 100f);
		}
	}
        
	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) MAX_SPEED_BONUS_PC + "%";
		return null;
	}

	@Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) SMOD_OVERLOAD_REDUCTION + "%";
		if (index == 1) return "" + (int) SMOD_EMP_RESISTANCE + "%";
		return null;
	}

	@Override
	public boolean hasSModEffect() {
		return true;
	}
    
        public void advanceInCombat(ShipAPI ship, float amount) { // borrowed from SRA Harmonic Shield Conduits for reference
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }
        
        FluxTrackerAPI fluxTracker = ship.getFluxTracker();
        float maxSpeedBonus = MAX_SPEED_BONUS_PC * (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux());
        float maxDecBonus = MAX_DEC_BONUS_PC * (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux());
        float maxAccBonus = MAX_ACC_BONUS_PC * (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux());
        float maxTurnBonus = MAX_TURN_BONUS_PC * (fluxTracker.getHardFlux() / fluxTracker.getMaxFlux());
        ship.getMutableStats().getMaxSpeed().modifyPercent("Tangerine", maxSpeedBonus);
        ship.getMutableStats().getDeceleration().modifyPercent("Tangerine", maxDecBonus);
        ship.getMutableStats().getAcceleration().modifyPercent("Tangerine", maxAccBonus);
        ship.getMutableStats().getTurnAcceleration().modifyPercent("Tangerine", maxTurnBonus);

        
        if (ship == Global.getCombatEngine().getPlayerShip() && maxSpeedBonus >= 1f) {
            Global.getCombatEngine().maintainStatusForPlayerShip(JPT_ID, JPT_ICON, JPT_NAME, "+" + (int) maxSpeedBonus + "% top speed & agility", false);
        }
        
//        float otm = ship.getMutableStats().getOverloadTimeMod().percentMod;
//        if (otm < 0 ) {
//            Global.getCombatEngine().maintainStatusForPlayerShip(JPT_ICON, JPTCC_ID, JPTCC_NAME, "Last overload time "+(int) otm+"%", true);
//        }
    }
}
