package data.scripts.tools;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.world.SUN_ICE_ExileFleetManager;

import java.util.Map;

public class SUN_ICE_Data {

	private final static String PREFIX = "sun_ice_";
	public static String IdoneusCitadelId = "sun_ice_idoneus_citadel";
	public static String IdoneusUlteriusId = "sun_ice_idoneus_ulterius";
	public static RepLevel REP_FOR_FLEET_INFO_STAGE1 = RepLevel.FAVORABLE;
	public static RepLevel REP_FOR_FLEET_INFO_STAGE2 = RepLevel.FRIENDLY;

	public static FactionAPI getICE() {
		return Global.getSector().getFaction("sun_ice");
	}

	public static FactionAPI getICI() {
		return Global.getSector().getFaction("sun_ici");
	}

	public static SectorEntityToken getIdoneusCitadel() {
		return Global.getSector().getEntityById("sun_ice_idoneus_citadel");
	}

	public static SectorEntityToken getFakeMarketEntity() {
		return Global.getSector().getEntityById("sun_ice_entity_hack");
	}

	public static SUN_ICE_ExileFleetManager getExileManager() {
		for (EveryFrameScript script : Global.getSector().getScripts()) {
			if (script instanceof SUN_ICE_ExileFleetManager) {
				return (SUN_ICE_ExileFleetManager) script;
			}
		}
		return null;
	}

	public static String getString(String key) {
		key = PREFIX + key;
		Map<String, Object> map = Global.getSector().getPersistentData();
		return map.containsKey(key) ? (String) map.get(key) : "";
	}

	public static Boolean getBool(String key, Boolean defaultValue) {
		key = PREFIX + key;
		Map<String, Object> map = Global.getSector().getPersistentData();
		if (map.containsKey(key)) {
			return (Boolean) map.get(key);
		} else {
			return defaultValue;
		}
	}

	public static void put(String key, FactionAPI faction) {
		put(key, faction == null ? "" : faction.getId());
	}

	public static void put(String key, MarketAPI market) {
		put(key, market == null ? "" : market.getId());
	}

	public static void put(String key, SectorEntityToken entity) {
		put(key, entity == null ? "" : entity.getId());
	}

	public static void put(String key, StarSystemAPI starSystem) {
		put(key, starSystem == null ? "" : starSystem.getId());
	}

	public static void put(String key, String id) {
		Map<String, Object> map = Global.getSector().getPersistentData();
		map.put(PREFIX + key, id);
	}

	public static void put(String key, boolean bool) {
		Map<String, Object> map = Global.getSector().getPersistentData();
		map.put(PREFIX + key, bool);
	}

	public static void put(String key, float num) {
		Map<String, Object> map = Global.getSector().getPersistentData();
		map.put(PREFIX + key, num);
	}

	public static void put(String key, int num) {
		Map<String, Object> map = Global.getSector().getPersistentData();
		map.put(PREFIX + key, num);
	}
}
