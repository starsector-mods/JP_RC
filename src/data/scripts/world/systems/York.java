package data.scripts.world.systems;

import java.awt.Color;
import java.util.Collections;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator.RemnantSystemType;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

public class York {

	public void generate(SectorAPI sector) {
		
		StarSystemAPI system = sector.createStarSystem("York");
		
		// Hidden far in the deep southeastern fringe of the Persean Sector
		system.getLocation().set(18500f, -14500f);
		
		system.setBackgroundTextureFilename("graphics/backgrounds/background6.jpg");
		
		// create the star and generate the hyperspace anchor for this system
		PlanetAPI star = system.initStar("york", // unique id for this star 
										 "star_yellow", // id in planets.json
										 450f, 250); 		// radius (in pixels at default zoom)
		
		system.setLightColor(new Color(245, 240, 220)); // Warm, inviting solar light

		// GLOBAL PARALLAX BACKGROUND
		system.addRingBand(star, "misc", "rings_dust0", 256f, 1, new Color(255, 255, 255, 80), 800f, 4000, 400f);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 2, new Color(220, 240, 255, 60), 1024f, 7000, 550f);
		system.addRingBand(star, "misc", "rings_ice0", 256f, 0, new Color(255, 255, 255, 45), 1200f, 10000, 700f);

		// Planet 1: Marple (Heavy Mining World)
		PlanetAPI york1 = system.addPlanet("marple", star, "Marple", "rocky_metallic", 270, 80, 1000, 120);
		york1.setCustomDescriptionId("planet_yorkI");
		york1.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		york1.getMarket().addCondition(Conditions.ORE_ULTRARICH);
		york1.getMarket().addCondition(Conditions.RARE_ORE_RICH);
		york1.getMarket().addCondition(Conditions.LOW_GRAVITY);

		// Planet 2: Appley Bridge (Cryo & Volatiles World)
		PlanetAPI york2 = system.addPlanet("appley_bridge", star, "Appley Bridge", "rocky_ice", 120, 95, 1850, 90);
		york2.setCustomDescriptionId("planet_yorkII");
		york2.getMarket().addCondition(Conditions.COLD);
		york2.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
		york2.getMarket().addCondition(Conditions.VOLATILES_ABUNDANT);
		york2.getMarket().addCondition(Conditions.ORE_MODERATE);

		// Planet 3: Lincoln (The Crown Jewel - Terran Garden Settlement World)
		PlanetAPI york3 = system.addPlanet("lincoln", star, "Lincoln", "terran", 120, 150, 3600, 130);
		york3.setCustomDescriptionId("planet_yorkIII");
		york3.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "volturn"));
		york3.getSpec().setGlowColor(new Color(245, 255, 250, 35)); // Faint, subtle pre-collapse ruins glow
		york3.applySpecChanges();
		york3.getMarket().addCondition(Conditions.HABITABLE);
		york3.getMarket().addCondition(Conditions.MILD_CLIMATE);
		york3.getMarket().addCondition(Conditions.FARMLAND_BOUNTIFUL);
		york3.getMarket().addCondition(Conditions.ORGANICS_ABUNDANT);
		york3.getMarket().addCondition(Conditions.WATER_SURFACE);
		york3.getMarket().addCondition(Conditions.RUINS_SCATTERED);

		system.addRingBand(york3, "misc", "rings_special0", 256f, 2, Color.white, 256f, 400, 40f);
		system.addRingBand(york3, "misc", "rings_special0", 256f, 2, Color.white, 256f, 420, 60f);

		// Abandoned Player Base: Lincoln Cathedral (Free Storage & Safe Anchorage)
		SectorEntityToken station = system.addCustomEntity("lincoln_cathedral", "Lincoln Cathedral", "station_side02", Factions.NEUTRAL);
		station.setCircularOrbitPointingDown(system.getEntityById("lincoln"), 45, 300, 50);
		station.setCustomDescriptionId("station_lincoln_cathedral");
		// station.setInteractionImage("illustrations", "abandoned_station3");
		Misc.setAbandonedStationMarket("lincoln_cathedral_market", station);

		// Abandoned Comm Relay (Can be restored by player)
		SectorEntityToken relay = system.addCustomEntity("york_relay", "Derelict Comm Relay", "comm_relay", Factions.NEUTRAL);
		relay.setCircularOrbit(system.getEntityById("lincoln"), 350, 1200, 60);

		// Planet 4: Lancaster (Gas Giant with Tilted 3D Rings)
		PlanetAPI york4 = system.addPlanet("york_lancaster", star, "Lancaster", "gas_giant", 15, 280, 8500, 350);
		york4.setCustomDescriptionId("planet_yorkIV");
		york4.getSpec().setPlanetColor(new Color(215, 225, 115, 255));
		york4.getSpec().setAtmosphereColor(new Color(160, 185, 45, 140));
		york4.getSpec().setCloudColor(new Color(195, 155, 115, 200));
		york4.getSpec().setTilt(45);
		york4.applySpecChanges();
		york4.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		york4.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		york4.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);

		// Lancaster 3D Tilted Ring Bands
		system.addRingBand(york4, "misc", "rings_dust0", 256f, 2, new Color(215, 225, 115, 180), 300f, 600, 40f);
		system.addRingBand(york4, "misc", "rings_ice0", 256f, 1, new Color(195, 155, 115, 150), 400f, 750, 50f);
		system.addRingBand(york4, "misc", "rings_dust0", 256f, 0, new Color(160, 185, 45, 120), 500f, 950, 65f);

		// Moon 4a: Kendal (Frozen Moon)
		PlanetAPI york4a = system.addPlanet("york_kendal", york4, "Kendal", "rocky_ice", 290, 55, 800, 45);
		york4a.setCustomDescriptionId("planet_yorkIVa");
		york4a.getMarket().addCondition(Conditions.COLD);
		york4a.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		york4a.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
		york4a.getMarket().addCondition(Conditions.ORE_SPARSE);

		// Moon 4b: Halifax (Pre-Collapse Hab-Tubes Moon)
		PlanetAPI york4b = system.addPlanet("york_halifax", york4, "Halifax", "rocky_metallic", 280, 65, 1000, 55);
		york4b.setCustomDescriptionId("planet_yorkIVb");
		york4b.getMarket().addCondition(Conditions.LOW_GRAVITY);
		york4b.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		york4b.getMarket().addCondition(Conditions.ORE_RICH);
		york4b.getMarket().addCondition(Conditions.RARE_ORE_MODERATE);
		york4b.getMarket().addCondition("JUNK_habTubes");

		// Primary Jump Point
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("york_gate", "York Gateway");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(york3, 0, 650, 40);
		jumpPoint.setOrbit(orbit);
		jumpPoint.setRelatedPlanet(york3);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		// Generates hyperspace destinations for in-system jump points
		system.autogenerateHyperspaceJumpPoints(true, true);

		// Spawn several giant Remnant drone fleets guarding the system
		spawnRemnants(system);

		cleanup(system);
	}

	public static void spawnRemnants(StarSystemAPI system) {
		if (system == null) return;
		if (system.getMemoryWithoutUpdate().getBoolean("$yorkRemnantsSpawned")) return;
		system.getMemoryWithoutUpdate().set("$yorkRemnantsSpawned", true);

		system.addTag(Tags.THEME_REMNANT_RESURGENT);
		system.addTag(Tags.THEME_UNSAFE);

		// Warning beacon in hyperspace indicating high-danger Remnant presence
		try {
			boolean hasBeacon = false;
			if (Global.getSector() != null && Global.getSector().getHyperspace() != null) {
				for (Object obj : Global.getSector().getHyperspace().getEntities(CustomCampaignEntityAPI.class)) {
					CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) obj;
					if (Entities.WARNING_BEACON.equals(entity.getCustomEntityType())) {
						if (entity.getOrbit() != null && entity.getOrbit().getFocus() == system.getHyperspaceAnchor()) {
							hasBeacon = true;
							break;
						}
					}
				}
			}
			if (!hasBeacon) {
				RemnantThemeGenerator.addBeacon(system, RemnantSystemType.RESURGENT);
			}
		} catch (Exception e) {
			Global.getLogger(York.class).warn("Failed to generate hyperspace warning beacon for York", e);
		}

		SectorEntityToken star = system.getEntityById("york");
		SectorEntityToken lincoln = system.getEntityById("lincoln");
		SectorEntityToken lancaster = system.getEntityById("york_lancaster");
		SectorEntityToken gate = system.getEntityById("york_gate");

		// Fleet 1: Lincoln Heavy Defense Ordo (Guarding Lincoln & Lincoln Cathedral)
		spawnRemnantFleet(system, lincoln, "radiant_Assault", 260f,
				FleetAssignment.PATROL_SYSTEM, lincoln, "defending Lincoln and orbital facilities");

		// Fleet 2: Gateway Interception Battlegroup (Guarding York Gateway Jump Point)
		spawnRemnantFleet(system, gate != null ? gate : lincoln, "radiant_Strike", 250f,
				FleetAssignment.PATROL_SYSTEM, gate != null ? gate : lincoln, "intercepting hyperspace gateway");

		// Fleet 3: Lancaster Deep-Space Battlegroup (Guarding Lancaster Gas Giant & Halifax)
		spawnRemnantFleet(system, lancaster != null ? lancaster : star, "radiant_Standard", 260f,
				FleetAssignment.PATROL_SYSTEM, lancaster != null ? lancaster : star, "patrolling Lancaster orbit and moons");

		// Fleet 4: Inner System Sweep Battlegroup (Patrolling around star York & inner mining worlds)
		spawnRemnantFleet(system, star, "nova_Attack", 240f,
				FleetAssignment.PATROL_SYSTEM, star, "sweeping inner solar corona and mining worlds");
	}

	public static CampaignFleetAPI spawnRemnantFleet(StarSystemAPI system,
													 SectorEntityToken anchor,
													 String flagshipVariantId,
													 float combatPts,
													 FleetAssignment assignment,
													 SectorEntityToken assignmentTarget,
													 String actionText) {
		Random random = new Random();
		FleetParamsV3 params = new FleetParamsV3(
				null,
				anchor != null ? anchor.getLocationInHyperspace() : system.getLocation(),
				Factions.REMNANTS,
				1.2f, // fleet quality mult
				FleetTypes.PATROL_LARGE,
				combatPts,
				0f, // freighterPts
				0f, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f  // qualityMod
		);
		params.withOfficers = true;
		params.officerNumberBonus = 4;
		params.officerNumberMult = 1.25f;
		params.random = random;
		if (flagshipVariantId != null) {
			params.addShips = Collections.singletonList(flagshipVariantId);
		}

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		if (fleet == null) return null;

		system.addEntity(fleet);

		if (anchor != null) {
			Vector2f loc = Misc.getPointAtRadius(anchor.getLocation(), anchor.getRadius() + 250f + random.nextFloat() * 250f);
			fleet.setLocation(loc.x, loc.y);
		} else {
			Vector2f loc = Misc.getPointAtRadius(new Vector2f(), 2500f + random.nextFloat() * 2500f);
			fleet.setLocation(loc.x, loc.y);
		}
		fleet.setFacing(random.nextFloat() * 360f);

		RemnantSeededFleetManager.initRemnantFleetProperties(random, fleet, false);

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		fleet.addTag(Tags.NEUTRINO_HIGH);

		// Ensure all ships have full combat readiness at spawn
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.getRepairTracker() != null) {
				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
			}
		}

		// Ensure the flagship is commanded by an integrated Alpha Core with combat and doctrine skills
		if (fleet.getFlagship() != null && (fleet.getCommander() == null || fleet.getCommander().isDefault())) {
			AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
			PersonAPI commander = plugin.createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), random);
			fleet.setCommander(commander);
			fleet.getFlagship().setCaptain(commander);
			RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.getFlagship());
			RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, null, 2, random);
		}

		fleet.getFleetData().sort();
		fleet.forceSync();

		if (assignment != null && assignmentTarget != null) {
			fleet.addAssignment(assignment, assignmentTarget, 1000000f, actionText);
		}

		return fleet;
	}

	void cleanup(StarSystemAPI system) {
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2f;
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
	}
}

