/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author paul
 */
public class JunkPiratesLostTechSalvageGen extends BaseCommandPlugin {
    
//public static class SDMParams {
//		public SectorEntityToken entity;
//		public String factionId;
//		public SDMParams() {
//		}
//	}
	
	
//	public static interface SalvageDefenderModificationPlugin extends GenericPluginManagerAPI.GenericPlugin {
//		float getStrength(SDMParams p, float strength, Random random, boolean withOverride);
//		float getProbability(SDMParams p, float probability, Random random, boolean withOverride);
//		float getQuality(SDMParams p, float quality, Random random, boolean withOverride);
//		float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride);
//		
//		void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride);
//		void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet);
//	}
	
//	public static class SalvageDefenderModificationPluginImpl extends BaseGenericPlugin implements SalvageDefenderModificationPlugin {
//		public float getStrength(SDMParams p, float strength, Random random, boolean withOverride) {
//			if (withOverride) return strength;
//			float bonus = Global.getSector().getMemoryWithoutUpdate().getFloat(DEFEATED_DERELICT_STR) * DEFEATED_TO_ADDED_FACTOR;
//			
//			String type = p.entity.getCustomEntityType();
//			float limit = 300f;
//			if (Entities.DERELICT_SURVEY_PROBE.equals(type)) {
//				limit = 60;
//			} else if (Entities.DERELICT_SURVEY_SHIP.equals(type)) {
//				limit = 90;
//			} else if (Entities.DERELICT_MOTHERSHIP.equals(type) || Entities.DERELICT_CRYOSLEEPER.equals(type)) {
//				limit = 150;
//			}
//			
////			if (Global.getSettings().isDevMode()) {
////				bonus = limit;
////			}
//			
//			if (bonus > limit) bonus = limit;
//			return strength + (int) bonus;
//		}
////		public float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride) {
////			if (withOverride) return maxSize;
////			
////			float bonus = Global.getSector().getMemoryWithoutUpdate().getFloat(DEFEATED_DERELICT_STR) * DEFEATED_TO_ADDED_FACTOR;
////			String type = p.entity.getCustomEntityType();
////			float bonusSize = 1;
////			if (Entities.DERELICT_SURVEY_PROBE.equals(type)) {
////				if (bonus >= 5) bonusSize = 2;
////			}
////			
////			return Math.max(maxSize, bonusSize);
////		}
//		public float getProbability(SDMParams p, float probability, Random random, boolean withOverride) {
//			if (withOverride) return probability;
//			return probability;
//		}
////		public void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
////			float total = Global.getSector().getMemoryWithoutUpdate().getFloat(DEFEATED_DERELICT_STR);
////			for (FleetMemberAPI member : Misc.getSnapshotMembersLost(fleet)) {
////				//total += FleetFactoryV2.getMemberWeight(member);
////				total += member.getFleetPointCost();
////			}
////			Global.getSector().getMemoryWithoutUpdate().set(DEFEATED_DERELICT_STR, total);
////		}
//		public void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
//			if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(MiscellaneousThemeGenerator.PLANETARY_SHIELD_PLANET)) {
//				FleetMemberAPI flagship = null;
//				for (ShipRolePick pick : fleet.getFaction().pickShip(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), null, random)) {
//					FleetMemberAPI member = fleet.getFleetData().addFleetMember(pick.variantId);
//					flagship = member;
//					// the name is used as part of the random seed for autofit, so, want it to be consistent
//					member.setShipName(fleet.getFaction().pickRandomShipName(random));
//				}
//				for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
//					member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
//					member.setFlagship(member == flagship);
//					
//					PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, OfficerManagerEvent.SkillPickPreference.NON_CARRIER, random);
//					member.setCaptain(person);
//				}
//				
//				PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, OfficerManagerEvent.SkillPickPreference.NON_CARRIER, random);
//				fleet.setCommander(person);
//				fleet.getFlagship().setCaptain(person);
//				
//				if (fleet.getInflater() instanceof DefaultFleetInflater) {
//					DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
//					((DefaultFleetInflaterParams)dfi.getParams()).allWeapons = true;
//					//dfi.setSeed(Misc.random.nextLong());
//				}
//			} else
//			if (Entities.DERELICT_CRYOSLEEPER.equals(p.entity.getCustomEntityType())) {
//				fleet.getFleetData().clear();
//				for (ShipRolePick pick : fleet.getFaction().pickShip(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), null, random)) {
//					fleet.getFleetData().addFleetMember(pick.variantId);
//				}
//				for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
//					member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
//				}
//				
//				PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, OfficerManagerEvent.SkillPickPreference.NON_CARRIER, random);
//				fleet.setCommander(person);
//				fleet.getFlagship().setCaptain(person);
//				
//			}
//		}
//		@Override
//		public int getHandlingPriority(Object params) {
//			if (!(params instanceof SDMParams)) return 0;
//			SDMParams p = (SDMParams) params;
//			
//			if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(MiscellaneousThemeGenerator.PLANETARY_SHIELD_PLANET)) {
//				return 1;
//			}
//			if (Factions.DERELICT.equals(p.factionId)) {
//				return 1;
//			}
//			
//			return 0;
//		}
//		public float getQuality(SDMParams p, float quality, Random random, boolean withOverride) {
//			if (withOverride) return quality;
//			float bonus = Global.getSector().getMemoryWithoutUpdate().getFloat(DEFEATED_DERELICT_STR) * DEFEATED_TO_QUALITY_FACTOR;
//			return quality + bonus;
//			
//		}
//	}
	
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		
		SectorEntityToken entity = dialog.getInteractionTarget();
		String specId = entity.getCustomEntityType();
		if (specId == null || entity.getMemoryWithoutUpdate().contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
			specId = entity.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
		}
		SalvageEntityGenDataSpec spec = SalvageEntityGeneratorOld.getSalvageSpec(specId);
		
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		if (memoryMap.containsKey(MemKeys.ENTITY)) {
			memory = memoryMap.get(MemKeys.ENTITY);
		}
		
		long seed = memory.getLong(MemFlags.SALVAGE_SEED);
		
		Random fleetRandom = Misc.getRandom(seed, 1);
		
		String factionId = (entity.getFaction() != null) ? entity.getFaction().getId() : "junk_pirates_losttech";
		if (spec != null && spec.getDefFaction() != null) {
			factionId = spec.getDefFaction();
		}
		
//		if (plugin != null) {
//			strength = plugin.getStrength(p, strength, random, override != null);
//			prob = plugin.getProbability(p, prob, random, override != null);
//		}
		
//		float probStation = spec.getProbStation();
//		if (override != null) {
//			probStation = override.probStation;
//		}
//		String stationRole = null;
//		if (fleetRandom.nextFloat() < probStation) {
//			stationRole = spec.getStationRole();
//			if (override != null && override.stationRole != null) {
//				stationRole = override.stationRole;
//			}
//		}
		
		//prob = 1f;
		//strength = 0;
		
		
		if (!memory.getBoolean("$defenderFleetDefeated")) {
			memory.set("$hasDefenders", true, 0);
			
			if (!memory.contains("$defenderFleet")) {
				
				FleetParamsV3 fParams = new FleetParamsV3(null, null,
								factionId,
								1.0f,
								FleetTypes.PATROL_SMALL,
								(int) 1.0f,
								0, 0, 0, 0, 0, 0);
				
				fParams.random = fleetRandom;
				fParams.withOfficers = false;
				fParams.maxShipSize = (spec != null) ? (int) spec.getMaxDefenderSize() : 4;
//				if (override != null) {
//					fParams.maxShipSize = override.maxDefenderSize;
//				}
//				if (plugin != null) {
//					fParams.maxShipSize = (int) (plugin.getMaxSize(p, fParams.maxShipSize, random, override != null));
//				}
				
				//fParams.allowEmptyFleet = true;
				
				CampaignFleetAPI defenders = FleetFactoryV3.createFleet(fParams);
                                defenders.getFleetData().clear();
                                defenders.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, ShipPickParams.all(), defenders);
                                defenders.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, ShipPickParams.all(), defenders);
                                
                                //memory.set("defenderFleet", defenders, 0);
				
				if (!defenders.isEmpty()) {
//					defenders.getInflater().setRemoveAfterInflating(false);
					
					//defenders.setName(entity.getName() + ": " + "Automated Defenses");
					defenders.setName("Automata Cloud");
	
//					if (stationRole != null) {
//						defenders.getFaction().pickShipAndAddToFleet(stationRole, FactionAPI.ShipPickParams.all(), defenders, fleetRandom);
//						defenders.getFleetData().sort();
//					}
					
//					defenders.clearAbilities();
//					
//					if (plugin != null) {
//						//System.out.println("NEXT: " + fleetRandom.nextLong());
//						plugin.modifyFleet(p, defenders, fleetRandom, override != null);
//					}
					
					defenders.getFleetData().sort();
					
					memory.set("$defenderFleet", defenders, 0);
					
				} else {
					memory.set("$hasDefenders", false, 0);
				}
			}
			
			
			CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
			if (defenders != null) {
				boolean hasStation = false;
				boolean hasNonStation = false;
				for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
					if (member.isStation()) {
						hasStation = true;
					} else {
						hasNonStation = true;
					}
				}
				memory.set("$hasStation", hasStation, 0);
				memory.set("$hasNonStation", hasNonStation, 0);
				
				defenders.setLocation(entity.getLocation().x, entity.getLocation().y);
			}
		} else {
			memory.set("$hasDefenders", false, 0);
			memory.set("$hasStation", false, 0);
		}
		
		//memory.set("hasSalvageSpecial", false, 0);
		//memory.set("salvageSpecialData", null, 0);
		//memory.set("salvageSpecialData", new DomainSurveyDerelictSpecialData(SpecialType.SCRAMBLED), 0);
		
	
		return true;
	}

	    
}
