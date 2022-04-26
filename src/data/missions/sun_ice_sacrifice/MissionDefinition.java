package data.missions.sun_ice_sacrifice;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.ENEMY, "ICS", FleetGoal.ATTACK, true, 0);
		api.setFleetTagline(FleetSide.ENEMY, getString("sacrifice_enemy_tagline"));
		api.addToFleet(FleetSide.ENEMY, "sun_ice_soulbane_Standard", FleetMemberType.SHIP, true);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_specter_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_specter_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_seraph_Standard", FleetMemberType.SHIP, false);

		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 0);
		api.setFleetTagline(FleetSide.PLAYER, getString("sacrifice_player_tagline"));
		api.addToFleet(FleetSide.PLAYER, "conquest_Elite", FleetMemberType.SHIP, "ISS Willow Seed", true);
		api.addToFleet(FleetSide.PLAYER, "hammerhead_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "brawler_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "brawler_Elite", FleetMemberType.SHIP, false);

		api.defeatOnShipLoss("ISS Willow Seed");

		float width = 12000f;
		float height = 12000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);

		api.addBriefingItem(getString("sacrifice_bi_1"));
		api.addBriefingItem(getString("sacrifice_bi_2"));
		api.addBriefingItem(getString("sacrifice_bi_3"));
	}
}