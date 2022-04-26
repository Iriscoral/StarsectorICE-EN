package data.missions.sun_ice_exodus;

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
		api.initFleet(FleetSide.ENEMY, "ICS", FleetGoal.ATTACK, true, 5);
		api.setFleetTagline(FleetSide.ENEMY, getString("exodus_enemy_tagline"));
		api.addToFleet(FleetSide.ENEMY, "sun_ice_abraxas_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_abraxas_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_voidreaver_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_kelpie_utility_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_eidolon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_eidolon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_soulbane_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_soulbane_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_soulbane_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_pentagram_utility_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_pentagram_utility_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_flashghast_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_flashghast_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sun_ice_flashghast_Standard", FleetMemberType.SHIP, false);

		api.initFleet(FleetSide.PLAYER, "ICS", FleetGoal.ESCAPE, false);
		api.setFleetTagline(FleetSide.PLAYER, getString("exodus_player_tagline"));
		api.addToFleet(FleetSide.PLAYER, "sun_ice_apocrypha_Standard", FleetMemberType.SHIP, "ICS Remorse", true);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_shalom_Standard", FleetMemberType.SHIP, "Progeny", false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_eidolon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_eidolon_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_shiekwraith_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_shiekwraith_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_shiekwraith_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_specter_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_specter_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_specter_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_flashghast_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_flashghast_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_seraph_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sun_ice_palantir_Standard", FleetMemberType.SHIP, false);
		api.defeatOnShipLoss("Progeny");

		float width = 18000f;
		float height = 24000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

		float minX = -width / 2;
		float minY = -height / 2;

		api.addNebula(minX + width * 0.66f, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.25f, minY + height * 0.6f, 1000);
		api.addNebula(minX + width * 0.25f, minY + height * 0.4f, 1000);

		for (int i = 0; i < 5; i++) {
			float x = (float) Math.random() * width - width / 2;
			float y = (float) Math.random() * height - height / 2;
			float radius = 100f + (float) Math.random() * 400f;
			api.addNebula(x, y, radius);
		}

		api.addObjective(minX + width * 0.25f + 2000f, minY + height * 0.5f, "sensor_array");
		api.addObjective(minX + width * 0.75f - 2000f, minY + height * 0.5f, "comm_relay");
		api.addObjective(minX + width * 0.33f + 2000f, minY + height * 0.4f, "nav_buoy");
		api.addObjective(minX + width * 0.66f - 2000f, minY + height * 0.6f, "nav_buoy");

		api.addAsteroidField(-(minY + height), minY + height, -45, 2000f, 20f, 70f, 100);

		api.addBriefingItem(getString("exodus_bi_1"));
		api.addBriefingItem(getString("exodus_bi_2"));
		api.addBriefingItem(getString("exodus_bi_3"));
		api.addPlugin(new MissionScript());
	}
}