/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.shipsystems.scripts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.Misc;

public class TrilateralAugmentationAlgorithmsStats extends BaseShipSystemScript {
	public static final Object KEY_JITTER = new Object();
	
	public static final float DAMAGE_INCREASE_PERCENT = 50;
	public static final float MISSILE_DAMAGE_INCREASE_PERCENT = 25;
        
        public static final float SPEED_INCREASE = 70;
        public static final float ACCEL_INCREASE = 50;
        public static final float DECEL_INCREASE = 50;
	
	public static final Color JITTER_UNDER_COLOR = new Color(55,0,255,125);
	public static final Color JITTER_COLOR = new Color(45,0,255,75);

	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		
		if (effectLevel > 0) {
			float jitterLevel = effectLevel;
			float maxRangeBonus = 5f;
			float jitterRangeBonus = jitterLevel * maxRangeBonus;
			for (ShipAPI fighter : getFighters(ship)) {
				if (fighter.isHulk()) continue;
				MutableShipStatsAPI fStats = fighter.getMutableStats();
				//fStats.getMaxSpeed().modifyFlat(id, SPEED_INCREASE);
				//fStats.getAcceleration().modifyFlat(id, ACCEL_INCREASE);
				//fStats.getDeceleration().modifyFlat(id, DECEL_INCREASE);
				fStats.getBallisticWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE_PERCENT * effectLevel);
				fStats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_INCREASE_PERCENT * effectLevel);
				fStats.getMissileWeaponDamageMult().modifyPercent(id, MISSILE_DAMAGE_INCREASE_PERCENT * effectLevel);
                                
				if (jitterLevel > 0) {
					//fighter.setWeaponGlow(effectLevel, new Color(255,50,0,125), EnumSet.allOf(WeaponType.class));
					fighter.setWeaponGlow(effectLevel, Misc.setAlpha(JITTER_UNDER_COLOR, 255), EnumSet.allOf(WeaponType.class));
					
					fighter.setJitterUnder(KEY_JITTER, JITTER_COLOR, jitterLevel, 5, 0f, jitterRangeBonus);
					fighter.setJitter(KEY_JITTER, JITTER_UNDER_COLOR, jitterLevel, 2, 0f, 0 + jitterRangeBonus * 1f);
					Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1f, 1f, fighter.getLocation(), fighter.getVelocity());
				}
			}
		}
	}
	
	private List<ShipAPI> getFighters(ShipAPI carrier) {
		List<ShipAPI> result = new ArrayList<ShipAPI>();
		
		for (com.fs.starfarer.api.combat.FighterLaunchBayAPI bay : carrier.getLaunchBaysCopy()) {
			if (bay.getWing() == null) continue;
			result.addAll(bay.getWing().getWingMembers());
		}
		
		return result;
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		for (ShipAPI fighter : getFighters(ship)) {
			if (fighter.isHulk()) continue;
			MutableShipStatsAPI fStats = fighter.getMutableStats();
                        
			//fStats.getMaxSpeed().unmodify(id);
			//fStats.getAcceleration().unmodify(id);
			//fStats.getDeceleration().unmodify(id);
			fStats.getBallisticWeaponDamageMult().unmodify(id);
			fStats.getEnergyWeaponDamageMult().unmodify(id);
			fStats.getMissileWeaponDamageMult().unmodify(id);
		}
	}
	
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			float mult = 1f + DAMAGE_INCREASE_PERCENT * effectLevel * 0.01f;
			return new StatusData("fighter guns: " + Misc.getRoundedValueMaxOneAfterDecimal(mult) + "x damage", false);
		} else if (index == 1) {
			float missileMult = 1f + MISSILE_DAMAGE_INCREASE_PERCENT * effectLevel * 0.01f;
			return new StatusData("fighter missiles: " + Misc.getRoundedValueMaxOneAfterDecimal(missileMult) + "x damage", false);
		}
		return null;
	}

	
}








/**
 *
 * @author paul
 */