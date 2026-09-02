/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author paul
 */
public class JunkPiratesPackMesh extends BaseHullMod {
        
        public static final float UPDATE_FREQUENCY_MINOR = 0.6f;
        public static final float UPDATE_FREQUENCY_MAJOR = 3.6f;
        
        private static final String CALIBRATING_TEXT = "-synchronising mesh-";
        
        private static final float SPEED_BONUS_MULT = 5f;
        private static final float RANGE_BONUS_MULT = 3f;
        private static final float DAMAGE_BONUS_MULT = 4f;
        
        private static final float WEAPON_FLUX_MULT = 10f; // this is weapon flux cost reduction pc
        private static final float SHIP_FLUX_MULT = 10f; // this is flux / cap increase pc
        
//        private static final float SPEED_MALUS_MULT = 10f;
//        private static final float RANGE_MALUS_MULT = 4f;
//        private static final float DAMAGE_MALUS_MULT = 8f;
        
        public static String PM_ICON = "graphics/icons/tactical/packMesh.png";
        public static String PM_ID = "JunkPiratesPackMesh";
        public static String PM_ID5 = "JunkPiratesPackMesh5";
        public static String PM_ID10 = "JunkPiratesPackMesh10";
        public static String PM_NAME = "PACK Mesh";
             
        public static final float SMOD_SPEED_BONUS = 5f;
        public static final float SMOD_FLUX_COST_BONUS = 5f;

        @Override
        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if (isSMod(stats)) {
			stats.getMaxSpeed().modifyPercent(id, SMOD_SPEED_BONUS);
			stats.getBallisticWeaponFluxCostMod().modifyMult(id, (100f - SMOD_FLUX_COST_BONUS) / 100f);
			stats.getEnergyWeaponFluxCostMod().modifyMult(id, (100f - SMOD_FLUX_COST_BONUS) / 100f);
			stats.getBeamWeaponFluxCostMult().modifyMult(id, (100f - SMOD_FLUX_COST_BONUS) / 100f);
		}
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) RANGE_BONUS_MULT + "%";
		if (index == 1) return "" + (int) DAMAGE_BONUS_MULT + "%";
		if (index == 2) return "" + (int) SPEED_BONUS_MULT + "%";
		if (index == 3) return "" + (int) WEAPON_FLUX_MULT + "%";
		if (index == 4) return "" + (int) SHIP_FLUX_MULT + "%";
		return null;
	}

	@Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) SMOD_SPEED_BONUS + "%";
		if (index == 1) return "" + (int) SMOD_FLUX_COST_BONUS + "%";
		return null;
	}

	@Override
	public boolean hasSModEffect() {
		return true;
	}
        
        @Override
        public void advanceInCombat(ShipAPI ship, float amount) {
            
//            super.advanceInCombat(ship, amount);
            Map<String, Object> packMeshCombatData = Global.getCombatEngine().getCustomData();
            
            if ( packMeshCombatData.get("timestamp_minor" + ship.getId()) instanceof Float ) { // it's an ... object?
                
            } else {
                packMeshCombatData.put("timestamp_minor" + ship.getId(), 0f);
                packMeshCombatData.put("meshstate" + ship.getId(), "initialise");
                packMeshCombatData.put("meshActive" + ship.getId(), false);
                
            }
            
            if ( packMeshCombatData.get("bonusONE" + ship.getId()) instanceof Boolean) {
                
            } else {
                    packMeshCombatData.put("bonusONE" + ship.getId(), false); // initialise these things
                    packMeshCombatData.put("bonusTWO" + ship.getId(), false);
            }
            
            if ( packMeshCombatData.get("timestamp_major" + ship.getId()) instanceof Float ) { // it's an ... object?
                
            } else {
                packMeshCombatData.put("timestamp_major" + ship.getId(), 0f);
            }
        
            if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
                return;
            }    



            if ((float) packMeshCombatData.get("timestamp_minor"+ship.getId()) == 0f)
                {
                    packMeshCombatData.put("timestamp_minor" + ship.getId(), Global.getCombatEngine().getTotalElapsedTime(false));
                }
            float time = Global.getCombatEngine().getTotalElapsedTime(false) - (float) packMeshCombatData.get("timestamp_minor"+ship.getId());

            if ((float) packMeshCombatData.get("timestamp_major"+ship.getId()) == 0f)
                {
                    packMeshCombatData.put("timestamp_major" + ship.getId(), Global.getCombatEngine().getTotalElapsedTime(false));
                }
            float time_major = Global.getCombatEngine().getTotalElapsedTime(false) - (float) packMeshCombatData.get("timestamp_major"+ship.getId());

            if (time_major >= UPDATE_FREQUENCY_MAJOR) {
                packMeshCombatData.put("meshstate" + ship.getId(), "calibrate");

                packMeshCombatData.put("timestamp_major" + ship.getId(), 0f);
            }
            
            if (time >= UPDATE_FREQUENCY_MINOR) {
                
                    String meshstate = (String) packMeshCombatData.get("meshstate" + ship.getId());

                    if (meshstate == null || "initialise".equals(meshstate)) {

                        turnOffBonus(ship);

                        packMeshCombatData.put("reportText" + ship.getId(),CALIBRATING_TEXT);

                        packMeshCombatData.put("meshstate" + ship.getId(), "calibrate");

    //                    packMeshCombatData.put("num_friendly_stored" + ship.getId(), countFriendlyShips(ship));

                    } else if ("calibrate".equals(meshstate)) {

                        if (packMeshCombatData.get("reportText" + ship.getId()) == null) {
                            packMeshCombatData.put("reportText" + ship.getId(), CALIBRATING_TEXT);
                        }

                        packMeshCombatData.put("meshstate" + ship.getId(), "operate");
    //                    packMeshCombatData.put("num_friendly_stored" + ship.getId(), countFriendlyShips(ship));
    //                    turnOffBonus(ship);

                    } else if ("operate".equals(meshstate)) {

                        HashMap<String, Integer> friendly_frigates = new HashMap<String, Integer>();
                        HashMap<String, Integer> friendly_destroyers = new HashMap<String, Integer>();
                        HashMap<String, Integer> friendly_cruisers = new HashMap<String, Integer>();

                        friendly_frigates.clear();
                        friendly_destroyers.clear();
                        friendly_cruisers.clear();

                        float speed_bonus = 0f;
                        float range_bonus = 0f;
                        float damage_bonus = 0f;

//                        float speed_malus = 0f;
//                        float range_malus = 0f;
//                        float damage_malus = 0f;                    

                        for (ShipAPI woof : Global.getCombatEngine().getShips()) {
//                            if ( countFriendlyShips(ship) <= 0 ) break;
                            if (woof.isAlive() && woof.getOwner() == ship.getOwner() && woof.getId() != ship.getId() && woof.getVariant().hasHullMod("pack_mesh")) {
                                if (woof.isFrigate()) {
                                    String hullKey = woof.getHullSpec().getHullName();
                                    if (friendly_frigates.containsKey(hullKey)) {
                                        friendly_frigates.put(hullKey, friendly_frigates.get(hullKey) + 1);
                                    } else {
                                        friendly_frigates.put(hullKey, 1);
                                    }
                                }

                                if (woof.isDestroyer()) {
                                    String hullKey = woof.getHullSpec().getHullName();
                                    if (friendly_destroyers.containsKey(hullKey)) {
                                        friendly_destroyers.put(hullKey, friendly_destroyers.get(hullKey) + 1);
                                    } else {
                                        friendly_destroyers.put(hullKey, 1);
                                    }
                                }

                                if (woof.isCruiser()) {
                                    String hullKey = woof.getHullSpec().getHullName();
                                    if (friendly_cruisers.containsKey(hullKey)) {
                                        friendly_cruisers.put(hullKey, friendly_cruisers.get(hullKey) + 1);
                                    } else {
                                        friendly_cruisers.put(hullKey, 1);
                                    }
                                }
                            }
                        }

                        range_bonus = friendly_frigates.size();
                        damage_bonus = friendly_destroyers.size();
                        speed_bonus = friendly_cruisers.size();
                        
                        float total_other_friendly_ships = range_bonus + damage_bonus + speed_bonus;
                        
                        if ( total_other_friendly_ships >= 1 ) {
                            packMeshCombatData.put("meshActive" + ship.getId(), true);
                        } else {
                            packMeshCombatData.put("meshActive" + ship.getId(), false);
                        }
                        
//                                for (String lilships : friendly_frigates.keySet()) { // keep in case ever implement batshit mode
//                                    speed_bonus += friendly_frigates.get(lilships);
//                                    range_malus += friendly_frigates.get(lilships) - 1;
//                                }
//
//                                for (String medships : friendly_destroyers.keySet()) {
//                                    range_bonus += friendly_destroyers.get(medships);
//                                    damage_malus += friendly_destroyers.get(medships) - 1;
//                                }
//
//                                for (String bigships : friendly_cruisers.keySet()) {
//                                    damage_bonus += friendly_cruisers.get(bigships);
//                                    speed_malus += friendly_cruisers.get(bigships) - 1;
//                                }
//
//                                if ( speed_malus < 0f) { speed_malus = 0f; }
//                                if ( range_malus < 0f) { range_malus = 0f; }
//                                if ( damage_malus < 0f) { damage_malus = 0f; }

    //                    float num_friendly_stored = (int) packMeshCombatData.get("num_friendly_stored" + ship.getId());

    //                    if (num_friendly_stored != countFriendlyShips(ship)) {
    //                        packMeshCombatData.put("meshstate" + ship.getId(), "calibrate");
    //                    } else {
    //                        applyShipBonuses(ship);
    //                    }

                    MutableShipStatsAPI stats = ship.getMutableStats();
                    String id = ship.getId();

                    float TOTAL_SPEED_BOOST = ( 100f + ( speed_bonus * SPEED_BONUS_MULT ) );
                    float TOTAL_RANGE_BOOST = ( 100f + ( range_bonus * RANGE_BONUS_MULT ) );
                    float TOTAL_DAMAGE_BOOST = ( 100f - ( damage_bonus * DAMAGE_BONUS_MULT ) ); // inverted as is damage reduction to shields

//                    float TOTAL_SPEED_DROP = ( 100f + ( speed_malus * SPEED_MALUS_MULT ) );
//                    float TOTAL_RANGE_DROP = ( 100f + ( range_malus * RANGE_MALUS_MULT ) );
//                    float TOTAL_DAMAGE_DROP = ( 100f + ( damage_malus * DAMAGE_MALUS_MULT ) );

//                    float TOTAL_SPEED_MULT = TOTAL_SPEED_BOOST - TOTAL_SPEED_DROP + 100;
//                    float TOTAL_RANGE_MULT = TOTAL_RANGE_BOOST - TOTAL_RANGE_DROP + 100;
//                    float TOTAL_DAMAGE_MULT = TOTAL_DAMAGE_DROP - TOTAL_DAMAGE_BOOST + 100;
//                    
//                    if ( TOTAL_SPEED_MULT > 130 ) TOTAL_SPEED_MULT = 130;
//                    if ( TOTAL_SPEED_MULT < 75 ) TOTAL_SPEED_MULT = 75;
//                    if ( TOTAL_RANGE_MULT > 115 ) TOTAL_RANGE_MULT = 115;
//                    if ( TOTAL_RANGE_MULT < 85 ) TOTAL_RANGE_MULT = 85;
//                    if ( TOTAL_DAMAGE_MULT > 120 ) TOTAL_DAMAGE_MULT = 120;
//                    if ( TOTAL_DAMAGE_MULT < 80 ) TOTAL_DAMAGE_MULT = 80;

                    int spdBonus = (int) (speed_bonus * SPEED_BONUS_MULT);
                    int rngBonus = (int) (range_bonus * RANGE_BONUS_MULT);
                    int shdBonus = (int) (damage_bonus * DAMAGE_BONUS_MULT);
                    packMeshCombatData.put("reportText" + ship.getId(), "Speed +" + spdBonus + "%, Range +" + rngBonus + "%, Shield Dmg -" + shdBonus + "%");
                    packMeshCombatData.put("bonusONE" + ship.getId(), false);
                    packMeshCombatData.put("bonusTWO" + ship.getId(), false);

                    if (total_other_friendly_ships >= 4) {
                        packMeshCombatData.put("bonusONE" + ship.getId(), true);
                    }

                    if (total_other_friendly_ships >= 9) {
                        packMeshCombatData.put("bonusTWO" + ship.getId(), true);
                    }
                    
                    if ((boolean) packMeshCombatData.get("bonusONE" + ship.getId())) {
                        // do stuff here with low level upgrade
                        stats.getBeamWeaponFluxCostMult().modifyMult(id, (100 - WEAPON_FLUX_MULT) / 100);
                        stats.getBallisticWeaponFluxCostMod().modifyMult(id, (100 - WEAPON_FLUX_MULT) / 100);
                        stats.getEnergyWeaponFluxCostMod().modifyMult(id, (100 - WEAPON_FLUX_MULT) / 100);
                    } else {
                        stats.getBeamWeaponFluxCostMult().unmodify(id);
                        stats.getBallisticWeaponFluxCostMod().unmodify(id);
                        stats.getEnergyWeaponFluxCostMod().unmodify(id);
                    }
                    
                    if ((boolean) packMeshCombatData.get("bonusTWO" + ship.getId())) {
                        // do stuff here with high level upgrade
                        stats.getFluxCapacity().modifyMult(id, (100 + SHIP_FLUX_MULT) / 100);
                        stats.getFluxDissipation().modifyMult(id, (100 + SHIP_FLUX_MULT) / 100);
                    } else {
                        stats.getFluxCapacity().unmodify(id);
                        stats.getFluxDissipation().unmodify(id);
                    }
                    
                    
                    stats.getMaxSpeed().modifyMult(id, TOTAL_SPEED_BOOST / 100);
                    stats.getMaxTurnRate().modifyMult(id, TOTAL_SPEED_BOOST / 100);
                    stats.getTurnAcceleration().modifyMult(id, TOTAL_SPEED_BOOST / 100);
                    stats.getAcceleration().modifyMult(id, TOTAL_SPEED_BOOST / 100);
                    stats.getDeceleration().modifyMult(id, TOTAL_SPEED_BOOST / 100);
                    stats.getBallisticWeaponRangeBonus().modifyPercent(id, range_bonus * RANGE_BONUS_MULT);
                    stats.getBeamWeaponRangeBonus().modifyPercent(id, range_bonus * RANGE_BONUS_MULT);
                    stats.getEnergyWeaponRangeBonus().modifyPercent(id, range_bonus * RANGE_BONUS_MULT);
//                    stats.getBeamWeaponDamageMult().modifyMult(id, TOTAL_DAMAGE_MULT / 100);
//                    stats.getBallisticWeaponDamageMult().modifyMult(id, TOTAL_DAMAGE_MULT / 100);
//                    stats.getEnergyWeaponDamageMult().modifyMult(id, TOTAL_DAMAGE_MULT / 100);
                    stats.getShieldDamageTakenMult().modifyMult(id, TOTAL_DAMAGE_BOOST / 100);

                    } else {
                        packMeshCombatData.put("meshstate" + ship.getId(), "initialise");
                        packMeshCombatData.put("reportText" + ship.getId(),"Initialising ...");
                    }

                    packMeshCombatData.put("timestamp_minor" + ship.getId(), 0f);

                }

            boolean thisMeshActive = (boolean) packMeshCombatData.get("meshActive" + ship.getId());
            
            if (thisMeshActive && ship == Global.getCombatEngine().getPlayerShip() ) {
                Global.getCombatEngine().maintainStatusForPlayerShip(PM_ID, PM_ICON, PM_NAME, (String) packMeshCombatData.get("reportText" + ship.getId()), false);
            }

            if (ship == Global.getCombatEngine().getPlayerShip() && (boolean) packMeshCombatData.get("bonusONE" + ship.getId())) {
                Global.getCombatEngine().maintainStatusForPlayerShip(PM_ID5, PM_ICON, PM_NAME + " (5 Nodes)", "weapon flux cost -" + (int) WEAPON_FLUX_MULT + "%", false);
            }

            if (ship == Global.getCombatEngine().getPlayerShip() && (boolean) packMeshCombatData.get("bonusTWO" + ship.getId())) {
                Global.getCombatEngine().maintainStatusForPlayerShip(PM_ID10, PM_ICON, PM_NAME + " (10 Nodes)", "flux capacity & dissipation +" + (int) SHIP_FLUX_MULT + "%", false);
            }
//        	void maintainStatusForPlayerShip(Object key, String spriteName, String title, String data, boolean isDebuff);
        }
    
//    @Override
//    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
//        for (String tmp : BLOCKED_HULLMODS) {
//            if (ship.getVariant().getHullMods().contains(tmp)) {
//                ship.getVariant().removeMod(tmp);
//            }
//        }
//    }
        
    public static int countFriendlyShips(ShipAPI ship) {
    
        int num_friendly = 0;
                    for (ShipAPI bote : Global.getCombatEngine().getShips()) {
                    if (bote.isAlive() && bote.getOwner() == ship.getOwner() && bote.getId() != ship.getId()) {
                        num_friendly += 1;
                    }
                }
        return num_friendly;   
    }
    
    public static void turnOffBonus(ShipAPI ship) {
                MutableShipStatsAPI stats = ship.getMutableStats();
                String id = ship.getId();
                
                stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getBallisticWeaponRangeBonus().unmodify(id);
		stats.getBeamWeaponRangeBonus().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		stats.getBeamWeaponDamageMult().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getBeamWeaponFluxCostMult().unmodify(id);
		stats.getBallisticWeaponFluxCostMod().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
		stats.getFluxCapacity().unmodify(id);
		stats.getFluxDissipation().unmodify(id);
    }
    
//    public static void applyShipBonuses(ShipAPI ship) {
//                MutableShipStatsAPI stats = ship.getMutableStats();
//                String id = ship.getId();
//                
//                Map<String, Object> packMeshCombatData = Global.getCombatEngine().getCustomData();
//                
//                float TOTAL_SPEED_BOOST = ( 100f + ( speed_bonus * SPEED_BONUS_MULT ) ) / 100;
//                float TOTAL_RANGE_BOOST = ( 100f + ( range_bonus * RANGE_BONUS_MULT ) ) / 100;
//                float TOTAL_DAMAGE_BOOST = ( 100f + ( damage_bonus * DAMAGE_BONUS_MULT ) ) / 100;
//                
//                float TOTAL_SPEED_DROP = ( 100f + ( speed_malus * SPEED_MALUS_MULT ) ) / 100;
//                float TOTAL_RANGE_DROP = ( 100f + ( range_malus * RANGE_MALUS_MULT ) ) / 100;
//                float TOTAL_DAMAGE_DROP = ( 100f + ( damage_malus * DAMAGE_MALUS_MULT ) ) / 100;
//                
//                float TOTAL_SPEED_MULT = TOTAL_SPEED_BOOST - TOTAL_SPEED_DROP + 1;
//                float TOTAL_RANGE_MULT = TOTAL_RANGE_BOOST - TOTAL_RANGE_DROP + 1;
//                float TOTAL_DAMAGE_MULT = TOTAL_DAMAGE_BOOST - TOTAL_DAMAGE_DROP + 1;
//                
//                packMeshCombatData.put("reportText" + ship.getId(), "SPD:" + (int) ( TOTAL_SPEED_MULT * 100f ) + " RNG:" + (int) ( TOTAL_RANGE_MULT * 100f ) + " DMG:" + (int) ( TOTAL_DAMAGE_MULT * 100f ));
//                
//                stats.getMaxSpeed().modifyMult(id, TOTAL_SPEED_MULT);
//		stats.getMaxTurnRate().modifyMult(id, TOTAL_SPEED_MULT);
//		stats.getTurnAcceleration().modifyMult(id, TOTAL_SPEED_MULT);
//		stats.getAcceleration().modifyMult(id, TOTAL_SPEED_MULT);
//		stats.getDeceleration().modifyMult(id, TOTAL_SPEED_MULT);
//		stats.getBallisticWeaponRangeBonus().modifyMult(id, TOTAL_RANGE_MULT);
//		stats.getBeamWeaponRangeBonus().modifyMult(id, TOTAL_RANGE_MULT);
//		stats.getBeamWeaponDamageMult().modifyMult(id, TOTAL_DAMAGE_MULT);
//		stats.getBallisticWeaponDamageMult().modifyMult(id, TOTAL_DAMAGE_MULT);
//    }
    
}
