package data.missions.sun_ice_duel;

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
		api.setFleetTagline(FleetSide.ENEMY, getString("duel_enemy_tagline"));
		api.addToFleet(FleetSide.ENEMY, "sun_ice_abraxas_Standard", FleetMemberType.SHIP, "ICS Samuel", false);

		api.initFleet(FleetSide.PLAYER, "ICS", FleetGoal.ATTACK, false, 0);
		api.setFleetTagline(FleetSide.PLAYER, getString("duel_player_tagline"));
		api.addToFleet(FleetSide.PLAYER, "sun_ice_nightseer_Standard", FleetMemberType.SHIP, "ICS Tiny Dancer", true);

		float width = 10000f;
		float height = 10000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);

		api.addBriefingItem(getString("duel_bi_1"));
		api.addBriefingItem(getString("duel_bi_2"));
		api.addBriefingItem(getString("duel_bi_3"));
	}
}