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
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.ArrayList;
import java.util.List;
import data.campaign.intel.misc.SyndicateAspCourierDepartureIntel;
import static data.scripts.JunkPiratesModPlugin.enableJunkPiratesIntel;
import static data.scripts.JunkPiratesModPlugin.junkPiratesFleetFrequencyModifier;
import static data.scripts.JunkPiratesModPlugin.junkPiratesMaxFleetModifier;
import data.scripts.campaign.fleets.SyndicateAspCourierFleetAssignmentAI.SyndicateAspCourierRouteData;
import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 *
 * @author paul - sets up a point to point courier fleet carrying valuable cargo.
 * Interception should have significant consequences in relations for customer and ASP. NOT APPLIED
 * But potential for high value rewards.
 */
public class SyndicateAspFleetManager extends BaseCampaignEventListener implements EveryFrameScript {

    private List<SyndicateAspCourierRouteData> activeAspFleets = new LinkedList<>();
    private IntervalUtil tracker;
    transient private boolean hasRunSweep = false;
    
//    public static final float minAspCourierSpawnInterval = 2.0f;
    
//    public static final String ASP_SOURCE_ID = "econ";
    public static final Logger log = Global.getLogger(SyndicateAspFleetManager.class);
    
//    protected TimeoutTracker<String> recentlySentAspCourierFleet = new TimeoutTracker<String>();
    
    public SyndicateAspFleetManager() {
        super(true);
    }
    
    public IntervalUtil getTracker() {
        if (tracker == null) {
            float interval = Global.getSettings().getFloat("averagePatrolSpawnInterval");
            tracker = new IntervalUtil(interval * 0.45f / junkPiratesFleetFrequencyModifier, interval * .75f / junkPiratesFleetFrequencyModifier);
        }
        return tracker;
    }
    
    protected Object readResolve() {
        Global.getSector().addTransientListener(this);
        getTracker();
        
        if (activeAspFleets == null) activeAspFleets = new java.util.LinkedList<>();
        
        return this;
    }
    
    @Override
    public void advance(float amount) {
        if (!hasRunSweep) {
            if (activeAspFleets == null) activeAspFleets = new java.util.LinkedList<>();
            if (activeAspFleets.isEmpty()) {
                for (com.fs.starfarer.api.campaign.LocationAPI loc : Global.getSector().getAllLocations()) {
                    for (com.fs.starfarer.api.campaign.CampaignFleetAPI f : loc.getFleets()) {
                        if (f != null && f.getMemoryWithoutUpdate() != null && 
                            (f.getMemoryWithoutUpdate().getBoolean("aspCourierFleetITEMS") ||
                             f.getMemoryWithoutUpdate().getBoolean("aspCourierFleetMONEY") ||
                             f.getMemoryWithoutUpdate().getBoolean("aspCourierFleetVIP") ||
                             f.getMemoryWithoutUpdate().getBoolean("aspCourierFleetPRISONER"))) {
                            boolean found = false;
                            for (SyndicateAspCourierRouteData rd : activeAspFleets) {
                                if (rd.fleet == f) { found = true; break; }
                            }
                            if (!found) {
                                activeAspFleets.add(new SyndicateAspCourierRouteData(f));
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

        List<SyndicateAspCourierRouteData> toRemove = new ArrayList<SyndicateAspCourierRouteData>();
        for (SyndicateAspCourierRouteData rd : activeAspFleets) {
            if (rd.fleet == null || !rd.fleet.isAlive() || rd.fleet.getContainingLocation() == null) {
                toRemove.add(rd);
            }
        }
        activeAspFleets.removeAll(toRemove);
        
        boolean asp_markets = false;
        
        for (MarketAPI checkMarkets : Global.getSector().getEconomy().getMarketsCopy()) {
            if ("syndicate_asp".equals(checkMarkets.getFactionId())) {
                asp_markets = true; // we are still alive
            }
        }
        
        if (!asp_markets) { // no we are not
            return;
        }
        
        addRouteFleetIfPossible();
        
        
    }
    
//    protected String getRouteSourceId() {
//            return ASP_SOURCE_ID;
//    }
    
    protected int getMaxFleets() {
        int numMarkets = 0; for(com.fs.starfarer.api.campaign.econ.MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) { if("syndicate_asp".equals(m.getFactionId())) numMarkets++; }
        int maxBasedOnMarket = (int) ( numMarkets * junkPiratesMaxFleetModifier/ 4 ); //numMarkets * 2 is vanilla equivalent for Economy fleets. We want to be well below this.
        return maxBasedOnMarket; // probably want to externalise this in mendoncaModSettings
    }
    
    protected void addRouteFleetIfPossible() {
        
        int numFleets = activeAspFleets.size();
        
        if (getSyndicateAspMarketSource() != null) { // check the syndicate have a market
        
            if (numFleets < getMaxFleets()) {
                log.info("There are " + numFleets + " Courier Fleets running out of a maximum " + getMaxFleets());
                MarketAPI from = pickSourceMarket();
                MarketAPI to = pickDestMarket(from);
                if (from != null && to != null) {
                    if (from != to) {

                        SyndicateAspCourierRouteData data = createData(from, to);

                        //log.info("Added ASP courier fleet route from " + from.getName() + " to " + to.getName());
                        //log.info("The fellas are mucking about with " + data.mission);

                        if (data.fleet != null && !Factions.PLAYER.equals(data.from.getFactionId())) {
                            // queues itself; don't do ones running from Player Colonies
                            if (enableJunkPiratesIntel) {
                                new SyndicateAspCourierDepartureIntel(data);
                            }
                        }
                        
                        spawnFleet(data);
                        


                    } else {
                        log.info("Forget about that then ... don't want to take between identical markets ...");
                    }
                }
            }
        }
    }
    
    public MarketAPI pickSourceMarket() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            if (market.getPrimaryEntity() == null) continue;
            if (!market.hasSpaceport()) continue;
            if (market.getFaction() != null && market.getFaction().isHostileTo("syndicate_asp")) continue; // only get markets who would work with the ASP Syndicate
            //if (SharedData.getData().getMarketsWithoutTradeFleetSpawn().contains(market.getId())) continue; // Can come from shitty worlds
            
//            if (recentlySentAspCourierFleet.contains(market.getId())) continue;
            
            float w = 0.5f; // start with a non-zero weight
            
            if (market.isFreePort()) w += 2; // prefer dodgy markets
            if (market.getStabilityValue() < 6.0f) w += 1;
            if (market.getStabilityValue() < 3.0f) w += 1;
            if (market.getStabilityValue() < 1.0f) w += 1; // low stability planets want things getting rid of
            if (market.getNetIncome() < 0f) w += 2; // economy is fucked, stuff needs to leave
            // other thoughts ... add weights based on conditions? Externalise this?
            if (market.hasCondition(Conditions.FRONTIER)) w +=2;
            if (market.hasCondition(Conditions.CLOSED_IMMIGRATION)) w +=1;
            if (market.hasCondition(Conditions.PATHER_CELLS)) w +=1;
            if (market.hasCondition(Conditions.RECENT_UNREST)) w +=3; // instability makes things leave
            if (market.hasCondition(Conditions.LARGE_REFUGEE_POPULATION)) w +=1;
            if (market.hasCondition(Conditions.PIRATE_ACTIVITY)) w +=1;
            if (market.hasCondition(Conditions.ORGANIZED_CRIME)) w +=3; // prefer dodgy markets
            
            markets.add(market, w); // Weights the market by conditions. Stuff to get rid of.
            
        }
        return markets.pick();
    }
    
    public MarketAPI pickDestMarket(MarketAPI from) {
        
        if (from == null) return null;
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            // need some logic to determine what the cargo is ...
            if (market.isHidden()) continue;
            if (market.getPrimaryEntity() == null) continue;
            if (!market.hasSpaceport()) continue;
            if (market.getFaction() != null && market.getFaction().isHostileTo("syndicate_asp")) continue; // only get markets who would work with the ASP Syndicate

            markets.add(market, market.getSize()); // weights the market by market size ... not necessarily what we want? Maybe for destination?
        }
        return markets.pick();
    }
    
    public static SyndicateAspCourierRouteData createData(MarketAPI from, MarketAPI to) {
        
        // do we care what is on the market? Or just conditions?
        
        float prisonerChance = 0f;
        float vipChance = 0f;
        //float itemsChance = 5f; //lets fall back on items
        float moneyChance = 2f; //small chance on mundane market that we are happy shifting money
        
        // Get some functions in place to assess what the courier is up to.
        
        if (from.hasCondition(Conditions.CLOSED_IMMIGRATION)) { // this should be smarter to allow for impact of modded industries
            prisonerChance += 1f;
            vipChance += 3f;
        }
        
        if (from.hasCondition(Conditions.DISSIDENT)) {
            prisonerChance += 3f;
            vipChance += 1f;
        }
        
        if (from.hasCondition(Conditions.FREE_PORT)) {
            //itemsChance += 1f;
            moneyChance += 1f;
        }
        
        if (from.hasCondition(Conditions.PATHER_CELLS)) {
            prisonerChance += 1f;
            vipChance += 1f;
            moneyChance += 1f;
        }
        
        if (from.hasCondition(Conditions.PIRATE_ACTIVITY)) {
            prisonerChance += 1f;
            vipChance += 3f;
            moneyChance += 1f;
        }
        
        if (from.hasCondition(Conditions.ORGANIZED_CRIME)) {
            prisonerChance += 1f;
            vipChance += 1f;
            moneyChance += 3f;
        }
        
//        HashMap courierPriority = new HashMap<>();
//        
//        courierPriority.put("prisonerChance", prisonerChance);
//        courierPriority.put("vipChance", vipChance);
//        courierPriority.put("itemsChance", itemsChance);
//        courierPriority.put("moneyChance", moneyChance);

        // What do we want to know? We want to know what mission this thing is doing.
        
        float prisonerRoll = (float) Math.random() * 10; // we really should weight this?
        float vipRoll = (float) Math.random() * 10;
        //float itemsRoll = (float) Math.random() * 10;
        float moneyRoll = (float) Math.random() * 10;
        
        CampaignFleetAPI placeHolder = FleetFactoryV3.createEmptyFleet("syndicate_asp", FleetTypes.TRADE_SMALL, from);
        
        SyndicateAspCourierRouteData data = new SyndicateAspCourierRouteData(placeHolder); // this logic is bollocks but lets see if we can run with something for now
        
//        if (itemsRoll < itemsChance) { // set items to true; this cargo is just items
//            data.items = true;
//        }
//        Always smuggling? No place for it in this logic just yet
        
        data.mission = "items";
        data.cargotype = "items";

        if (moneyRoll < moneyChance) { // Roll for money first ... doing mutually exclusive and unique memflags for rules
                data.mission = "money";
                data.cargotype = "money";
        }
        
        if (prisonerRoll < prisonerChance) { // if not money can be prisoner
            if(!"money".equals(data.mission)) {
                data.mission = "prisoner";
                data.cargotype = "prisoner";
            }
        }
        
        if (vipRoll < vipChance) { // can overwrite money but not prisoner
            if(!"prisoner".equals(data.mission)) {
                data.mission = "vip"; 
                data.cargotype = "vip"; 
            }
        }
        
//        if ("money".equals(data.mission) ||
//                "vip".equals(data.mission) ||
//                "prisoner".equals(data.mission) ) {
//            data.mission = "items";
//        }
        
        data.size = to.getSize(); // size of contract based on market where this is being taken
        data.customerFaction = to.getFactionId();
        
        data.from = from;
        data.to = to;
        
        return data;
    }
    
    public CampaignFleetAPI spawnFleet(SyndicateAspCourierRouteData data) {
//        Random random = new Random();
        
        CampaignFleetAPI fleet = createAspCourierFleet(data);
        
        if (fleet == null || data.from == null || data.to == null || data.cargotype == null ||
            data.from.getPrimaryEntity() == null || data.from.getPrimaryEntity().getContainingLocation() == null || 
            data.from.getPrimaryEntity().getLocation() == null) return null;
        
        data.fleet = fleet;
        
        if (data.fleet.getFaction() != null && "syndicate_asp_familia".equals(data.fleet.getFaction().getId())) {
            data.fleet.setFaction("syndicate_asp");
        } // do this before add to list otherwise change list and don't recognise it when despawning
        
        fleet.addScript(new SyndicateAspCourierFleetAssignmentAI(fleet, data)); //hmm is this right
        //log.info("ASP Courier fleet created... ");
        
        SectorEntityToken entity = data.from.getPrimaryEntity();
        entity.getContainingLocation().addEntity(fleet);
        fleet.setLocation(entity.getLocation().x, entity.getLocation().y);
        
        activeAspFleets.add(data);
        
        return fleet;
    }
    
    public void cleanFleet(SyndicateAspCourierRouteData data) { // not doing anything see reason to despawn ... keep in case need to purge list?
        for (SyndicateAspCourierRouteData thingy : activeAspFleets) {
            if (data.equals(thingy)) {
                activeAspFleets.remove(data);
                break;
            }
        }
    }
    
    public static MarketAPI getSyndicateAspMarketSource() {
        
        WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<MarketAPI>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            if (!market.hasSpaceport()) continue;
            if (!"syndicate_asp".equals(market.getFactionId())) continue; // don't get non-syndicate markets
            
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
    
    public static String getFleetTypeIdForTier(float tier, boolean smuggling) {
        
        String type = FleetTypes.TRADE_SMALL;
        
        if (tier > 4) {
            type = FleetTypes.TRADE;
        }
        
        return type;
    }
    
    public static CampaignFleetAPI createAspCourierFleet(SyndicateAspCourierRouteData data) {
        
        if (data.from == null || data.to == null) return null;
        
        MarketAPI from = data.from;
        MarketAPI to = data.to;
        
        float tier = data.size;
        
        if (data.smuggling && tier > 3) {
            tier = 3;
        }
        
        String factionId = "syndicate_asp";
        data.customerFaction = to.getFactionId();
        boolean prisoner = false;
        boolean vip = false;
        @SuppressWarnings("unused")
                boolean items = false;
        boolean money = false;
        
        
        if ("prisoner".equals(data.mission) ) { prisoner = true; }
        if ("items".equals(data.mission) ) { items = true; }
        if ("money".equals(data.mission) ) { money = true; }
        if ("vip".equals(data.mission) ) { vip = true; }
        
        // get a cleverer, more nuanced way of deciding whether the Familia are involved
        // Might want a memory tag on this fleet if they are, after we snap them back to pure ASP?
        
        if (prisoner) {
            factionId = "syndicate_asp_familia"; // we'll want to move this back after generating the fleet
        }
        
        log.info("Creating ASP Courier Fleet from " + from.getName() + " to " + to.getName()); // + "PRIS:" +
                //prisoner + "; VIP:" + vip + "; ITEMS:" + items + "; MONEY:" + money + " ");
        
        float combat = tier * 3f;
        float freighter = tier * 2f;
        float tanker = tier * 2f;
        float transport = 0f;
        float liner = 0f;
        
        float utility = 0f;
        
        if (prisoner) { // go heavy on combat
            combat = Math.max(4f, tier) * 5f; // minimum 20, up to Tier x 5
            freighter = 0f;
        }
        
        if (vip) { // give them some luxury - the numbnuts thinks they are invincible
            liner = Math.max(4f, tier) * 5f; // minimum 20, up to Tier x 5
            combat = tier * 2f;
        }
        
        String type = getFleetTypeIdForTier(tier, data.smuggling);
        if (data.smuggling) {
            combat += 1f;
            // nothing to see here, not yet triggered
        }
        
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
//                params.timestamp = route.getTimestamp();
		params.onlyApplyFleetSizeToCombatShips = true;
//		params.maxShipSize = 3;
                if (vip || prisoner) {
                    params.officerLevelBonus = 4;
                } else {
                    params.officerLevelBonus = 0;
                }
                params.modeOverride = Misc.getShipPickMode(market);
		params.officerNumberMult = 0.6f;
//		params.random = random;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
                
                if (fleet == null || fleet.isEmpty()) return null;
                
                if (vip) {
                    fleet.getMemoryWithoutUpdate().set("aspCourierFleetVIP", true);
                    fleet.setName("Pleasure Cruise");
                } else if (prisoner) {
                    fleet.getMemoryWithoutUpdate().set("aspCourierFleetPRISONER", true);
                    fleet.setName("Armed Guard");
                } else if (money) {
                    fleet.getMemoryWithoutUpdate().set("aspCourierFleetMONEY", true);
                    fleet.setName("Courier");
                } else {
                    fleet.getMemoryWithoutUpdate().set("aspCourierFleetITEMS", true);
                    fleet.setName("Courier");
                }
                
                //is this the place to add cargo / loot? Possibly now whilst we've got Market info ...
                
                // need to normalise faction; flip back from the FAMILY
//                if ("syndicate_asp_familia".equals(fleet.getFaction().getId())) {
//                    fleet.setFaction("syndicate_asp");
//                }
                
                data.startingFP = fleet.getFleetPoints();
                data.cargoCap = fleet.getCargo().getMaxCapacity();
		data.fuelCap = fleet.getCargo().getMaxFuel();
		data.personnelCap = fleet.getCargo().getMaxPersonnel();
                
                return fleet;
    }    

    @Override
    public void reportFleetDespawned(CampaignFleetAPI despawnedfleet, FleetDespawnReason reason, Object param) {
        
        for (SyndicateAspCourierRouteData thingy : activeAspFleets) {
            if (thingy.fleet == despawnedfleet) {
                activeAspFleets.remove(thingy);
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
        if (player_won) {
            boolean foughtAsp = false;
            if (result.getBattle() != null) {
                for (CampaignFleetAPI f : result.getBattle().getNonPlayerSide()) {
                    if (f.getFaction() != null && "syndicate_asp".equals(f.getFaction().getId()) && !f.getMemoryWithoutUpdate().getBoolean("$aspHitSquad")) {
                        foughtAsp = true;
                        break;
                    }
                }
            } else if (result.getLoserResult() != null && result.getLoserResult().getFleet() != null) {
                if (result.getLoserResult().getFleet().getFaction() != null && "syndicate_asp".equals(result.getLoserResult().getFleet().getFaction().getId()) && !result.getLoserResult().getFleet().getMemoryWithoutUpdate().getBoolean("$aspHitSquad")) {
                    foughtAsp = true;
                }
            }
            if (foughtAsp) {
                Global.getSector().getMemoryWithoutUpdate().set("$playerIsSyndicateAspWanted", true);
            }
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
