package data.campaign.econ.impl;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteFleetSpawner;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;


public class FamiliaHQ extends BaseIndustry implements RouteFleetSpawner, FleetEventListener {
	
	@Override
	public boolean isHidden() {
		return false;
	}
	
	@Override
	public boolean isFunctional() {
		return super.isFunctional() && market != null && market.getFactionId() != null && "syndicate_asp".equals(market.getFactionId());
	}

	@Override
	public boolean canShutDown() {
		return super.canShutDown() || (market != null && (!"syndicate_asp".equals(market.getFactionId()) || market.isPlayerOwned()));
	}

	public void apply() {
		super.apply(true);
		
		if (market == null) return;
		
		int size = market.getSize();
		
		demand(Commodities.SUPPLIES, size - 1);
		demand(Commodities.FUEL, size - 1);
		demand(Commodities.SHIPS, size - 1);
		
		supply(Commodities.CREW, size);
		
		demand(Commodities.HAND_WEAPONS, size);
		supply(Commodities.MARINES, size);
			
		Pair<String, Integer> deficit = getMaxDeficit(Commodities.HAND_WEAPONS);
		applyDeficitToProduction(1, deficit, Commodities.MARINES);
		
		modifyStabilityWithBaseMod();
		
		// Register patrol fleet caps so getMaxPatrols() and skills/governors work correctly
		if (market.getStats() != null && market.getStats().getDynamic() != null) {
			market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).modifyFlat(getModId(), 3);
			market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).modifyFlat(getModId(), 2);
			market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).modifyFlat(getModId(), 1);
		}
		
		MemoryAPI memory = market.getMemoryWithoutUpdate();
		if (memory != null) {
			Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, getModId(), true, -1);
			Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, getModId(), true, -1);
		}
		
		if (!isFunctional()) {
			supply.clear();
			demand.clear();
			unapply();
			return;
		}

	}

	@Override
	public void unapply() {
		super.unapply();
		
		if (market != null) {
			if (market.getStats() != null && market.getStats().getDynamic() != null) {
				market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).unmodify(getModId());
				market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).unmodify(getModId());
				market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).unmodify(getModId());
			}
			
			MemoryAPI memory = market.getMemoryWithoutUpdate();
			if (memory != null) {
				Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, getModId(), false, -1);
				Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, getModId(), false, -1);
			}
		}
		
		unmodifyStabilityWithBaseMod();
	}
	
	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			addStabilityPostDemandSection(tooltip, hasDemand, mode);
		}
	}
	
	@Override
	protected int getBaseStabilityMod() {
		return 2;
	}
	
	public String getNameForModifier() {
		if (getSpec() != null && getSpec().getName() != null) {
			if (getSpec().getName().contains("HQ")) {
				return getSpec().getName();
			}
			return Misc.ucFirst(getSpec().getName());
		}
		return "Familia HQ";
	}
	
	@Override
	protected Pair<String, Integer> getStabilityAffectingDeficit() {
		return getMaxDeficit(Commodities.SUPPLIES, Commodities.FUEL, Commodities.SHIPS, Commodities.HAND_WEAPONS);
	}
	
	@Override
	public String getCurrentImage() {
		return super.getCurrentImage();
	}

	
	public boolean isDemandLegal(CommodityOnMarketAPI com) {
		return true;
	}

	public boolean isSupplyLegal(CommodityOnMarketAPI com) {
		return true;
	}

	protected IntervalUtil tracker = null;

	protected IntervalUtil getTracker() {
		if (tracker == null) {
			float base = Global.getSettings().getFloat("averagePatrolSpawnInterval");
			tracker = new IntervalUtil(base * 0.7f, base * 1.3f);
		}
		return tracker;
	}
	
	protected float returningPatrolValue = 0f;

	@Override
	protected Object readResolve() {
		super.readResolve();
		return this;
	}
	
	@Override
	protected void buildingFinished() {
		super.buildingFinished();
		
		getTracker().forceIntervalElapsed();
	}
	
	@Override
	protected void upgradeFinished(Industry previous) {
		super.upgradeFinished(previous);
		
		getTracker().forceIntervalElapsed();
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		
		if (Global.getSector() == null || Global.getSector().getEconomy() == null || Global.getSector().getEconomy().isSimMode()) return;

		if (!isFunctional() || market == null) return;
		
		float days = Global.getSector().getClock().convertToDays(amount);
		
		float spawnRate = 1f;
		if (market.getStats() != null && market.getStats().getDynamic() != null) {
			float rateMult = market.getStats().getDynamic().getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).getModifiedValue();
			spawnRate *= rateMult;
		}
		
		float extraTime = 0f;
		if (returningPatrolValue > 0) {
			// apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
			float interval = getTracker().getIntervalDuration();
			extraTime = interval * days;
			returningPatrolValue -= days;
			if (returningPatrolValue < 0) returningPatrolValue = 0;
		}
		getTracker().advance(days * spawnRate + extraTime);
		
		if (getTracker().intervalElapsed()) {
			String sid = getRouteSourceId();
			
			int light = getCount(PatrolType.FAST);
			int medium = getCount(PatrolType.COMBAT);
			int heavy = getCount(PatrolType.HEAVY);

			int maxLight  = getMaxPatrols(PatrolType.FAST);
			int maxMedium = getMaxPatrols(PatrolType.COMBAT);
			int maxHeavy  = getMaxPatrols(PatrolType.HEAVY);
			
			WeightedRandomPicker<PatrolType> picker = new WeightedRandomPicker<PatrolType>();
			picker.add(PatrolType.HEAVY, maxHeavy - heavy); 
			picker.add(PatrolType.COMBAT, maxMedium - medium); 
			picker.add(PatrolType.FAST, maxLight - light); 
			
			if (picker.isEmpty()) return;
			
			PatrolType type = picker.pick();
			PatrolFleetData custom = new PatrolFleetData(type);
			
			OptionalFleetData extra = new OptionalFleetData(market);
			extra.fleetType = type.getFleetType();
			
			if (RouteManager.getInstance() == null) return;
			RouteData route = RouteManager.getInstance().addRoute(sid, market, Misc.genRandomSeed(), extra, this, custom);
			if (route == null) return;
			float patrolDays = 35f + (float) Math.random() * 10f;
			
			if (market.getPrimaryEntity() != null) {
				route.addSegment(new RouteSegment(patrolDays, market.getPrimaryEntity()));
			}
		}
	}

	
	public void reportAboutToBeDespawnedByRouteManager(RouteData route) {
	}
	
	public boolean shouldRepeat(RouteData route) {
		return false;
	}
	
	public int getCount(PatrolType ... types) {
		if (types == null || types.length == 0 || market == null || RouteManager.getInstance() == null) return 0;
		int count = 0;
		java.util.List<RouteData> routes = RouteManager.getInstance().getRoutesForSource(getRouteSourceId());
		if (routes == null) return 0;
		for (RouteData data : routes) {
			if (data != null && data.getCustom() instanceof PatrolFleetData) {
				PatrolFleetData custom = (PatrolFleetData) data.getCustom();
				if (custom == null) continue;
				for (PatrolType type : types) {
					if (type == custom.type) {
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	public int getMaxPatrols(PatrolType type) {
		if (!isFunctional() || market == null) return 0;
		switch (type) {
			case FAST: return 2;
			case COMBAT: return 1;
			case HEAVY: return market.getSize() >= 6 ? 1 : 0;
			default: return 0;
		}
	}
	
	public boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		return false;
	}

	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		
	}



	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		if (!isFunctional() || fleet == null || reason == null || market == null || RouteManager.getInstance() == null) return;
		
		if (reason == FleetDespawnReason.REACHED_DESTINATION) {
			RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
			if (route != null && route.getCustom() instanceof PatrolFleetData) {
				PatrolFleetData custom = (PatrolFleetData) route.getCustom();
				if (custom != null && custom.spawnFP > 0) {
					float fraction = fleet.getFleetPoints() / custom.spawnFP;
					returningPatrolValue += fraction;
				}
			}
		}
	}
	
	public CampaignFleetAPI spawnFleet(RouteData route) {
		if (!isFunctional()) return null;
		if (route == null || !(route.getCustom() instanceof PatrolFleetData)) return null;
		if (market == null || market.getContainingLocation() == null || market.getPrimaryEntity() == null || market.getFactionId() == null) return null;
		
		PatrolFleetData custom = (PatrolFleetData) route.getCustom();
		PatrolType type = custom.type;
		if (type == null) return null;
		
		Random random = route.getRandom();
		if (random == null) random = new Random();
		
		float combat = 0f;
		float tanker = 0f;
		float freighter = 0f;
		String fleetType = type.getFleetType();
		switch (type) {
		case FAST:
			combat = Math.round(3f + random.nextFloat() * 2f) * 5f;
			break;
		case COMBAT:
			combat = Math.round(6f + random.nextFloat() * 3f) * 5f;
			tanker = Math.round(random.nextFloat()) * 5f;
			break;
		case HEAVY:
			combat = Math.round(10f + random.nextFloat() * 5f) * 5f;
			tanker = Math.round(random.nextFloat()) * 10f;
			freighter = Math.round(random.nextFloat()) * 10f;
			break;
		}
		
		FleetParamsV3 params = new FleetParamsV3(
				market, 
				null, // loc in hyper; don't need if have market
				"syndicate_asp_familia",
				route.getQualityOverride(), // quality override
				fleetType,
				combat, // combatPts
				freighter, // freighterPts 
				tanker, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f // qualityMod - since the Familia is in a different-faction market, counter that penalty
				);
		params.timestamp = route.getTimestamp();
		params.random = random;
		params.modeOverride = Misc.getShipPickMode(market);
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		
		if (fleet == null || fleet.isEmpty()) return null;
		
		fleet.setFaction(market.getFactionId(), true);
		fleet.setNoFactionInName(true);
		
		fleet.addEventListener(this);
		
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);

		if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true);
		}
		
		String postId = Ranks.POST_PATROL_COMMANDER;
		String rankId = Ranks.SPACE_COMMANDER;
		switch (type) {
		case FAST:
			rankId = Ranks.SPACE_LIEUTENANT;
			break;
		case COMBAT:
			rankId = Ranks.SPACE_COMMANDER;
			break;
		case HEAVY:
			rankId = Ranks.SPACE_CAPTAIN;
			break;
		}
		
		if (fleet.getCommander() != null) {
			fleet.getCommander().setPostId(postId);
			fleet.getCommander().setRankId(rankId);
		}
		
		market.getContainingLocation().addEntity(fleet);
		fleet.setFacing((float) Math.random() * 360f);
		// this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
		if (market.getPrimaryEntity().getLocation() != null) {
			fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
		}
		
		fleet.addScript(new PatrolAssignmentAIV4(fleet, route));
		
		if (custom.spawnFP <= 0) {
			custom.spawnFP = fleet.getFleetPoints();
		}
		
		return fleet;
	}
	
	public String getRouteSourceId() {
		return (getMarket() == null ? "null" : getMarket().getId()) + "_" + "familia";
	}

	@Override
	public boolean isAvailableToBuild() {
		if (market == null || market.getFactionId() == null) return false;
		return "syndicate_asp".equals(market.getFactionId()) && market.getSize() >= 4;
	}
	
	public boolean showWhenUnavailable() {
		return false;
	}
	
	
}

