/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.fleets;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.JunkPiratesModPlugin.junkPiratesFleetFrequencyModifier;
import static data.scripts.JunkPiratesModPlugin.junkPiratesMaxFleetModifier;
import data.scripts.campaign.fleets.JunkPiratesExplorerFleetAssignmentAI.JunkPiratesExplorerData;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author paul - sets up a point to point courier fleet carrying valuable cargo.
 * Interception should have significant consequences in relations for customer and ASP. NOT APPLIED
 * But potential for high value rewards.
 */
public class JunkPiratesExplorerFleetManager extends BaseCampaignEventListener implements EveryFrameScript {

    private List<JunkPiratesExplorerData> activeJunkFleets = new LinkedList<>();
    private IntervalUtil tracker;
    transient private boolean hasRunSweep = false;
    
    public static final Logger log = Global.getLogger(JunkPiratesExplorerFleetManager.class);
    
    public JunkPiratesExplorerFleetManager() {
        super(true);
    }

    public IntervalUtil getTracker() {
        if (tracker == null) {
            float interval = Global.getSettings().getFloat("averagePatrolSpawnInterval");
            tracker = new IntervalUtil(interval * 0.45f / junkPiratesFleetFrequencyModifier, interval * 0.75f / junkPiratesFleetFrequencyModifier);
        }
        return tracker;
    }

    protected Object readResolve() {
        Global.getSector().addTransientListener(this);
        getTracker();
        
        if (activeJunkFleets == null) activeJunkFleets = new java.util.LinkedList<>();
        
        return this;
    }
    
    @Override
    public void advance(float amount) {
        if (!hasRunSweep) {
            Global.getSector().addTransientListener(this);
            if (activeJunkFleets == null) activeJunkFleets = new java.util.LinkedList<>();
            if (activeJunkFleets.isEmpty()) {
                for (com.fs.starfarer.api.campaign.LocationAPI loc : Global.getSector().getAllLocations()) {
                    for (com.fs.starfarer.api.campaign.CampaignFleetAPI f : loc.getFleets()) {
                        if (f != null && f.getMemoryWithoutUpdate() != null && f.getMemoryWithoutUpdate().getBoolean("junkPirateExplorers")) {
                            boolean found = false;
                            for (JunkPiratesExplorerFleetAssignmentAI.JunkPiratesExplorerData rd : activeJunkFleets) {
                                if (rd.fleet == f) { found = true; break; }
                            }
                            if (!found) {
                                activeJunkFleets.add(new JunkPiratesExplorerFleetAssignmentAI.JunkPiratesExplorerData(f));
                            }
                        }
                    }
                }
            }
            hasRunSweep = true;
        }

        if (Global.getSector() == null || Global.getSector().getClock() == null) return;
        float days = Global.getSector().getClock().convertToDays(amount);
        
        getTracker().advance(days);
        if (!getTracker().intervalElapsed()) {
            return;
        }

        List<JunkPiratesExplorerData> toRemove = new ArrayList<JunkPiratesExplorerData>();
        for (JunkPiratesExplorerData rd : activeJunkFleets) {
            if (rd.fleet == null || !rd.fleet.isAlive() || rd.fleet.getContainingLocation() == null) {
                toRemove.add(rd);
            }
        }
        activeJunkFleets.removeAll(toRemove);
        
        boolean junk_markets = false; // check the Junk Pirates have a presence
        
        for (MarketAPI checkMarkets : Global.getSector().getEconomy().getMarketsCopy()) {
            if ("junk_pirates".equals(checkMarkets.getFactionId())) {
                junk_markets = true; // we are still alive
            }
        }
        
        if (!junk_markets) { // if we are not then step out
            return;
        }
        
        
        addExplorerFleetIfPossible();
        
        
    }
    
    protected int getMaxFleets() {
        int numMarkets = 0; for(com.fs.starfarer.api.campaign.econ.MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) { if("junk_pirates".equals(m.getFactionId())) numMarkets++; }
        if (numMarkets == 0) return 0;
        int maxBasedOnMarket = (int) ( numMarkets * junkPiratesMaxFleetModifier / 6 ); //numMarkets * 2 is vanilla equivalent for Economy fleets. We want to be well below this.
        return Math.max(3, maxBasedOnMarket); // probably want to externalise this in mendoncaModSettings? 3, or more
    }
    
    protected void addExplorerFleetIfPossible() {
        
        int numFleets = activeJunkFleets.size();
        
        if (getJunkPiratesMarketSource() != null) { // check the Junk Pirates have a market
            log.info("Currently " + numFleets + " Explorers operating out of max " + getMaxFleets());
            if (numFleets < getMaxFleets()) {

                MarketAPI from = getJunkPiratesMarketSource();
                //log.info("Trying from " + from.getName() + " to " + to.getName());
                if (from != null) {

                        JunkPiratesExplorerData data = createData(from);

                        log.info("Added Junk Explorer starting from " + from.getName()); // + " to " + to.getName());

                        spawnFleet(data);

                }
            }
        }
    }

    
    public MarketAPI pickExplorerSourceMarket() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            if (!market.hasSpaceport()) continue;
            if (!"junk_pirates".equals(market.getFactionId())) continue; // only get ASP Syndicate Markets
            
            float w = market.getSize() + market.getStabilityValue();
            
            markets.add(market, w); // Weights the market by market size / stability.
            
        }
        return markets.pick();
    }
    
    public MarketAPI pickExplorerDestMarket(MarketAPI from) {
        
        if (from == null) return null;
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            // Find markets to visit.
            if (market.isHidden()) continue;
            if (market == from) continue;

            if (!market.hasSpaceport()) continue;
            if (market.getFaction() != null && market.getFaction().isHostileTo("junk_pirates")) continue; // only get markets who would work with the ASP Syndicate

            markets.add(market, market.getSize()); // weights the market by market size.
        }
        return markets.pick();
    }
    
    public static JunkPiratesExplorerData createData(MarketAPI from) {
                     
        CampaignFleetAPI placeHolder = FleetFactoryV3.createEmptyFleet("junk_pirates", FleetTypes.MERC_PRIVATEER, from);
        
        JunkPiratesExplorerData data = new JunkPiratesExplorerData(placeHolder); // this logic is bollocks but lets see if we can run with something for now
        
//        public String mission;
//        public float startingFP;
//        public MarketAPI from;
//        public MarketAPI to;
//                
//        public CampaignFleetAPI fleet;
        
        if (data.mission == null) data.mission = "mission_complete";
        
        data.from = from;
        data.to = from; // we don't know what to do yet
        
        return data;
    }
       
    public CampaignFleetAPI spawnFleet(JunkPiratesExplorerData data) {
//        Random random = new Random();
        
        CampaignFleetAPI fleet = createJunkExplorerFleet(data);
        
        if (fleet == null || data.from == null || data.from.getPrimaryEntity() == null || 
            data.from.getPrimaryEntity().getContainingLocation() == null || data.from.getPrimaryEntity().getLocation() == null) return null;
        data.fleet = fleet;
        data.startingFP = fleet.getFleetPoints();
        
        // fleet.addEventListener(this); // does it need a listener? Resolve with rules triggered scripts
        
        fleet.addScript(new JunkPiratesExplorerFleetAssignmentAI(fleet, data)); //hmm is this right
        //log.info("Junk Explorers Created ... ");
        
        SectorEntityToken entity = data.from.getPrimaryEntity();
        entity.getContainingLocation().addEntity(fleet);
        fleet.setLocation(entity.getLocation().x, entity.getLocation().y);
      
        activeJunkFleets.add(data);
        
        return fleet;
    }
    
    public void cleanFleet(JunkPiratesExplorerData data) {
        for (JunkPiratesExplorerData thingy : activeJunkFleets) {
            if (data.equals(thingy)) {
                activeJunkFleets.remove(data);
                break;
            }
        }
    }
    
    public static MarketAPI getJunkPiratesMarketSource() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            if (!market.hasSpaceport()) continue;
            if (!"junk_pirates".equals(market.getFactionId())) continue; // don't get non-syndicate markets
            
            float w = market.getSize(); // weight by market size
            
            markets.add(market, w); // Weights the market by size
            
        }
        
        if (markets.isEmpty()) {
            log.info("No Junk Pirates Markets found ...");
            return null;
        }
        MarketAPI thismarket = markets.pick();

        return thismarket;
    }
    
    public static String getFleetTypeIdForTier(float fp) {
        
        String type = FleetTypes.PATROL_MEDIUM;
        
        if (fp > 60) {
            type = FleetTypes.PATROL_LARGE;
        }
        
        return type;
    }
    
    public static float findFPBonus(JunkPiratesExplorerData data) {
        float fp_bonus = 0f;
        
        MarketAPI market = data.from;
        
        if (market.hasIndustry(Industries.PATROLHQ) || market.hasIndustry(Industries.MILITARYBASE) || market.hasIndustry(Industries.HIGHCOMMAND)) fp_bonus += 20f;
        if (market.hasIndustry(Industries.BATTLESTATION) || market.hasIndustry(Industries.BATTLESTATION_MID) || market.hasIndustry(Industries.BATTLESTATION_HIGH)) fp_bonus += 20f;
        if (market.hasIndustry(Industries.GROUNDDEFENSES) || market.hasIndustry(Industries.HEAVYBATTERIES)) fp_bonus += 10f;
        if (market.hasIndustry(Industries.HEAVYINDUSTRY) || market.hasIndustry(Industries.ORBITALWORKS)) fp_bonus += 40f;
        if (market.hasIndustry(Industries.STARFORTRESS) || market.hasIndustry(Industries.STARFORTRESS_MID) || market.hasIndustry(Industries.STARFORTRESS_HIGH)) fp_bonus += 40f;
        if (market.hasIndustry(Industries.WAYSTATION)) fp_bonus += 10f;
        
        return fp_bonus;
    }
    
    public static CampaignFleetAPI createJunkExplorerFleet(JunkPiratesExplorerData data) {
        
        if (data.from == null) return null;
        
//        MarketAPI from = data.from;
//        MarketAPI to = data.to;
        
        float base_fp_fleet = ( data.from.getSize() + data.from.getStabilityValue() ) * 8f * (float) Math.random();
        
        float bonus_fp_fleet = findFPBonus(data);
        
        float fp_fleet = base_fp_fleet + bonus_fp_fleet;
        
        String factionId = "junk_pirates";
        
        float combat = fp_fleet; // how is this defined
        float freighter = 0.1f;
        float liner = 0f;
        float tanker = 0.15f;
        float transport = 0f;
        float utility = 0.05f;
        
        String type = getFleetTypeIdForTier(fp_fleet);
        
        MarketAPI market = data.from;
        
        FleetParamsV3 params = new FleetParamsV3(
                        market,
                        null, // locinhyper
                        factionId,
                        1.0f, // qualityOverride
                        type,
                        combat, // combatPts
                        freighter, // freighterPts 
                        tanker, // tankerPts
                        transport, // transportPts
                        liner, // linerPts
                        utility, // utilityPts
                        0f //-0.5f // qualityBonus
		);
//              params.timestamp = route.getTimestamp();
//		params.onlyApplyFleetSizeToCombatShips = true;
//		params.maxShipSize = 3;
                params.officerLevelBonus = 4;
		params.officerNumberMult = 0.6f;
                params.modeOverride = Misc.getShipPickMode(market);
//		params.random = random;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
                
                if (fleet == null || fleet.isEmpty()) return null;
                
                fleet.getMemoryWithoutUpdate().set("junkPirateExplorers", true);
                fleet.setName("Explorer");
                
                return fleet;
    }    

    @Override
    public void reportFleetDespawned(CampaignFleetAPI despawnedfleet, FleetDespawnReason reason, Object param) {
        
        for (JunkPiratesExplorerData thingy : activeJunkFleets) {
            if (thingy.fleet == despawnedfleet) {
                activeJunkFleets.remove(thingy);
                break;
            }
        }
        
    }
    
    public boolean shouldRepeat(RouteData route) {
            return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return false;
    }    

//    @Override
//    public void reportPlayerEngagement(EngagementResultAPI result) {
//        boolean player_won = result.didPlayerWin();
//        if (!player_won) {
//            Global.getSector().getMemoryWithoutUpdate().set("$playerIsSyndicateAspWanted", false); // given the player a hiding. Happy, for now.
//            }
//        List<FleetMemberAPI> shipsDone = new ArrayList<>();
//        shipsDone.addAll(engagementResult.getDestroyed());
//        shipsDone.addAll(engagementResult.getDisabled());
//        
//        int damageDone = 0;
//        
//        for (FleetMemberAPI ship : shipsDone) {
//            if (ship.isFighterWing()) continue;
//            damageDone += ship.getFleetPointCost();
//        }
//        
//        CustomRepImpact impact = new CustomRepImpact();
//        impact.delta = damageDone / 10;
//        
//        ReputationAdjustmentResult rep =        
//            Global.getSector().adjustPlayerReputation( // tank the relationship to all hell
//                    new RepActionEnvelope(RepActions.CUSTOM,
//                        impact, null, null ,false, true),
//                    "syndicate_asp");
//    }
    
    @Override
    public boolean isDone()
    {
        return false;
    }
    
}
