/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.procgen.themes;

import data.scripts.campaign.ids.JunkPiratesEntities;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.NameAssigner;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.procgen.themes.ThemeGenContext;
//import com.fs.starfarer.api.impl.campaign.procgen.themes.Themes;
import data.scripts.campaign.ids.JunkPiratesTags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.utilities.JunkPiratesConfig.loadJunkPiratesModConfig;
import static data.scripts.utilities.JunkPiratesConfig.getSpineretteSystemBlacklist;
import static data.scripts.utilities.JunkPiratesConfig.getSpineretteSystemWhitelist;
import static data.scripts.utilities.JunkPiratesConfig.getSpineretteTagBlacklist;
import static data.scripts.utilities.JunkPiratesConfig.getSpineretteTagWhitelist;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author paul
 */
public class JunkPiratesAnarchistThemeGenerator extends BaseThemeGenerator {

        private int minAnarchistConstellations = 3;
        private int maxAnarchistConstellations = 6;
        private float skipProbability = 0.9f;

        private int softMaxSpinerettes = 3;

        private boolean enableProcGen = true;
        private boolean enableSpinerettes = true;
    
        
    
	public static enum AnarchistSystemType {
		JUNKBOYS(JunkPiratesTags.THEME_JUNK_BOYS, "$junkJunkBoys"),
		HOUNDS(JunkPiratesTags.THEME_JUNK_HOUNDS, "$junkHounds"),
		TECHNICIANS(JunkPiratesTags.THEME_JUNK_TECHNICIANS, "$junkTechnicians"),
		;
		
		private String tag;
		private String beaconFlag;
		private AnarchistSystemType(String tag, String beaconFlag) {
			this.tag = tag;
			this.beaconFlag = beaconFlag;
		}
		public String getTag() {
			return tag;
		}
		public String getBeaconFlag() {
			return beaconFlag;
		}
	}
        
	
	public String getThemeId() {
		return JunkPiratesThemes.ANARCHISTS;
	}

	@Override
	public void generateForSector(ThemeGenContext context, float allowedUnusedFraction) {

        loadJunkPiratesModConfig(); // this loads white / blacklists if present
            
        try {
            JSONObject procGenSettings = Global.getSettings().loadJSON("mendoncaModSettings.json");

                enableProcGen = procGenSettings.getBoolean("enableProcGen");
                enableSpinerettes = procGenSettings.getBoolean("enableSpinerettes");

                minAnarchistConstellations = procGenSettings.getInt("minAnarchistConstellations");
                maxAnarchistConstellations = procGenSettings.getInt("maxAnarchistConstellations");
                skipProbability = (float)procGenSettings.getDouble("skipProbability");
                softMaxSpinerettes = procGenSettings.getInt("softMaxSpinerettes");

            } catch (IOException | JSONException ex) {
                System.out.println("JP Config Exception " + ex);
            }
            
            System.out.println("enableProcGen: " + enableProcGen);
            System.out.println("minAnarchistConstellations: " + minAnarchistConstellations);
//            System.out.println("enablePACK: " + enablePACK);
//            System.out.println("enableJunkPirates: " + enableJunkPirates);
            
            if (enableProcGen) {
		float total = (float) (context.constellations.size() - context.majorThemes.size()) * allowedUnusedFraction;
		// total provides a number of constellations available; based on the total in the sector, less the ones with majorThemes set, and then we only look at a certain fraction of these.
		if (DEBUG) {System.out.println("Found " + total + " constellations for potential Anarchy");}
                
                if (total <= 0) return;
		
		int num = (int) StarSystemGenerator.getNormalRandom(minAnarchistConstellations, maxAnarchistConstellations);
		//Run the starsystemgenerator for a random amount of constellations; based on the settings we have defined elsewhere.
		
                if (DEBUG) {System.out.println("We are trying to find homes in " + num + " constellations");}
                
		if (num > total) num = (int) total;
		// Make sure we don't run it any more than the number of constellations we have available - as set out above in variables
		
		
		int numDestroyed = (int) (num * (0.23f + 0.1f * random.nextFloat()));
		if (numDestroyed < 1) numDestroyed = 1;
		// Generate a random number for 'Tier 1' low danger constellations
		int numSuppressed = (int) (num * (0.23f + 0.1f * random.nextFloat()));
		if (numSuppressed < 1) numSuppressed = 1;
		// generate a random number for 'Tier 2' danger constellations
		
		float suppressedStationMult = 0.5f;
		int suppressedStations = (int) Math.ceil(numSuppressed * suppressedStationMult);
		// round up the number of suppressed stations (within Tier 2 constellations) up to the nearest whole number, which is half as many as Tier 2 systems - every other Tier 2 constellation COULD contain a suppressed station.
		
		WeightedRandomPicker<Boolean> addSuppressedStation = new WeightedRandomPicker<Boolean>(random);
		for (int i = 0; i < numSuppressed; i++) { //iterate for the number of Tier 2 constellations, decide randomly whether we add a station to this constellation?
			if (i < suppressedStations) {
				addSuppressedStation.add(true, 1f);
			} else {
				addSuppressedStation.add(false, 1f);
			}
		}
		
		List<Constellation> constellations = getSortedAvailableConstellations(context, false, new Vector2f(), null); //sorts constellations from 0,0, false means empty is not ok
		Collections.reverse(constellations); // flip them; put the furthest away candidates at the top of the list. 
		
		float skipProb = skipProbability; // just a mixer-upper; chance of skipping a given constellation, I think, based on number of candidates vs. number of systems we want.
		if (total < num / (1f - skipProb)) {
			skipProb = 1f - (num / total);
		}
//		skipProb = 0f;

		List<StarSystemData> anarchistSystems = new ArrayList<>(); //intialise our list of candidates
		
		if (DEBUG) {System.out.println("\n\n\n");}
		if (DEBUG) {System.out.println("Generating Junk Pirates Anarchist systems");}
                
		@SuppressWarnings("unused")
				int count = 0;
		
		int numUsed = 0;
                
                int tangerine = 0;
                
		for (int i = 0; i < num && i < constellations.size(); i++) {
			Constellation c = constellations.get(i); // we are iterating through the list of constellations, from the furthest first to the nearest last.
			if (numUsed <= minAnarchistConstellations) {
                            skipProb = 0f;
                        } else {
                            skipProb = skipProbability;
                        }
                        
                        if (numUsed > maxAnarchistConstellations || random.nextFloat() < skipProb) {
                                // have a roll against the skip probability.
				if (DEBUG) System.out.println("Skipping constellation " + c.getName());
				continue;
			}
			
			
			List<StarSystemData> systems = new ArrayList<>(); // let us get the list of systems in our first candidate constellation
			for (StarSystemAPI system : c.getSystems()) {
				StarSystemData data = computeSystemData(system);
				systems.add(data); // each 'data' is a system pulled out of the constellation 'c'
			}
			
			List<StarSystemData> mainCandidates = getSortedSystemsSuitedToBePopulated(systems); // do a check on the systems within the constellation, grab the most suitable 'fun' constellations.
			// mainCandidates is a list of systems pulled out of a constellation.
			int numMain = 1 + random.nextInt(2); // We are targeting the population of from 1 to 3 systems in this constellation.
			if (numMain > mainCandidates.size()) numMain = mainCandidates.size(); // if there aren't enough systems in the list; cap it at the number of systems available
			if (numMain <= 0) { // error checking. If no mainCandidates then don't worry about this system.
				if (DEBUG) System.out.println("Skipping constellation " + c.getName() + ", no suitable main candidates");
				continue;
			}
			
			AnarchistSystemType type = AnarchistSystemType.TECHNICIANS; // Default to Technicians. First roll, we are on the ... furthest away constellation in our list.
			if (numUsed < numDestroyed) { // if we are starting out (numUsed = 0) we step in to JUNKBOYS
				type = AnarchistSystemType.JUNKBOYS;
			} else if (numUsed < numDestroyed + numSuppressed) { // again; if numUsed = 0, we are going to roll in to HOUNDS
				type = AnarchistSystemType.HOUNDS;
			}
			
			context.majorThemes.put(c, JunkPiratesThemes.ANARCHISTS); // drop in a majorTheme to the constellation; so that other procgen content knows to stay away.
			numUsed++; //we've found a constellation; numUsed +1

			if (DEBUG) System.out.println("Generating " + numMain + " main systems in " + c.getName());
			for (int j = 0; j < numMain; j++) {

				StarSystemData data = mainCandidates.get(j); //we'll start grabbing the system (indexed) within the constellation to do stuff with
                                if (tangerine < 2) {
                                    data.system.addTag("derelict_Tangerine");
                                    tangerine++ ;
                                } else if (tangerine < 5) {
                                    if ((random.nextInt(100) + 1) < 20) {
                                        data.system.addTag("derelict_Tangerine");
                                        tangerine++ ;
                                    }
                                } else {
                                        if ((random.nextInt(100) + 1) < 5) {
                                        data.system.addTag("derelict_Tangerine");
                                        tangerine++ ;
                                        }
                                }
				populateMain(data, type); // run the populate function, based on the type we have
				
				data.system.addTag(JunkPiratesTags.THEME_ANARCHISTS); // tag up the system
				data.system.addTag(JunkPiratesTags.THEME_ANARCHISTS_MAIN);
				data.system.addTag(type.getTag());
				anarchistSystems.add(data); // add the StarSystemData to our list, a maintained list of candidate systems ...

				if (!NameAssigner.isNameSpecial(data.system)) { // non special names get changed for the system
					NameAssigner.assignSpecialNames(data.system);
				}
				
				
				if (type == AnarchistSystemType.JUNKBOYS) {
					JunkPiratesAnarchistSeededFleetManager fleets = new JunkPiratesAnarchistSeededFleetManager(data.system, 2, 5, 2, 6, 1f);
					data.system.addScript(fleets); // assign a fleetmanager to the system
				} else if (type == AnarchistSystemType.HOUNDS) {
					JunkPiratesAnarchistSeededFleetManager fleets = new JunkPiratesAnarchistSeededFleetManager(data.system, 3, 8, 4, 12, 1f);
					data.system.addScript(fleets); // assign a fleetmanager to the system

					Boolean addStation = random.nextFloat() < suppressedStationMult; // put in a suppressed station, maybe
					if (j == 0 && !addSuppressedStation.isEmpty()) addSuppressedStation.pickAndRemove();
					if (addStation) {
						List<CampaignFleetAPI> stations = addBattlestations(data, 1f, 1, 1, createStringPicker("pack_anarchist_station1_Den", 10f));
						for (CampaignFleetAPI station : stations) {
							int maxFleets = 2 + random.nextInt(2);
							JunkPiratesAnarchistStationFleetManager activeFleets = new JunkPiratesAnarchistStationFleetManager(
									station, 1f, 0, maxFleets, 20f, 4, 10);
							data.system.addScript(activeFleets);
						}
						
					}
				} else if (type == AnarchistSystemType.TECHNICIANS) {
					List<CampaignFleetAPI> stations = addBattlestations(data, 1f, 1, 1, createStringPicker("pack_anarchist_station2_Camp", 10f));
					for (CampaignFleetAPI station : stations) {
						int maxFleets = 4 + random.nextInt(4);
						JunkPiratesAnarchistStationFleetManager activeFleets = new JunkPiratesAnarchistStationFleetManager(
								station, 1f, 0, maxFleets, 10f, 4, 14);
						data.system.addScript(activeFleets); // assign a fleetmanager and stick in a full-fat station
					}
				}
			}
			
			for (StarSystemData data : systems) { // then for every system, do something with it
				int index = mainCandidates.indexOf(data);
				if (index >= 0 && index < numMain) continue; // step out if we're in a MAIN system.
                                if ((random.nextInt(100) + 1) < 5) {
                                    data.system.addTag("derelict_Tangerine");
                                    tangerine++ ;
                                }
				populateNonMain(data); // do something with the systems which aren't 'MAIN' systems. i.e. if a constellation has 12 systems; 3 will be main and 9 will be non-main
				
				data.system.addTag(JunkPiratesTags.THEME_ANARCHISTS);
				data.system.addTag(JunkPiratesTags.THEME_ANARCHISTS_NONMAIN);
				data.system.addTag(type.getTag());
				anarchistSystems.add(data); // tag up, fill up the list of systems StarSystemData to allow us to be able to refer back to anarchist systems
				
		//		if (random.nextFloat() < 0.5f) {
				JunkPiratesAnarchistSeededFleetManager fleets = new JunkPiratesAnarchistSeededFleetManager(data.system, 1, 3, 1, 2, 0.05f);
				data.system.addScript(fleets); // I'm adding a small fleet manager to every non-main system. Vanilla has a 50/50 chance in remnants case.
		//		} else {
		//			data.system.addTag(Tags.THEME_REMNANT_NO_FLEETS);
		//		}
			}
                        
//			if (count == 18) {
//				System.out.println("REM RANDOM INDEX " + count + ": " + random.nextLong());
//			}
			count++;
		}
                
//		for (int i = 0; i < num && i < constellations.size(); i++) { // let's have another look and plonk some Spinerette's around the place
//			Constellation c = constellations.get(i); // we are iterating through the list of constellations, from the furthest first to the nearest last.                
//                
//                // start by checking out the constellations for shit we need
//                    List<StarSystemData> systems = new ArrayList<>(); // let us get the list of systems in our first candidate constellation
//                    for (StarSystemAPI system : c.getSystems()) {
//                            StarSystemData data = computeSystemData(system);
//                            systems.add(data); // each 'data' is a system pulled out of the constellation 'c'
//                    }
//                    
//                    // we should find cryovolcanic worlds with ultrarich ores or rare ores
//                    // So grab a list of planets in the system
//                    // if there is cryovolcanic check for resources
//                    // if we've got them then chuck a spinerette in orbit
//                    // add to the spinerette counter until we've achieved max number
//                    // then add a hazard rating to the planet; associate with the spinerette in orbit
//                    // we could also add hab tubes
//                    // maybe we need some script sitting on the planet telling it there is a live spinerette
//                    
//                }
                
                System.out.println("Created" + tangerine + " Tangerines\n");
		
		SalvageSpecialAssigner.SpecialCreationContext specialContext = new SalvageSpecialAssigner.SpecialCreationContext();
		specialContext.themeId = getThemeId();
		SalvageSpecialAssigner.assignSpecials(anarchistSystems, specialContext); // stick special salvage in 'all' anarchist systems based on the rules of that function
		
		addDefenders(anarchistSystems); // addDefenders to anarchist systems (not relevant for me, as prob = 0f, but function retained)
		
                if (enableSpinerettes) {
                    addSpinerettes(context);
                }
                
		if (DEBUG) System.out.println("Finished generating Anarchist systems\n\n\n\n\n");
            }	
	}
	
        public void addSpinerettes(ThemeGenContext context) {
            
            int spineret = 0;

            
            

            List<Constellation> constellations = getSortedAvailableConstellations(context, false, new Vector2f(), null); //sorts constellations from 0,0, false means empty is not ok
            Collections.reverse(constellations); // flip them; put the furthest away candidates at the top of the list. 
            
            
            for (int i = 0; i < constellations.size(); i++) {
                Constellation c = constellations.get(i);
            
            List<StarSystemData> systems = new ArrayList<>(); // let us get the list of systems in our first candidate constellation
            for (StarSystemAPI system : c.getSystems()) {
                StarSystemData data = computeSystemData(system);
                systems.add(data); // each 'data' is a system data pulled out of the constellation 'c'
            }
            
            
            
            for (StarSystemData data : systems) {
                for (PlanetAPI planet : data.system.getPlanets()) {
                    //if (spineret >= MAX_SPINERETTES) continue;
                    if (checkSystemsForSuitability(data)) {
                        if ("cryovolcanic".equals(planet.getTypeId())) {
                            if (planet.getMarket() != null && (planet.getMarket().hasCondition(Conditions.RARE_ORE_RICH) || planet.getMarket().hasCondition(Conditions.RARE_ORE_ULTRARICH))) {
                                if (spineret < 1) {
                                    data.system.addTag("junk_pirates_Spinerette");
                                    spineret++ ;
                                    addSpinerette(data, planet);
                                    if (planet != null && planet.getMarket() != null) {
                                        planet.getMarket().addCondition("JUNK_habTubes_active");
                                    }
                                } else if (spineret < softMaxSpinerettes) {
                                        if ((random.nextInt(100) + 1) < 50) {
                                        data.system.addTag("junk_pirates_Spinerette");
                                        spineret++ ;
                                        addSpinerette(data, planet);
                                        if (planet != null && planet.getMarket() != null) {
                                            planet.getMarket().addCondition("JUNK_habTubes_active");
                                        }
                                    }
                                } else {
                                        if ((random.nextInt(100) + 1) < 5) {
                                        data.system.addTag("junk_pirates_Spinerette");
                                        spineret++ ;
                                        addSpinerette(data, planet);
                                        if (planet != null && planet.getMarket() != null) {
                                            planet.getMarket().addCondition("JUNK_habTubes_active");
                                        }
                                        }
                                    }  
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public void addSpinerette(StarSystemData data, PlanetAPI planet) {
            
            EntityLocation loc = createLocationAtRandomGap(random, planet, 100f);
            
            addEntity(random, data.system, loc, "junk_pirates_spinerette_active", "junk_pirates_losttech");

//                                FleetParamsV3 fParams = new FleetParamsV3(null, null,
//                                                        "junk_pirates_losttech",
//                                                        1.0f,
//                                                        FleetTypes.PATROL_SMALL,
//                                                        0f,
//                                                        0, 0, 0, 0, 0, 0);
//            
//            			CampaignFleetAPI defenders = FleetFactoryV3.createFleet(fParams);
//				
//                                //float pickShipAndAddToFleet(String role, FactionAPI.ShipPickParams params, CampaignFleetAPI fleet, Random random);
//                                defenders.getFleetData().clear();
//                                
//                                defenders.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, ShipPickParams.all(), defenders);
//                                PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, OfficerManagerEvent.SkillPickPreference.NON_CARRIER, random);
//				member.setCaptain(person);
//                                defenders.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, ShipPickParams.all(), defenders);
//                                
//
//            
//            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(random, 0, 0, false, null, null);
//            Object specialData = creator.createSpecial(spinner.entity, new SalvageSpecialAssigner.SpecialCreationContext());
//            if (specialData != null) {
//                    Misc.setSalvageSpecial(ae.entity, specialData);
//            }

// Use a similar salvagegenfromseed via rules and interaction dialogs, memory etc
            
        }
	
	public void addDefenders(List<StarSystemData> systemData) { // should try and get this working so not remnants ... ?
		for (StarSystemData data : systemData) {
//			float prob = 0.1f;
//			float max = 3f;
//			if (data.system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {
//				prob = 0.05f;
//				max = 1f;
//			}
			float mult = 1f;
			if (data.system.hasTag(JunkPiratesTags.THEME_ANARCHISTS_NONMAIN)) {
				mult = 0.5f;
			}
			
                        String anarchist_faction = "pack";
                        if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_BOYS)) {
                            anarchist_faction = JunkPiratesThemes.JUNKBOYS;
                        } else if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_HOUNDS)) {
                            anarchist_faction = JunkPiratesThemes.HOUNDS;
                        } else if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_TECHNICIANS)) {
                            anarchist_faction = JunkPiratesThemes.TECHNICIANS;
                        }
                        
			for (AddedEntity added : data.generated) {
				if (added.entityType == null) continue;
				if (Entities.WRECK.equals(added.entityType)) continue;
				
				float prob = 0f;
				float min = 1f;
				float max = 1f;
				if (JunkPiratesEntities.STATION_MINING_ANARCHISTS.equals(added.entityType)) {
					prob = 0.00f;
					min = 8;
					max = 15;
				} else if (JunkPiratesEntities.ORBITAL_HABITAT_ANARCHISTS.equals(added.entityType)) {
					prob = 0.00f;
					min = 8;
					max = 15;
				} else if (JunkPiratesEntities.STATION_RESEARCH_ANARCHISTS.equals(added.entityType)) {
					prob = 0.00f;
					min = 10;
					max = 20;
				}
				
				prob *= mult;
				min *= mult;
				max *= mult;
				if (min < 1) min = 1;
				if (max < 1) max = 1;
				
				if (random.nextFloat() < prob) {
					Misc.setDefenderOverride(added.entity, new DefenderDataOverride(anarchist_faction, 1f, min, max, 4));
				}
				//Misc.setDefenderOverride(added.entity, new DefenderDataOverride(Factions.REMNANTS, prob, 1, max));
			}
		}
		
	}
	
	public void populateNonMain(StarSystemData data) {
		if (DEBUG) System.out.println(" Generating secondary Anarchist system in " + data.system.getName());
		boolean special = data.isBlackHole() || data.isNebula() || data.isPulsar();
		if (special) {
			addResearchStations(data, 0.75f, 1, 1, createStringPicker(JunkPiratesEntities.STATION_RESEARCH_ANARCHISTS, 10f));
		}
		
		if (random.nextFloat() < 0.5f) return;
		
                if (data.system.hasTag("derelict_Tangerine")) {
                    addTangerine(data);
                }
                
		if (!data.resourceRich.isEmpty()) {
			addMiningStations(data, 0.5f, 1, 1, createStringPicker(JunkPiratesEntities.STATION_MINING_ANARCHISTS, 10f));
		}
		
		if (!special && !data.habitable.isEmpty()) {
			// ruins on planet, or orbital station
			addHabCenters(data, 0.25f, 1, 1, createStringPicker(JunkPiratesEntities.ORBITAL_HABITAT_ANARCHISTS, 10f));
		}
		
		
		addShipGraveyard(data, 0.05f, 1, 1,
				createStringPicker("junk_pirates", 10f, "junk_pirates_junkboys", 7f, "junk_pirates_technicians", 3f));
		
		//addDebrisFields(data, 0.25f, 1, 2, Factions.REMNANTS, 0.1f, 1, 1);
		addDebrisFields(data, 0.25f, 1, 2);

		addDerelictShips(data, 0.5f, 0, 3, 
				createStringPicker("junk_pirates", 10f, "junk_pirates_junkboys", 7f, "junk_pirates_technicians", 3f));
		
		addCaches(data, 0.25f, 0, 2, createStringPicker( 
				JunkPiratesEntities.WEAPONS_CACHE_ANARCHISTS, 4f,
				JunkPiratesEntities.WEAPONS_CACHE_SMALL_ANARCHISTS, 10f,
				Entities.SUPPLY_CACHE, 4f,
				Entities.SUPPLY_CACHE_SMALL, 10f,
				Entities.EQUIPMENT_CACHE, 4f,
				Entities.EQUIPMENT_CACHE_SMALL, 10f
				));
		
	}
        

        
        public boolean checkListTags(List<String> list, StarSystemData data) {
            if (list == null) return false;
            for (String tag : list) {
                if (data.system.hasTag(tag)) {
                    return true;
                }
            }
            return false;
        }
        
        public boolean checkSystemsForSuitability(StarSystemData data) {
            
            boolean systemcheck = false; // default to false so as not to muck about with other mods unneccesarrily
            List<String> systemBlacklist = getSpineretteSystemBlacklist();
            List<String> tagBlacklist = getSpineretteTagBlacklist();
            List<String> systemWhitelist = getSpineretteSystemWhitelist();
            List<String> tagWhitelist = getSpineretteTagWhitelist();

            if (systemBlacklist.contains(data.system.getName()) || checkListTags(tagBlacklist, data)) {
                    systemcheck = false; // stay false if we are blacklisted - which then skips any other tags etc. as below
            } else {
                if ( //but if we meet any of these (including empty tags) then we can populate in here
                    data.system.hasTag(JunkPiratesTags.THEME_ANARCHISTS_NONMAIN) ||
                    data.system.hasTag(JunkPiratesTags.THEME_JUNK_BOYS) ||
                    data.system.hasTag(JunkPiratesTags.THEME_JUNK_HOUNDS) ||
                    data.system.hasTag(JunkPiratesTags.THEME_JUNK_TECHNICIANS) ||
                    data.system.hasTag(JunkPiratesTags.THEME_ANARCHISTS_MAIN) ||
                    data.system.hasTag(Tags.THEME_RUINS) ||
                    data.system.hasTag(Tags.THEME_DERELICT) ||
                    data.system.hasTag(Tags.THEME_REMNANT) ||
                    data.system.getTags().isEmpty() ||
                    data.system.hasTag(Tags.THEME_MISC) ||
                    systemWhitelist.contains(data.system.getName()) ||
                    checkListTags(tagWhitelist, data)
                    ) {
                systemcheck = true;
            }
            
            if (data.isBlackHole() || data.isPulsar()) {
                // might consider externalising this?
                systemcheck = false;
                }
            }
            return systemcheck;
            
            
        }
	
        public void addTangerine(StarSystemData data) {
            EntityLocation loc = pickAnyLocation(random, data.system, 70f, null);
            
            AddedEntity ae = addDerelictShip(data, loc, "junk_pirates_tangerine_Hull");
            
            if (ae == null || ae.entity == null) return;

            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(random, 0, 0, false, null, null);
            Object specialData = creator.createSpecial(ae.entity, new SalvageSpecialAssigner.SpecialCreationContext());
            if (specialData != null) {
                    Misc.setSalvageSpecial(ae.entity, specialData);
            }
            
//            if (loc != null) {
//			if (loc.orbit != null) {
//				ship.setOrbit(loc.orbit);
//				loc.orbit.setEntity(ship);
//			} else {
//				ship.setOrbit(null);
//				ship.getLocation().set(loc.location);
//			}
//            
//            }
        }
	
	public void populateMain(StarSystemData data, AnarchistSystemType type) {
		
		if (DEBUG) System.out.println(" Generating Pre Pack Anarchists in " + data.system.getName());
		
		StarSystemAPI system = data.system;
		
		if (type == AnarchistSystemType.TECHNICIANS) {
			addBeacon(system, type);
		}
                
                if (system.hasTag("derelict_Tangerine")) {
                    addTangerine(data);
                }
		
		if (DEBUG) System.out.println("    Added warning beacon");
		
		int maxHabCenters = 1 + random.nextInt(3);
		
		HabitationLevel level = HabitationLevel.LOW;
		if (maxHabCenters == 2) level = HabitationLevel.MEDIUM;
		if (maxHabCenters >= 3) level = HabitationLevel.HIGH;

		addHabCenters(data, 1, maxHabCenters, maxHabCenters, createStringPicker(JunkPiratesEntities.ORBITAL_HABITAT_ANARCHISTS, 10f));
		
		// add various stations, orbiting entities, etc
		@SuppressWarnings("unused")
				float probGate = 1f;
		float probRelay = 1f;
		float probMining = 0.8f;
		float probResearch = 0.05f;
		
		switch (level) {
		case HIGH:
			probGate = 0.5f;
			probRelay = 1f;
			break;
		case MEDIUM:
			probGate = 0.3f;
			probRelay = 0.75f;
			break;
		case LOW:
			probGate = 0.1f;
			probRelay = 0.5f;
			break;
		}
		
		//addCommRelay(data, probRelay);
		addObjectives(data, probRelay);
		//addInactiveGate(data, probGate, 0.5f, 0.5f, 
		//		createStringPicker(Factions.TRITACHYON, 10f, Factions.HEGEMONY, 7f, Factions.INDEPENDENT, 3f));
		
		addShipGraveyard(data, 0.5f, 1, 1,
				createStringPicker("junk_pirates", 10f, "junk_pirates_junkboys", 7f, "junk_pirates_technicians", 3f));
		
		addMiningStations(data, probMining, 1, 1, createStringPicker(JunkPiratesEntities.STATION_MINING_ANARCHISTS, 10f));
		
		addResearchStations(data, probResearch, 1, 1, createStringPicker(JunkPiratesEntities.STATION_RESEARCH_ANARCHISTS, 10f));
		
		
		//addDebrisFields(data, 0.75f, 1, 5, Factions.REMNANTS, 0.2f, 1, 3);
		addDebrisFields(data, 0.75f, 1, 5);

		
//		MN-6186477243757813340		
//		float test = Misc.getDistance(data.system.getLocation(), new Vector2f(-33500, 9000));
//		if (test < 600) {
//			System.out.println("HERE");
//		}
		
		addDerelictShips(data, 0.75f, 0, 7, 
				createStringPicker("junk_pirates", 10f, "junk_pirates_junkboys", 7f, "junk_pirates_technicians", 3f));
		
		addCaches(data, 0.75f, 0, 3, createStringPicker( 
				JunkPiratesEntities.WEAPONS_CACHE_ANARCHISTS, 10f,
				JunkPiratesEntities.WEAPONS_CACHE_SMALL_ANARCHISTS, 10f,
				Entities.SUPPLY_CACHE, 10f,
				Entities.SUPPLY_CACHE_SMALL, 10f,
				Entities.EQUIPMENT_CACHE, 10f,
				Entities.EQUIPMENT_CACHE_SMALL, 10f
				));
		
	}
	
	
	
	public List<StarSystemData> getSortedSystemsSuitedToBePopulated(List<StarSystemData> systems) {
		List<StarSystemData> result = new ArrayList<StarSystemData>();
		
		for (StarSystemData data : systems) {
			if (data.isBlackHole() || data.isNebula() || data.isPulsar()) continue;
			
			if (data.planets.size() >= 4 || data.habitable.size() >= 1) {
				result.add(data);
				
//				Collections.sort(data.habitable, new Comparator<PlanetAPI>() {
//					public int compare(PlanetAPI o1, PlanetAPI o2) {
//						return (int) Math.signum(o1.getMarket().getHazardValue() - o2.getMarket().getHazardValue());
//					}
//				});
			}
		}
		
		Collections.sort(systems, new Comparator<StarSystemData>() {
			public int compare(StarSystemData o1, StarSystemData o2) {
				float s1 = getMainCenterScore(o1);
				float s2 = getMainCenterScore(o2);
				return (int) Math.signum(s2 - s1);
			}
		});
		
		return result;
	}
	
	public float getMainCenterScore(StarSystemData data) {
		float total = 0f;
		total += data.planets.size() * 1f;
		total += data.habitable.size() * 2f;
		total += data.resourceRich.size() * 0.25f;
		return total;
	}
	
	
	public static CustomCampaignEntityAPI addBeacon(StarSystemAPI system, AnarchistSystemType type) {
		
		SectorEntityToken anchor = system.getHyperspaceAnchor();
		if (anchor == null) return null;
		@SuppressWarnings("unchecked")
		List<SectorEntityToken> points = Global.getSector().getHyperspace().getEntities(JumpPointAPI.class);
		
		float minRange = 600;
		
		float closestRange = Float.MAX_VALUE;
		JumpPointAPI closestPoint = null;
		for (SectorEntityToken entity : points) {
			JumpPointAPI point = (JumpPointAPI) entity;
			
			if (point.getDestinations().isEmpty()) continue;
			
			JumpPointAPI.JumpDestination dest = point.getDestinations().get(0);
			if (dest.getDestination().getContainingLocation() != system) continue;
			
			float dist = Misc.getDistance(anchor.getLocation(), point.getLocation());
			if (dist < minRange + point.getRadius()) continue;
			
			if (dist < closestRange) {
				closestPoint = point;
				closestRange = dist;
			}
		}
		
		CustomCampaignEntityAPI beacon = Global.getSector().getHyperspace().addCustomEntity(null, null, "junk_pirates_warning_beacon", Factions.NEUTRAL);
		//beacon.getMemoryWithoutUpdate().set("remnant", true);
		beacon.getMemoryWithoutUpdate().set(type.getBeaconFlag(), true);
		
		switch (type) {
		case JUNKBOYS: beacon.addTag("junk_pirates_prepack_beacon"); break;
		case HOUNDS: beacon.addTag("junk_pirates_prepack_beacon"); break;
		case TECHNICIANS: beacon.addTag("junk_pirates_prepack_beacon"); break;
		}
		
		if (closestPoint == null) {
			float orbitDays = minRange / (10f + StarSystemGenerator.random.nextFloat() * 5f);
			//beacon.setCircularOrbit(anchor, StarSystemGenerator.random.nextFloat() * 360f, minRange, orbitDays);
			beacon.setCircularOrbitPointingDown(anchor, StarSystemGenerator.random.nextFloat() * 360f, minRange, orbitDays);
		} else {
			float angleOffset = 20f + StarSystemGenerator.random.nextFloat() * 20f;
			float angle = Misc.getAngleInDegrees(anchor.getLocation(), closestPoint.getLocation()) + angleOffset;
			float radius = closestRange;
			
			if (closestPoint.getOrbit() != null) {
//				OrbitAPI orbit = Global.getFactory().createCircularOrbit(anchor, angle, radius, 
//																closestPoint.getOrbit().getOrbitalPeriod()); 
				OrbitAPI orbit = Global.getFactory().createCircularOrbitPointingDown(anchor, angle, radius, 
						closestPoint.getOrbit().getOrbitalPeriod()); 
				beacon.setOrbit(orbit);
			} else {
				Vector2f beaconLoc = Misc.getUnitVectorAtDegreeAngle(angle);
				beaconLoc.scale(radius);
				Vector2f.add(beaconLoc, anchor.getLocation(), beaconLoc);
				beacon.getLocation().set(beaconLoc);
			}
		}
		
		Color glowColor = new Color(0,140,250,255);
		Color pingColor = new Color(0,160,250,255);
		if (type == AnarchistSystemType.HOUNDS) {
			glowColor = new Color(250,155,0,255);
			pingColor = new Color(250,155,0,255);
		} else if (type == AnarchistSystemType.JUNKBOYS) {
			glowColor = new Color(250,55,0,255);
			pingColor = new Color(250,125,0,255);
		}
		Misc.setWarningBeaconColors(beacon, glowColor, pingColor);
		
		
		return beacon;
	}
	
//	Map<LocationType, Float> weights = new HashMap<LocationType, Float>();
//	weights.put(LocationType.PLANET_ORBIT, 10f);
//	weights.put(LocationType.JUMP_ORBIT, 1f);
//	weights.put(LocationType.NEAR_STAR, 1f);
//	weights.put(LocationType.OUTER_SYSTEM, 5f);
//	weights.put(LocationType.IN_ASTEROID_BELT, 10f);
//	weights.put(LocationType.IN_RING, 10f);
//	weights.put(LocationType.IN_ASTEROID_FIELD, 10f);
//	weights.put(LocationType.STAR_ORBIT, 1f);
//	weights.put(LocationType.IN_SMALL_NEBULA, 1f);
//	weights.put(LocationType.L_POINT, 1f);
//	WeightedRandomPicker<EntityLocation> locs = getLocations(system, 100f, weights);
	
	
	/**
	 * Sorted by *descending* distance from sortFrom.
	 * @param context
	 * @param sortFrom
	 * @return
	 */
	protected List<Constellation> getSortedAvailableConstellations(ThemeGenContext context, boolean emptyOk, final Vector2f sortFrom, List<Constellation> exclude) {
		List<Constellation> constellations = new ArrayList<Constellation>();
		for (Constellation c : context.constellations) {
			if (context.majorThemes.containsKey(c)) continue;
			if (!emptyOk && constellationIsEmpty(c)) continue;
			
			boolean isCore = false;
			for (StarSystemAPI s : c.getSystems()) {
				if (s.hasTag(Tags.THEME_CORE)) {
					isCore = true;
					break;
				}
			}
			if (isCore) continue;
			
			if (c.getLocation() != null && sortFrom != null) {
				if (Misc.getDistance(c.getLocation(), sortFrom) < 22500) continue;
			} else if (c.getLocation() == null) {
				continue;
			}
			
			constellations.add(c);
		}
		
		if (exclude != null) {
			constellations.removeAll(exclude);
		}
		
		Collections.sort(constellations, new Comparator<Constellation>() {
			public int compare(Constellation o1, Constellation o2) {
				float d1 = Misc.getDistance(o1.getLocation(), sortFrom);
				float d2 = Misc.getDistance(o2.getLocation(), sortFrom);
				return (int) Math.signum(d2 - d1);
			}
		});
		return constellations;
	}
        
	public static boolean constellationIsEmpty(Constellation c) {
		for (StarSystemAPI s : c.getSystems()) {
			if (!systemIsEmpty(s)) return false;
		}
		return true;
	}
	public static boolean systemIsEmpty(StarSystemAPI system) {
		for (PlanetAPI p : system.getPlanets()) {
			if (!p.isStar()) return false;
		}
		//system.getTerrainCopy().isEmpty()
		return true;
	}

	
	
	public List<CampaignFleetAPI> addBattlestations(StarSystemData data, float chanceToAddAny, int min, int max, 
												WeightedRandomPicker<String> stationTypes) {
		List<CampaignFleetAPI> result = new ArrayList<CampaignFleetAPI>();
		if (random.nextFloat() >= chanceToAddAny) return result;
		
		int num = min + random.nextInt(max - min + 1);
		if (DEBUG) System.out.println("    Adding " + num + " battlestations");
		for (int i = 0; i < num; i++) {
			
			EntityLocation loc = pickCommonLocation(random, data.system, 200f, true, null);
			
                        String anarchist_faction = "pack";
                        if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_BOYS)) {
                            anarchist_faction = JunkPiratesThemes.JUNKBOYS;
                        } else if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_HOUNDS)) {
                            anarchist_faction = JunkPiratesThemes.HOUNDS;
                        } else if (data.system.hasTag(JunkPiratesTags.THEME_JUNK_TECHNICIANS)) {
                            anarchist_faction = JunkPiratesThemes.TECHNICIANS;
                        }                        
                        
			String type = stationTypes.pick();
			if (loc != null) {
				
				CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(anarchist_faction, FleetTypes.BATTLESTATION, null);
				
				FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, type);
				fleet.getFleetData().addFleetMember(member);
				
				//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
				
				fleet.setStationMode(true);
				
				addJunkPiratesAnarchistStationInteractionConfig(fleet);
				
				data.system.addEntity(fleet);
				
				//fleet.setTransponderOn(true);
				fleet.clearAbilities();
				fleet.addAbility(Abilities.TRANSPONDER);
				fleet.getAbility(Abilities.TRANSPONDER).activate();
				fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);
				
				fleet.setAI(null);
				
				setEntityLocation(fleet, loc, null);
				convertOrbitWithSpin(fleet, 5f);
				
				boolean damaged = type.toLowerCase().contains("damaged");
				@SuppressWarnings("unused")
								float mult = 25f;
				int level = 9;
				if (damaged) {
					mult = 10f;
					level = 4;
					fleet.getMemoryWithoutUpdate().set("damagedStation", true);
				} //else {
					PersonAPI commander = OfficerManagerEvent.createOfficer(
							Global.getSector().getFaction(anarchist_faction), level, true);
//					if (!damaged) {
//						commander.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 3);
//					}
					FleetFactoryV3.addCommanderSkills(commander, fleet, random);
					fleet.setCommander(commander);
					fleet.getFlagship().setCaptain(commander);
				//}
				
				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
				
				
				//RemnantSeededFleetManager.addRemnantAICoreDrops(random, fleet, mult);
				
				result.add(fleet);
				
//				MarketAPI market = Global.getFactory().createMarket("station_market_" + fleet.getId(), fleet.getName(), 0);
//				market.setPrimaryEntity(fleet);
//				market.setFactionId(fleet.getFaction().getId());
//				market.addCondition(Conditions.ABANDONED_STATION);
//				market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
//				((StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);
//				fleet.setMarket(market);
				
			}
		}
		
		return result;
	}
	
	public static void addJunkPiratesAnarchistStationInteractionConfig(CampaignFleetAPI fleet) {
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, 
				   new JunkPiratesAnarchistStationInteractionConfigGen());		
	}
	
	
	@Override
	public int getOrder() {
		return 1500;
	}


	public static class JunkPiratesAnarchistStationInteractionConfigGen implements FleetInteractionDialogPluginImpl.FIDConfigGen {
		public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
			FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
			
			config.alwaysAttackVsAttack = true;
			config.leaveAlwaysAvailable = true;
			config.showFleetAttitude = false;
			config.showTransponderStatus = false;
			config.showEngageText = false;
			
			config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
				public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
				}
				public void notifyLeave(InteractionDialogAPI dialog) {
				}
				public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
					bcc.aiRetreatAllowed = false;
					bcc.objectivesAllowed = false;
				}
			};
			return config;
		}
	}
	
	
}













