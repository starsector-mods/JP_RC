/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.fleets;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.CargoQuantityData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import static data.scripts.JunkPiratesModPlugin.isExerelin;
import org.apache.log4j.Logger;

/**
 *
 * @author paul yeah right
 */
public final class SyndicateAspCourierFleetAssignmentAI implements EveryFrameScript {
    
    private static final Logger log = Global.getLogger(SyndicateAspCourierFleetAssignmentAI.class);
    
    private final SyndicateAspCourierRouteData data;
    private final CampaignFleetAPI fleet;
    private boolean orderedEscape = false;
    
    public SyndicateAspCourierFleetAssignmentAI(CampaignFleetAPI fleet, SyndicateAspCourierRouteData data) {
        this.fleet = fleet;
        this.data = data;

        setFleetUp();
    }

    @Override
    public void advance(float amount) {
        //SectorEntityToken home = data.from.getPrimaryEntity();
        
        if (fleet.getAI().getCurrentAssignment() != null) { // there is a command to action
            if (data.to.getPrimaryEntity() == null) { // nowhere to go
                fleet.clearAssignments();
                orderedEscape = true;
            } else
            
            if (fleet.getFleetPoints() < data.startingFP / 2 ) { // severely damaged
                fleet.clearAssignments();
                orderedEscape = true;
            }
        } else {
            if (orderedEscape) {
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, data.from.getPrimaryEntity(), 1000, "aborting mission");// go back whence they came and despawn
            } else if ("delivered".equals(data.mission)) { // no assignments and not esacping - get on with it
                data.fleet.getCargo().clear();
                fleet.addAssignment (FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, data.from.getPrimaryEntity(), getOrbitDays(), getReturningActionText());
            } else {
                fleet.addAssignment (FleetAssignment.ORBIT_PASSIVE, data.from.getPrimaryEntity(), getOrbitDays(), getStartingActionText());
                fleet.addAssignment (FleetAssignment.GO_TO_LOCATION, data.to.getPrimaryEntity(), 1000, getTravelActionText());
                fleet.addAssignment (FleetAssignment.ORBIT_PASSIVE, data.to.getPrimaryEntity(), getOrbitDays(), getEndingActionText());
                data.mission = "delivered";
                
            }
        }

    }    

    public static class SyndicateAspCourierRouteData {
        public String mission = "items"; // refactor to 'mission', 'customerFaction', we will need fuel and cargo capacity to be fair
        public String cargotype = "items"; // refactor to 'mission', 'customerFaction', we will need fuel and cargo capacity to be fair
        public String customerFaction;
        public float cargoCap;
        public float fuelCap;
        public float personnelCap;
        public float startingFP;
        //public boolean money = false;
        public float size;
        public boolean smuggling = false;
        public MarketAPI from;
        public MarketAPI to;
        
        public List<CargoQuantityData> cargoDeliver = new ArrayList<CargoQuantityData>();
        public List<SpecialItemData> specialDeliver = new ArrayList<SpecialItemData>();
        public List<String> weaponDeliver = new ArrayList<String>();
        
        public CampaignFleetAPI fleet;
        // add something like a getcargolist function
        
        // we will need a list of stuff that is in cargo probably just a cargoDeliver
        public void addDeliver(String id, int qty) {
			cargoDeliver.add(new CargoQuantityData(id, qty));
            }
        
        // add Deliver function
        public static String getCargoList(List<CargoQuantityData> cargo) {
            if (cargo == null || cargo.isEmpty()) {
                return "nothing";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cargo.size(); i++) {
                CargoQuantityData item = cargo.get(i);
                sb.append(item.units).append(" ").append(item.cargo);
                if (i < cargo.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
        
        public SyndicateAspCourierRouteData(CampaignFleetAPI fleet) {
            this.fleet = fleet;
        }
        
        }
	
	@SuppressWarnings("unused")
		private String origFaction;
	@SuppressWarnings("unused")
		private String customerFaction;
//	private IntervalUtil factionChangeTracker = new IntervalUtil(0.1f, 0.3f);
        
        protected float getOrbitDays() {
            return 3.0f;
        }
        
        protected final void setFleetUp() {            
            updateCargo();
        }
        
	protected final void updateCargo() { // needs work based on missions
	
            
            
                float tier = data.size;
		
		CargoAPI cargo = fleet.getCargo();
		cargo.clear();
		
                if ("prisoner".equals(data.mission)) {
                    data.addDeliver(Commodities.CREW, (int) tier * 25);
                    if (isExerelin) { // Nex is enabled and we can stick a prisoner in the cargo hold
                            data.addDeliver("prisoner", (int) tier);
                            float creds = (float) Math.random() * tier;
                            data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                    } else {
                        float creds = (float) Math.random() * 3 + tier;// not much to do I guess ... stick a few credits in the hold
                        data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                    }
                }
                
                if ("vip".equals(data.mission)) {
                    data.addDeliver(Commodities.LOBSTER, (int) tier * 5);
                    data.addDeliver(Commodities.LUXURY_GOODS, (int) tier * 10);
                    if (isExerelin) { // Nex is enabled and we can stick a VIP in the cargo hold
                            data.addDeliver("agent", (int) tier);
                            float creds = (float) Math.random() * 3 + tier;
                            data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                    } else {
                        float creds = (float) Math.random() * 6 + tier;// not much to do I guess ... stick a few thousand credits in the hold
                        data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                    }
                }
                
                if ("money".equals(data.mission)) {
                        float creds = (float) Math.random() + tier * 3;// not much to do I guess ... stick a few thousand credits in the hold
                        data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                }
                
                if ("items".equals(data.mission)) {
                        float creds = (float) Math.random() * tier + 3;
                        data.addDeliver("syndicate_asp_credit_chip", (int) creds);
                        
                        boolean isMilitary = data.to.hasIndustry("militarybase") || data.to.hasIndustry("highcommand");
                        boolean isHeavy = data.to.hasIndustry("heavyindustry") || data.to.hasIndustry("orbitalworks");
                        boolean isMining = data.to.hasIndustry("mining") || data.to.hasIndustry("refining");
                        boolean isCommerce = data.to.hasIndustry("commerce") || data.to.hasIndustry("lightindustry");
                        
                        if (isMilitary) {
                            data.addDeliver(Commodities.HAND_WEAPONS, (int) (tier * 20));
                            data.addDeliver(Commodities.MARINES, (int) (tier * 30));
                            data.addDeliver(Commodities.SUPPLIES, (int) (tier * 100));
                        } else if (isHeavy) {
                            data.addDeliver(Commodities.HEAVY_MACHINERY, (int) (tier * 40));
                            data.addDeliver(Commodities.RARE_METALS, (int) (tier * 50));
                            if (Math.random() > 0.85f) {
                                cargo.addSpecial(new SpecialItemData("corrupted_nanoforge", null), 1);
                            }
                        } else if (isMining) {
                            data.addDeliver(Commodities.DRUGS, (int) (tier * 30));
                            data.addDeliver(Commodities.VOLATILES, (int) (tier * 50));
                            data.addDeliver(Commodities.HEAVY_MACHINERY, (int) (tier * 20));
                        } else if (isCommerce) {
                            data.addDeliver(Commodities.LUXURY_GOODS, (int) (tier * 50));
                            data.addDeliver(Commodities.DRUGS, (int) (tier * 20));
                            data.addDeliver(Commodities.ORGANS, (int) (tier * 10));
                        } else {
                            data.addDeliver(Commodities.LUXURY_GOODS, (int) (tier * 20));
                            data.addDeliver(Commodities.DRUGS, (int) (tier * 10));
                        }
                }
                
                for (CargoQuantityData thing : data.cargoDeliver) { // then stick cargo in the fleet data
                    try {
                        if (Global.getSettings().getCommoditySpec(thing.cargo) != null) {
                            cargo.addCommodity(thing.cargo, thing.units);
                        }
                    } catch (Exception e) {
                        log.error("Failed to add commodity " + thing.cargo + " to ASP courier fleet", e);
                    }
                }
                
	}
	
	
	
	protected String getStartingActionText() {
            String mission = data.mission;
            
            String missionText = "INITIALISE";

            if ( "prisoner".equals(mission)) missionText = "Negotiating with " + factionDisplayName(data.customerFaction) + " officials";
            if ( "vip".equals(mission)) missionText = "Wining and dining with notable " + factionDisplayName(data.customerFaction) + " individuals";
            if ( "items".equals(mission)) missionText = "Discussing terms with " + factionDisplayName(data.customerFaction) + " traders";
            if ( "money".equals(mission)) missionText = "Discussing terms with " + factionDisplayName(data.customerFaction) + " financiers";
            
            return missionText + " at " + data.from.getName();
            
	}

        protected String factionDisplayName(String faction) {
            
            String factionName = Global.getSector().getFaction(faction).getDisplayName();
            
            return factionName;
        }
        
	protected String getEndingActionText() {
                
		return "Closing contract with " + factionDisplayName(data.customerFaction) + " representatives at " + getData().to.getName();
	}
        
        protected String getReturningActionText() {
            return "Returning back to " + data.from.getName();
        }
	
	protected String getTravelActionText() {
            
                String mission = data.mission;
                
		String missionText = "INITIALISE";
                
                if ( "prisoner".equals(mission)) missionText = "Transporting a dangerous prisoner";
                if ( "vip".equals(mission)) missionText = "Travelling with style";
                if ( "items".equals(mission)) missionText = "On a delivery contract";
                if ( "money".equals(mission)) missionText = "Providing financial services";
                
                if (mission.isEmpty()) {
                        return "traveling to " + getData().to.getName();
                }

                return missionText + " to " + getData().to.getName();

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
	
	protected SyndicateAspCourierRouteData getData() {
                
                return data;
	}
	
}
	











