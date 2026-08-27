/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.fleets;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.campaign.intel.misc.JunkExplorerDecisionIntel;
import static data.scripts.JunkPiratesModPlugin.enableJunkPiratesIntel;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author paul well actually sort of was this time
 */
public final class JunkPiratesExplorerFleetAssignmentAI implements EveryFrameScript {
    
    private final JunkPiratesExplorerData data;
    private final CampaignFleetAPI fleet;
    private boolean orderedEscape = false;
    
    public static Logger log = Global.getLogger(JunkPiratesExplorerFleetManager.class);
    
    public JunkPiratesExplorerFleetAssignmentAI(CampaignFleetAPI fleet, JunkPiratesExplorerData data) {
        this.fleet = fleet;
        this.data = data;

        setFleetUp();
    }

    @Override
    public void advance(float amount) {
        //SectorEntityToken home = data.from.getPrimaryEntity();
        
        if (fleet.getAI().getCurrentAssignment() != null) { // there is a command to action
            if (data.to != null && data.to.getPrimaryEntity() == null) { // nowhere to go
                fleet.clearAssignments();
                orderedEscape = true;
            } else
            
            if (fleet.getFleetPoints() < data.startingFP / 2 ) { // severely damaged
                fleet.clearAssignments();
                orderedEscape = true;
            }
        } else {
            if (orderedEscape) { // review logic against behaviour!
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, data.from.getPrimaryEntity(), 1000f, "aborting mission");// go back whence they came and despawn
            } else if ("mission_complete".equals(data.mission)) { // no assignments and not escaping - get on with it
                fleet.addAssignment (FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, data.from.getPrimaryEntity(), 1000f, getReturningActionText());
            } else if ("go_party".equals(data.mission)) {
                
                MarketAPI partyPlanet = findPartyPlanet();
                
                if (partyPlanet != null) {
                    log.info("Junk Explorer, " + data.fleet.getCommander().getNameString() + " wants to party at " + partyPlanet.getName());
                } else {
                    log.info("Junk Explorer, " + data.fleet.getCommander().getNameString() + " can't party today. Sadface.");
                }
                
                if (data.fleet != null && partyPlanet != null) {
                    data.to = partyPlanet;
                    if (enableJunkPiratesIntel) {
                        new JunkExplorerDecisionIntel(data, "party");
                    }
                    fleet.addAssignment (FleetAssignment.GO_TO_LOCATION, data.to.getPrimaryEntity(), 1000f, getTravelActionText());
                    fleet.addAssignment (FleetAssignment.ORBIT_PASSIVE, data.to.getPrimaryEntity(), getOrbitDays(), getOrbitActionText());
                    data.mission = "mission_complete";
                } else {
                    data.mission = "mission_complete";
                }
            } else if ("troll_about".equals(data.mission)) {
                
                MarketAPI friendlyTrollPort = findBaseToTrollSystemFrom();
                
                if (friendlyTrollPort != null) {
                    log.info("Junk Explorer, " + data.fleet.getCommander().getNameString() + " wants to troll around from " + friendlyTrollPort.getName());
                } else {
                    log.info("Junk Explorer, " + data.fleet.getCommander().getNameString() + " can't troll today. Not good.");
                }
                
                if (data.fleet != null && friendlyTrollPort != null) {
                    
                    data.to = friendlyTrollPort;
                    if (enableJunkPiratesIntel) {
                        new JunkExplorerDecisionIntel(data, "troll");
                    }
                    data.fleet.setName("Troublemakers");
                    // might want to enlarge fleet at this point? See how goes.
                    fleet.addAssignment (FleetAssignment.GO_TO_LOCATION, data.to.getPrimaryEntity(), 1000f, getTravelActionText());
                    fleet.addAssignment (FleetAssignment.ORBIT_AGGRESSIVE, data.to.getPrimaryEntity(), getOrbitDays(), getOrbitActionText());
                    fleet.addAssignment (FleetAssignment.PATROL_SYSTEM, data.to.getPrimaryEntity(), getOrbitDays() * 2, getPatrolActionText());
                    data.mission = "mission_complete";
                } else {
                    data.mission = "mission_complete";
                }
            } else if ("scavenge_system".equals(data.mission)) {
                StarSystemAPI scavSystem = findScavengeSystem();
                SectorEntityToken targetEntity = null;
                
                if (scavSystem != null) {
                    List<SectorEntityToken> salvageables = scavSystem.getEntitiesWithTag(Tags.SALVAGEABLE);
                    if (!salvageables.isEmpty()) {
                        targetEntity = salvageables.get((int) (Math.random() * salvageables.size()));
                    }
                }
                
                MarketAPI targetMarket = findScavengePlanetMarket(scavSystem);
                
                if (data.fleet != null && targetEntity != null && targetMarket != null) {
                    data.to = targetMarket;
                    if (enableJunkPiratesIntel) {
                        new JunkExplorerDecisionIntel(data, "scavenge");
                    }
                    
                    String targetName = targetEntity.getName();
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, targetEntity, 1000f, "Traveling to salvage " + targetName + " in the " + scavSystem.getBaseName() + " system");
                    fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, targetEntity, getOrbitDays(), "Scavenging " + targetName);
                    data.mission = "mission_complete";
                } else {
                    data.mission = "mission_complete";
                }
            } else if ("then_what".equals(data.mission)) {
                
                    data.mission = "mission_complete";
                    
            } else if ("self_reflect".equals(data.mission)) {
                // this is the point where we have just started orbiting our homeworld and we are deciding what to do
                // This defines the principal path through decision logic
                float makeChoice = (float) Math.random();
                
                if ( makeChoice <= 0.3f ) { // 30% chance to go make trouble.
                    data.mission = "troll_about";
                } else if ( makeChoice <= 0.6f ) { // 30% chance to go scavenging.
                    data.mission = "scavenge_system";
                } else { // 40% chance to go party.
                    data.mission = "go_party";
                }
            }
        }

    }    
    
    public MarketAPI findBaseToTrollSystemFrom() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        
        StarSystemAPI thisSys = findTrollSystem();
        
        if (thisSys != null && thisSys.getPlanets() != null) {
        
            for (PlanetAPI planet : thisSys.getPlanets()) {

                MarketAPI market = planet.getMarket();

                if (market != null) {
                    // Find markets to visit.
                    if (market.isHidden()) continue;
                    if (market == data.from) continue; // this is where we started

                    if (!market.hasSpaceport()) continue;
                    if (market.getFaction() != null && market.getFaction().isHostileTo("junk_pirates")) continue;

                    markets.add(market,1); // go evens on non hostile markets
                        }
                }
            } else {
                return null;
        }
        
        return markets.pick();
    }
    
//    public boolean playerHere() {
//        if (Global.getSector().getPlayerFleet().getStarSystem() == data.to.getStarSystem()) {
//            return true;
//        } else {
//            return false;
//        }
//    }
    
    public StarSystemAPI findPartySystem() {
            WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>();
            
            for (StarSystemAPI starsys : Global.getSector().getStarSystems()) { //grab systems one by one
                
                float weight = 0; // clear the weighting
                
                boolean decivpop = false;
                boolean systemhostile = false; //set up / reset triggers ready for next system
                
                for (PlanetAPI planet : starsys.getPlanets()) { // for each planet in this system


                    
                    MarketAPI market = planet.getMarket(); // grab the market

                            if (market != null) { // as long as there is a market here then ...
                                
                                    // Find markets to visit.
                                    //if (market.isHidden()) continue;
                                    if (market == data.from) continue; // this is where we grew up
                                    
                                    if (market.getFaction() != null && market.getFaction().isHostileTo("junk_pirates")) {
                                        systemhostile = true; // we don't really want hostile systems, would kill the buzz
                                                }
                                    
                                    
                                    if (market.hasCondition(Conditions.DECIVILIZED_SUBPOP) || market.hasCondition(Conditions.DECIVILIZED)) {
                                        decivpop = true; // We need this, otherwise how else would we party.
                                        weight +=10; // strongly favour deciv pops, esp. will weight for multiple per system.
                                                }
                                    
                                    if (market.hasCondition(Conditions.POPULATION_1) ||
                                            market.hasCondition(Conditions.POPULATION_2) ||
                                            market.hasCondition(Conditions.POPULATION_3) ||
                                            market.hasCondition(Conditions.POPULATION_4) ||
                                            market.hasCondition(Conditions.POPULATION_5) ||
                                            market.hasCondition(Conditions.POPULATION_6) ||
                                            market.hasCondition(Conditions.POPULATION_7) ||
                                            market.hasCondition(Conditions.POPULATION_8) ||
                                            market.hasCondition(Conditions.POPULATION_9) ||
                                            market.hasCondition(Conditions.POPULATION_10)) {
                                        
                                        weight -= 5; // but try and steer clear of populated systems. Defeats the purpose.
                                        
                                                }


                                    }
                                    // end of if market null check

                    }
                // end of planet loop
                // still in system loop
                        if (decivpop && !systemhostile) {
                            if (weight <= 0) weight = 0;
                            systems.add(starsys, weight); // go to systems where we know we are going to find a party system.
                        // prefer busier systems, generally
                            }
                        
        } // end of system check loop
        
        return systems.pick();
    }
    
    public StarSystemAPI findTrollSystem() {
            WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>();
            
            for (StarSystemAPI starsys : Global.getSector().getStarSystems()) { //grab systems one by one
                
                float weight = 0; // clear the system weighting
                
                boolean systemfriendly = false;
                boolean systemhostile = false; //set up / reset triggers ready for next system
                
                for (PlanetAPI planet : starsys.getPlanets()) { // for each planet in this system


                    
                    MarketAPI market = planet.getMarket(); // grab the market

                            if (market != null) { // as long as there is a market here then ...
                                
                                    // Find markets to visit.
                                    if (market.isHidden()) continue;
                                    if (market == data.from) continue; // this is where we grew up
                                    if (!market.hasSpaceport()) continue; // not a relevant market really
                                    
                                    if (market.getFaction() != null && market.getFaction().isHostileTo("junk_pirates")) {
                                        systemhostile = true;
                                        weight += 3;// we do want at least one hostile market
                                                } else if
                                    
                                     (market.hasCondition(Conditions.POPULATION_1) ||
                                            market.hasCondition(Conditions.POPULATION_2) ||
                                            market.hasCondition(Conditions.POPULATION_3) ||
                                            market.hasCondition(Conditions.POPULATION_4) ||
                                            market.hasCondition(Conditions.POPULATION_5) ||
                                            market.hasCondition(Conditions.POPULATION_6) ||
                                            market.hasCondition(Conditions.POPULATION_7) ||
                                            market.hasCondition(Conditions.POPULATION_8) ||
                                            market.hasCondition(Conditions.POPULATION_9) ||
                                            market.hasCondition(Conditions.POPULATION_10)) {
                                        
                                        systemfriendly = true; // any populated market suits
                                        weight += 2;
                                                }


                                    }
                                    // end of if market null check

                    }
                // end of planet loop
                // still in system loop
                        if (systemfriendly && systemhostile) { // there are bad guys, and not bad guys.
                            systems.add(starsys, weight); // go to systems where we can get a scrap
                        // prefer busier systems, generally
                            }
                        
        } // end of system check loop
        
        return systems.pick();
    }
    
    public MarketAPI findPartyPlanet() { // find somewhere with a decivilized population away from hostile folk
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        
        StarSystemAPI thisSys = findPartySystem(); // party system tried to find systems with decivilized populations

            if (thisSys != null && thisSys.getPlanets() != null) {
                for (PlanetAPI planet : thisSys.getPlanets()) {
                    MarketAPI market = planet.getMarket();
                    if (market != null) {
                        // Find markets to visit.
                        if (market == data.from) continue; // this is where we grew up

                        if (market.getFaction() != null && market.getFaction().isHostileTo("junk_pirates")) continue; // might be in civ space, don't go to unfriendly market.

                        if (market.hasCondition(Conditions.DECIVILIZED) || market.hasCondition(Conditions.DECIVILIZED_SUBPOP))
                        markets.add(market,1); // go evens on all markets in this system that comply
                    }

                }
            } else {
                return null;
            }
        
        return markets.pick(); // slam in a new destination in the data;

    }

    public StarSystemAPI findScavengeSystem() {
        WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>();
        
        for (StarSystemAPI starsys : Global.getSector().getStarSystems()) {
            if (starsys.hasTag(Tags.THEME_CORE)) continue;
            
            float weight = 0;
            boolean hasInhabited = false;
            
            for (PlanetAPI planet : starsys.getPlanets()) {
                MarketAPI market = planet.getMarket();
                if (market != null && !market.isHidden() && !market.getFactionId().equals(Factions.NEUTRAL)) {
                    hasInhabited = true;
                    break;
                }
            }
            if (hasInhabited) continue;
            
            List<SectorEntityToken> salvageables = starsys.getEntitiesWithTag(Tags.SALVAGEABLE);
            if (salvageables.isEmpty()) continue;
            
            weight += salvageables.size() * 5f;
            systems.add(starsys, weight);
        }
        
        return systems.pick();
    }

    public MarketAPI findScavengePlanetMarket(StarSystemAPI starsys) {
        if (starsys == null || starsys.getPlanets().isEmpty()) return null;
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (PlanetAPI planet : starsys.getPlanets()) {
            MarketAPI market = planet.getMarket();
            if (market != null) {
                markets.add(market, 1f);
            }
        }
        return markets.pick();
    }
    
    public static class JunkPiratesExplorerData {
        public String mission;
        public float startingFP;
        public MarketAPI from;
        public MarketAPI to;
                
        public CampaignFleetAPI fleet;
        
        public JunkPiratesExplorerData(CampaignFleetAPI fleet) {
            this.fleet = fleet;
            }
        
        }
        
        protected float getOrbitDays() {
            return 3.0f + ( (float) Math.random() * 5.0f ); // 3-8 days ... 8 days seemed a bit slow for some fleets.
        }
        
        protected final void setFleetUp() {
                fleet.addAssignment (FleetAssignment.ORBIT_PASSIVE, data.from.getPrimaryEntity(), getOrbitDays(), getStartingActionText());
                data.mission = "self_reflect";
        }
        
	
	
	
	protected String getStartingActionText() {
            // Starting in orbit around home planet
            
            WeightedRandomPicker<String> motivation = new WeightedRandomPicker<String>();
            
            motivation.add("Reflecting on means to achieve imperfection", 10);
            motivation.add("Seeking motivation", 8);
            motivation.add("Considering life", 10);
            motivation.add("Lost in self reflection", 4);
            motivation.add("Drowning in pity", 1);
            
            return motivation.pick();
            
	}
	protected String getPatrolActionText() {
            // Starting in orbit around home planet
            
            String missionText = "Patrolling the system aggressively";
            
            return missionText;
            
	}

        protected String playerName() {
            return Global.getSector().getPlayerPerson().getNameString();
        }

        protected String fleetCommanderName() {
            return data.fleet.getCommander().getNameString();
        }
        
        protected String factionDisplayName(String faction) {
            
            String factionName = Global.getSector().getFaction(faction).getDisplayName();
            
            return factionName;
        }
        
        protected String getOrbitActionText() {
                // Hanging about ...
                
                if ("troll_about".equals(data.mission)) {
                    return "Preparing for local raids in " + data.to.getStarSystem().getBaseName();
                } else if ("go_party".equals(data.mission)) {
                    return randomPartyActivity() + " at " + data.to.getName();
                }
		return "Everything seems fine, right?";
	}
        
        protected String randomPartyActivity() {
            WeightedRandomPicker<String> kneesup = new WeightedRandomPicker<String>();
            WeightedRandomPicker<String> locals = new WeightedRandomPicker<String>();
            WeightedRandomPicker<String> joiner = new WeightedRandomPicker<String>();
            
            kneesup.add("Hunting for large wildlife", 2);
            kneesup.add("Providing group therapy", 1);
            kneesup.add("Participating in combat games", 1);
            kneesup.add("Getting drunk", 5);
            kneesup.add("Playing war games", 2);
            kneesup.add("Sharing stories about friends", 2);
            kneesup.add("Organising high stakes card games", 2);
            kneesup.add("Partying planetside", 10);
            kneesup.add("Having a drink", 5);
            kneesup.add("Playing Chess", 1);
            kneesup.add("Playing Go", 1);
            kneesup.add("Organising an orbital party", 1);
            kneesup.add("Experimenting with psychotropic substances", 1);
            kneesup.add("Controlling manually-operated land-based hydrocarbon-based vehicles irresponsibly", 1);
            
            joiner.add(" with", 6);
            joiner.add(" alongside", 1);
            joiner.add(" on behalf of", 1);
            
            locals.add(" the locals",15);
            locals.add(" anyone and everyone",3);
            locals.add(" the machine people",2);
            locals.add(" biker gangs",2);
            locals.add(" a cyborg queen",1);
            locals.add(" local transhumanist subfactions",5);
            locals.add(" local so-called scientists",2);
            locals.add(" the first people who asked",2);
            locals.add(" an awestruck group of local pseudo-diplomats",1);
            
            return kneesup.pick() + joiner.pick() + locals.pick();
        }
        
        protected String getReturningActionText() {
            // Done. Either going back because run out of gas (failed) or successful attack on player (won).
            // Decide whether to be happy or sad or just non-specific.
            return "Returning back to " + data.from.getName(); // from shouldn't change for a fleet,
        }
        
        protected String getMissionText() {
            // Done. Either going back because run out of gas (failed) or successful attack on player (won).
            // Decide whether to be happy or sad or just non-specific.
            return "Returning back to " + data.from.getName(); // from shouldn't change for a fleet,
        }
	
	protected String getTravelActionText() {
// strings returned for explorers when traveling
                           
            String mission = data.mission;
            
            String missionText = "Traveling";

            if ( "troll_about".equals(mission)) missionText = "Traveling to " + data.to.getName();
            if ( "go_party".equals(mission)) missionText = "Exploring; Traveling to " + data.to.getName() + " in the " + data.to.getStarSystem().getBaseName() + " system";
            if ( "mission_complete".equals(mission)) missionText = "Returning to " + data.from.getName();
                
                if (mission.isEmpty()) {
                        return "traveling to " + getData().to.getName();
                } else {
                    return missionText;
                }
	}
	
	protected String getInSystemActionText(RouteSegment segment) {
            return "WHAT";
	}
        
        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
        
        @Override
        public boolean isDone()
        {
            return !fleet.isAlive();
        }
	
	protected JunkPiratesExplorerData getData() {
                
                return data;
	}
	
}
	











