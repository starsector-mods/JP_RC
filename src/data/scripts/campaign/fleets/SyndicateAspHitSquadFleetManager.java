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
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.JunkPiratesModPlugin.junkPiratesFleetFrequencyModifier;
import static data.scripts.JunkPiratesModPlugin.junkPiratesMaxFleetModifier;
import data.scripts.campaign.fleets.SyndicateAspHitSquadFleetAssignmentAI.SyndicateAspHitSquadData;
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
public class SyndicateAspHitSquadFleetManager extends BaseCampaignEventListener implements EveryFrameScript {

    private final List<SyndicateAspHitSquadData> activeAspHitFleets = new LinkedList<>();
    private final IntervalUtil tracker;
    
//    public static final float minAspCourierSpawnInterval = 2.0f;
    
//    public static final String ASP_SOURCE_ID = "econ";
    public static final Logger log = Global.getLogger(SyndicateAspHitSquadFleetManager.class);
    
//    protected TimeoutTracker<String> recentlySentAspCourierFleet = new TimeoutTracker<String>();
    
    public SyndicateAspHitSquadFleetManager() {
        super(true);
        
        float interval = Global.getSettings().getFloat("averagePatrolSpawnInterval");
        tracker = new IntervalUtil(interval * 0.5f / junkPiratesFleetFrequencyModifier, interval * .75f / junkPiratesFleetFrequencyModifier);
        
    }
    
    protected Object readResolve() {
        Global.getSector().addTransientListener(this);
        return this;
    }
    
    @Override
    public void advance(float amount) {
        
        float days = Global.getSector().getClock().convertToDays(amount);
        
        tracker.advance(days);
        if (!tracker.intervalElapsed()) {
            return;
        }

        List<SyndicateAspHitSquadData> toRemove = new ArrayList<SyndicateAspHitSquadData>();
        for (SyndicateAspHitSquadData rd : activeAspHitFleets) {
            if (rd.fleet == null || !rd.fleet.isAlive() || rd.fleet.getContainingLocation() == null) {
                toRemove.add(rd);
            }
        }
        activeAspHitFleets.removeAll(toRemove);
        
        boolean playerIsCruel = Global.getSector().getMemoryWithoutUpdate().getBoolean("$playerIsSyndicateAspWanted");
        
        if (!playerIsCruel) { //not wanted
            return;
        }
        
        if  (!Global.getSector().getFaction("syndicate_asp").isHostileTo(Factions.PLAYER)) { // or not hostile
            return;
        }
        
        // we have determined that player is indeed cruel
        // and player is also hostile to asp
        
        boolean asp_markets = false; // check the ASP have a presence
        
        for (MarketAPI checkMarkets : Global.getSector().getEconomy().getMarketsCopy()) {
            if ("syndicate_asp".equals(checkMarkets.getFactionId())) {
                asp_markets = true; // we are still alive
            }
        }
        
        if (!asp_markets) { // if we are not then step out
            return;
        }
        
        
        addHitFleetIfPossible();
        
        
    }
    
//    protected String getRouteSourceId() {
//            return ASP_SOURCE_ID;
//    }
    
    protected int getMaxFleets() {
        int numMarkets = Global.getSector().getEconomy().getNumMarkets();
        int maxBasedOnMarket = (int) ( numMarkets * junkPiratesMaxFleetModifier / 6 ); //numMarkets * 2 is vanilla equivalent for Economy fleets. We want to be well below this.
        return Math.max(3, maxBasedOnMarket ); // probably want to externalise this in mendoncaModSettings? 3, or more in huge world
//        return 3;
    }
    
    protected void addHitFleetIfPossible() {
        
        int numFleets = activeAspHitFleets.size();
        
        if (getSyndicateAspMarketSource() != null) { // check the syndicate have a market
            log.info("Currently " + numFleets + " hit fleets operating out of max " + getMaxFleets());
            if (numFleets < getMaxFleets()) {

                MarketAPI from = getSyndicateAspMarketSource();
                MarketAPI to = pickHitDestMarket(from);
                //log.info("Trying from " + from.getName() + " to " + to.getName());
                if (from != null && to != null) {
                    if (from != to) {

                        SyndicateAspHitSquadData data = createData(from, to);

                        log.info("Added ASP hit fleet route starting from " + from.getName() + " to " + to.getName());

                        spawnFleet(data);

                    } else {
                        log.info("Forget about that then ... don't want to take between identical markets ...");
                    }
                }
            }
        }
    }
    
    public MarketAPI pickHitSourceMarket() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            if (!market.hasSpaceport()) continue;
            if (market.getFaction() != null && !"syndicate_asp".equals(market.getFaction().getId())) continue; // only get ASP Syndicate Markets
            
            float w = market.getSize() + market.getStabilityValue();
            
            markets.add(market, w); // Weights the market by market size / stability.
            
        }
        return markets.pick();
    }
    
    public MarketAPI pickHitDestMarket(MarketAPI from) {
        
        if (from == null) return null;
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            // Find markets to visit.
            if (market.isHidden()) continue;
            if (market == from) continue;

            if (!market.hasSpaceport()) continue;
            if (market.getFaction() != null && market.getFaction().isHostileTo("syndicate_asp")) continue; // only get markets who would work with the ASP Syndicate

            markets.add(market, market.getSize()); // weights the market by market size.
        }
        return markets.pick();
    }
    
    public static SyndicateAspHitSquadData createData(MarketAPI from, MarketAPI to) {
                     
        CampaignFleetAPI placeHolder = FleetFactoryV3.createEmptyFleet("syndicate_asp", FleetTypes.MERC_PRIVATEER, from);
        
        SyndicateAspHitSquadData data = new SyndicateAspHitSquadData(placeHolder); // this logic is bollocks but lets see if we can run with something for now
        
//        public String mission;
//        public float startingFP;
//        public MarketAPI from;
//        public MarketAPI to;
//                
//        public CampaignFleetAPI fleet;
        
        if (data.mission == null) data.mission = "mission_complete";
        
        data.from = from;
        data.to = to;
        
        return data;
    }
    
    public CampaignFleetAPI spawnFleet(SyndicateAspHitSquadData data) {
//        Random random = new Random();
        
        CampaignFleetAPI fleet = createAspHitFleet(data);
        
        if (fleet == null) return null;
        data.fleet = fleet;
        data.startingFP = fleet.getFleetPoints();
        
        if ("syndicate_asp_familia".equals(data.fleet.getFaction().getId())) {
            data.fleet.setFaction("syndicate_asp");
        }
        
        // fleet.addEventListener(this); // does it need a listener? Resolve with rules triggered scripts
        
        fleet.addScript(new SyndicateAspHitSquadFleetAssignmentAI(fleet, data)); //hmm is this right
        //log.info("ASP Hit fleet created... ");
        
        SectorEntityToken entity = data.from.getPrimaryEntity();
        entity.getContainingLocation().addEntity(fleet);
        fleet.setLocation(entity.getLocation().x, entity.getLocation().y);
      
        activeAspHitFleets.add(data);
        
        return fleet;
    }
    
    public void cleanFleet(SyndicateAspHitSquadData data) {
        for (SyndicateAspHitSquadData thingy : activeAspHitFleets) {
            if (data.equals(thingy)) {
                activeAspHitFleets.remove(data);
                break;
            }
        }
    }
    
    public static MarketAPI getSyndicateAspMarketSource() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            if (!market.hasSpaceport()) continue;
            if (!"syndicate_asp".equals(market.getFaction().getId())) continue; // don't get non-syndicate markets
            
            float w = market.getSize(); // weight by market size
            
            markets.add(market, w); // Weights the market by conditions. Stuff to get rid of.
            
        }
        
        if (markets.isEmpty()) {
            log.info("No syndicate markets found ...");
            return null;
        }
        MarketAPI thismarket = markets.pick();
        //log.info("Syndicate market picked :" + thismarket.getName());
        return thismarket;
    }
    
    public static String getFleetTypeIdForTier(float fp) {
        
        String type = FleetTypes.PATROL_MEDIUM;
        
        if (fp > 60) {
            type = FleetTypes.PATROL_LARGE;
        }
        
        return type;
    }
    
    public static CampaignFleetAPI createAspHitFleet(SyndicateAspHitSquadData data) {
        
        @SuppressWarnings("unused")
                MarketAPI from = data.from;
        @SuppressWarnings("unused")
                MarketAPI to = data.to;
        
        float fp_base = Global.getSector().getPlayerFleet().getFleetPoints() + 15f; // always a little bigger
        float player_level = Global.getSector().getPlayerPerson().getStats().getLevel(); // more so for higher level
        
        float fp_fleet = fp_base + ( player_level * 2.0f ); // extra boost for extra levels
        
        String factionId = "syndicate_asp_familia"; // come on it's a hit squad for Pete's sake
        
        //log.info("Creating ASP Hit Fleet starting from " + from.getName() + " to " + to.getName()); // + "PRIS:" +
                //prisoner + "; VIP:" + vip + "; ITEMS:" + items + "; MONEY:" + money + " ");
        
        float combat = fp_fleet;
        float freighter = 0f;
        float liner = 0f;
        float tanker = 0f;
        float transport = 0f;
        float utility = 0f;
        
        String type = getFleetTypeIdForTier(fp_fleet);
        
        MarketAPI market = getSyndicateAspMarketSource();
        
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
                
                fleet.getMemoryWithoutUpdate().set("$aspHitSquad", true);
                fleet.setName("Hit Squad");
                
//                if ("syndicate_asp_familia".equals(fleet.getFaction().getId())) {
//                    fleet.setFaction("syndicate_asp");
//                }
                
                return fleet;
    }    

    @Override
    public void reportFleetDespawned(CampaignFleetAPI despawnedfleet, FleetDespawnReason reason, Object param) {
        
        for (SyndicateAspHitSquadData thingy : activeAspHitFleets) {
            if (thingy.fleet == despawnedfleet) {
                activeAspHitFleets.remove(thingy);
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

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        boolean player_won = result.didPlayerWin();
        if (!player_won) {
            Global.getSector().getMemoryWithoutUpdate().set("$playerIsSyndicateAspWanted", false); // given the player a hiding. Happy, for now.
            }
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
    }
    
    @Override
    public boolean isDone()
    {
        return false;
    }
    
}
