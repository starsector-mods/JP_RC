package data.missions.syndicate_asp_test_mission;

import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		api.initFleet(FleetSide.PLAYER, "RNS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Royal Junk Collection Fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Target Practice");
		
		api.addBriefingItem("Test all ships from Junk Pirates, PACK, and ASP Syndicate");
		api.addBriefingItem("ASP Flagship must survive");
		
		// ASP Syndicate
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_diamondback_Standard", FleetMemberType.SHIP, "ASP Flagship", true);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_copperhead_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_hognose_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_gigantophis_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_kingcobra_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_cerberus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_mercury_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_vigilance_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_hammerhead_Balanced", FleetMemberType.SHIP, false);
		
		// Familia variants
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_diamondback_p_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_copperhead_p_Brutaliser", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_hognose_p_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_gigantophis_p_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "syndicate_asp_kingcobra_p_PD", FleetMemberType.SHIP, false);

		api.defeatOnShipLoss("ASP Flagship");
		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "junk_pirates_spinerette_base_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "junk_pirates_kraken_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "pack_bulldog_bullseye_Bullseye", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "syndicate_asp_kingcobra_p_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "junk_pirates_orca_Standard", FleetMemberType.SHIP, false);

		// Set up the map.
		float width = 20000f;
		float height = 16000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		for (int i = 0; i < 7; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 800f; 
			api.addNebula(x, y, radius);
		}
		
		api.addObjective(minX + width * 0.25f, minY + height * 0.5f, 
						 "sensor_array", BattleObjectiveAPI.Importance.NORMAL);
		api.addObjective(minX + width * 0.75f, minY + height * 0.5f, 
						 "nav_buoy", BattleObjectiveAPI.Importance.NORMAL);
		api.addObjective(minX + width * 0.40f, minY + height * 0.6f, 
						 "comm_relay", BattleObjectiveAPI.Importance.NORMAL);
		api.addObjective(minX + width * 0.60f, minY + height * 0.4f, 
						 "sensor_array", BattleObjectiveAPI.Importance.NORMAL);
		
		api.addAsteroidField(width, height, 45, 2000f, 20f, 70f, 100);
		
		api.addPlanet(minX + width * 0.55f, minY + height * 0.25f, 300f, "rocky_ice", 200f);
	}

}
