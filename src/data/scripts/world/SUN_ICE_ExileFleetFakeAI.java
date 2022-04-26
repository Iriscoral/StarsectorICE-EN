package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_GetJay;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.intel.SUN_ICE_ExileRemnantWarIntel;
import data.scripts.ICEGenWhenNEX;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SUN_ICE_ExileFleetFakeAI {

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	public enum ExileState {
		NULL(0), START(1), STAY(2), TRAVELING(3), DEAD(4), SETTLE(5), FINISHED(6);

		private int id;

		ExileState(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
			//return this.ordinal();
		}

		public boolean shouldDisplayBy(FactionAPI faction) {
			if (isAlwaysDisplay()) return true;

			FactionAPI player = Global.getSector().getPlayerFaction();
			return player.isAtWorst(faction, SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE1);
		}

		public boolean isAlwaysDisplay() {
			return this == START || this == DEAD || this == FINISHED;
		}

		public boolean isEnd() {
			return this == DEAD || this == FINISHED;
		}
	}

	public SUN_ICE_ExileFleetFakeAI(SUN_ICE_ExileFleetManager manager) {
		this.manager = manager;
	}

	//About AI
	private SUN_ICE_ExileFleetManager manager;
	private float stayedDaysInSystem = 0f;
	private float daysToStayInSystem = 60f;
	private float livedDays = 0f;
	private float totalLivedDays = 0f;
	private float homeCheckerDays = 30f;
	private int warChecker = 5;
	private boolean warSpawning = false;
	private boolean warSpawned = false;
	private boolean firstInit = true;
	private boolean wantToTravelingToAnotherSystem = true;
	public boolean hasEnteredHyperspace = false;
	private boolean waitingPlayerRespond = false;
	private PlanetAPI tmpSettle = null;
	private PlanetAPI finalSettle = null;
	private StarSystemAPI destination = null;
	private ExileState state = ExileState.NULL;

	public PlanetAPI getFinalSettle() {
		return finalSettle;
	}

	public StarSystemAPI getDestination() {
		return destination;
	}

	public ExileState getState() {
		return state;
	}

	void initAI() {
		stayedDaysInSystem = 0f;
		daysToStayInSystem = 60f;
		livedDays = 0f;
		firstInit = true;
		wantToTravelingToAnotherSystem = true;
		hasEnteredHyperspace = false;
		waitingPlayerRespond = false;
		tmpSettle = null;
		destination = null;
		state = ExileState.START;
		resetHomeCheckerDays();
	}

	void advanceAI(CampaignFleetAPI exiles, float amount) {
		if (firstInit) {
			chooseNewTargetToTravel(exiles);
			firstInit = false;
		}

		SectorAPI sector = Global.getSector();
		float days = sector.getClock().convertToDays(amount);

		stayedDaysInSystem += days;
		livedDays += days;
		totalLivedDays += days;
		if (totalLivedDays >= 720f && livedDays >= 120f) {
			homeCheckerDays -= days;
		}

		if (!(exiles.getInflater() instanceof SUN_ICE_ExileFleetInflater)) {
			exiles.setInflater(new SUN_ICE_ExileFleetInflater());
			exiles.setInflated(true);
		}

		SectorEntityToken target = exiles.getInteractionTarget();
		if (target != null && exiles.isHostileTo(target)) {
			if (!target.getFaction().getId().contentEquals(Factions.REMNANTS))
			exiles.getAI().doNotAttack(target, 10f); // So be passive, maybe
		}

		if (!exiles.isAlive() || isColonyShipDied(exiles) || isMarketDied(exiles)) {
			state = ExileState.DEAD;

			SUN_ICE_IceUtils.decivilize(exiles.getMarket());
			SUN_ICE_MissionManager.doomStages();

			exiles.setMarket(null);
			exiles.setName(getString("exiled_name_failed"));
			if (exiles.isAlive()) {
				orderDespawn(exiles);
			}

			manager.makeFleetInvalid();
			return;
		} else if (finalSettle != null) {
			state = ExileState.SETTLE;
		} else if (wantToTravelingToAnotherSystem && hasEnteredHyperspace && exiles.isInOrNearSystem(destination)) {
			state = ExileState.STAY;
			hasEnteredHyperspace = false;
			wantToTravelingToAnotherSystem = false;
			stayedDaysInSystem = 0f;
			daysToStayInSystem = 30f + (float) Math.random() * 50f;

			exiles.clearAssignments();
			exiles.addAssignment(FleetAssignment.PATROL_SYSTEM, destination.getStar(), 10f, getString("exiled_wander"), new WardingScript(destination, exiles));
			if (!warSpawned && warSpawning) {
				warSpawned = true;
				daysToStayInSystem = 999999f;
				spawnRemnantWarEvent(exiles);
			}
		} else if (!wantToTravelingToAnotherSystem && stayedDaysInSystem > daysToStayInSystem) {
			if (homeCheckerDays <= 0f) {
				boolean wantMakeHome = noMissionStageToPreventMakingHome(exiles);
				if (wantMakeHome) {
					pauseAndProcessHomeSelecting(exiles);
					return;
				} else {
					resetHomeCheckerDays();
				}
			}

			removeNotStartedMissionGiver(exiles);
			chooseNewTargetToTravel(exiles);
			wantToTravelingToAnotherSystem = true;
			state = ExileState.TRAVELING;
		}

		if (exiles.isInHyperspace()) {
			hasEnteredHyperspace = true;
		} else {

			if (exiles.getCargo().getFuel() < 100) {
				exiles.getCargo().addFuel(exiles.getCargo().getMaxFuel() * 0.3f);
			}
			if (exiles.getCargo().getSupplies() < 100) {
				exiles.getCargo().addSupplies(exiles.getCargo().getMaxCapacity() * 0.1f);
			}
		}
	}

	public void forceMove() {
		daysToStayInSystem = -1f;
	}

	public void increaseDaysToStay(float days) {
		daysToStayInSystem += days;
	}

	public void setDaysToStay(float days) {
		daysToStayInSystem = days;
	}

	public void setHomeCheckerDays(float days) {
		homeCheckerDays = days;
	}

	public void resetHomeCheckerDays() {
		homeCheckerDays = 60f;
	}

	private static void setShowICE(boolean value) {
		SUN_ICE_Data.getICE().setShowInIntelTab(value);
	}

	private static String getTravelString(StarSystemAPI target) {
		FactionAPI player = Global.getSector().getPlayerFaction();
		if (player.isAtWorst(SUN_ICE_Data.getICE(), SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE1)) {
			return getString("exiled_travel") + target.getName();
		}

		return getString("exiled_traveling");
	}

	private static String getTravelString(SectorEntityToken target) {
		FactionAPI player = Global.getSector().getPlayerFaction();
		if (player.isAtWorst(SUN_ICE_Data.getICE(), SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE1)) {
			return getString("exiled_travel") + target.getName();
		}

		return getString("exiled_traveling");
	}

	private boolean noMissionStageToPreventMakingHome(CampaignFleetAPI exiles) {
		SUN_ICE_MissionManager.MissionStage stage = SUN_ICE_MissionManager.getStage();
		if (stage == SUN_ICE_MissionManager.MissionStage.STAGE_NOT_STARTED) return true;
		if (stage == SUN_ICE_MissionManager.MissionStage.STAGE_FINALE) return true;

		if (stage == SUN_ICE_MissionManager.MissionStage.STAGE_AFTER_ALL && !SUN_ICE_MissionManager.isAllSpecialTagsCompleted()) {
			return true;
		}

		return false;
	}

	private void pauseAndProcessHomeSelecting(CampaignFleetAPI exiles) {
		if (mayMakeHomeUnderPlayer(exiles) && !SUN_ICE_Data.getBool(SUN_ICE_ExileFleetFinalDialog.EXILE_FLEET_FINAL_DIALOG_ASKED_KEY, false)) {
			tmpSettle = pickFinalSettleTargetWithPlayer(exiles);
			if (tmpSettle != null) {
				waitingPlayerRespond = true;
				SUN_ICE_Data.put(SUN_ICE_ExileFleetFinalDialog.EXILE_FLEET_FINAL_DIALOG_ASKED_KEY, true);
				Global.getSector().getCampaignUI().showInteractionDialog(new SUN_ICE_ExileFleetFinalDialog(exiles, tmpSettle), null);
			}
		}

		if (waitingPlayerRespond && !SUN_ICE_Data.getBool(SUN_ICE_ExileFleetFinalDialog.EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY, false)) {
			return;
		}

		if (SUN_ICE_Data.getBool(SUN_ICE_ExileFleetFinalDialog.EXILE_FLEET_FINAL_DIALOG_AGREE_KEY, false)) {
			setFinalSettle(exiles, tmpSettle, true);
		} else {
			if (tmpSettle == null) tmpSettle = pickFinalSettleTarget(exiles); // re-pick
			if (tmpSettle != null) setFinalSettle(exiles, tmpSettle, false);
			/*
			tmpSettle = pickFinalSettleTarget(exiles); // re-pick
			if (tmpSettle != null) {
				setFinalSettle(exiles, tmpSettle, false);
			} else {
				resetHomeCheckerDays();
			}

			 */
		}
	}

	private boolean mayMakeHomeUnderPlayer(CampaignFleetAPI exiles) {
		SUN_ICE_MissionManager.MissionStage stage = SUN_ICE_MissionManager.getStage();
		if (stage != SUN_ICE_MissionManager.MissionStage.STAGE_FINALE) return false;

		return exiles.getFaction().isAtWorst("player", RepLevel.COOPERATIVE); // && playerHasColony();
	}

	private boolean isColonyShipDied(CampaignFleetAPI exiles) {
		for (FleetMemberAPI m : exiles.getFleetData().getMembersListCopy()) {
			if (m.getHullSpec().getBaseHullId().contentEquals("sun_ice_shalom")) {
				return false;
			}
		}

		Global.getLogger(this.getClass()).info("Exile Fleet no shalom");
		return true;
	}

	private boolean isMarketDied(CampaignFleetAPI exiles) {
		if (!exiles.getFaction().getId().contentEquals("sun_ice") || exiles.getMarket() == null || exiles.getMarket().isPlanetConditionMarketOnly()) {
			for (FleetMemberAPI m : exiles.getFleetData().getMembersListCopy()) {
				if (m.getHullSpec().getBaseHullId().contentEquals("sun_ice_shalom")) {
					exiles.removeFleetMemberWithDestructionFlash(m);
				}
			}

			Global.getLogger(this.getClass()).info("Exile Fleet no market");
			return true;
		}

		return false;
	}

	private void removeNotStartedMissionGiver(CampaignFleetAPI exiles) {
		SUN_ICE_MissionManager.MissionStage stage = SUN_ICE_MissionManager.getStage();
		if (stage == SUN_ICE_MissionManager.MissionStage.STAGE_NOT_STARTED) {
			PersonAPI missionGiver = SUN_ICE_MissionManager.getMissionGiver();
			if (missionGiver != null) {
				MarketAPI market = exiles.getMarket();
				market.getCommDirectory().removePerson(missionGiver);
				market.removePerson(missionGiver);
			}
		}
	}

	private void setFinalSettle(CampaignFleetAPI exiles, PlanetAPI tmpSettle, boolean withPlayer) {
		exiles.clearAssignments();
		exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, tmpSettle, 9999, getTravelString(tmpSettle));
		exiles.addAssignment(FleetAssignment.ORBIT_PASSIVE, tmpSettle, getDaysToOrbit(exiles), getString("exiled_settle") + tmpSettle.getName());
		exiles.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, tmpSettle, 9999, new SettleAndBuildColonyScript(this, withPlayer));
		destination = tmpSettle.getStarSystem();
		finalSettle = tmpSettle;
	}

	private void orderDespawn(CampaignFleetAPI exiles) {
		setShowICE(false);
		List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
		markets.remove(manager.getExiledFleet().getMarket());
		markets.remove(SUN_ICE_Data.getIdoneusCitadel().getMarket());

		WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();
		for (MarketAPI tryMarket : markets) {
			if (tryMarket.isHidden() || tryMarket.getPrimaryEntity() == null) {
				continue;
			}
			LocationAPI loc = tryMarket.getPrimaryEntity().getContainingLocation();
			Vector2f locHyper = tryMarket.getPrimaryEntity().getLocationInHyperspace();
			Vector2f locFleet = exiles.getLocationInHyperspace();
			float weight = 1000f / Math.max(MathUtils.getDistance(locFleet, locHyper), 1f);
			if (tryMarket.getFaction().isHostileTo(exiles.getFaction())) {
				weight *= 0.001f;
			}
			if (loc == exiles.getContainingLocation()) {
				weight *= 100f;
			}
			picker.add(tryMarket, weight);
		}

		if (picker.isEmpty()) {
			manager.killExiledFleet();
			return;
		}

		MarketAPI selectedMarket = picker.pick();
		SectorEntityToken token = selectedMarket.getPrimaryEntity();
		Global.getLogger(SUN_ICE_ExileFleetFakeAI.class).info("Exile colony fleet to despawn at: " + selectedMarket.getName());
		exiles.getDetectedRangeMod().unmodify("fleet");
		exiles.clearAssignments();
		exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 9999);
		exiles.addAssignment(FleetAssignment.ORBIT_PASSIVE, token, getDaysToOrbit(exiles), getString("exiled_dismiss") + token.getName());
		exiles.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, selectedMarket.getPrimaryEntity(), 9999);
	}

	private void chooseNewTargetToTravel(CampaignFleetAPI exiles) {
		if (!warSpawning && SUN_ICE_MissionManager.isAllSpecialTagsCompleted()) {
			warChecker--;
			Global.getLogger(this.getClass()).info("Exile war checker " + warChecker);

			if (warChecker <= 0) {
				warSpawning = true;
				destination = pickRemnantWarLocation();
				exiles.clearAssignments();
				exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, destination.getHyperspaceAnchor(), 9999, getTravelString(destination));

				Global.getLogger(this.getClass()).info("Exile colony fleet set war destination to: " + destination.getName());
				return;
			}
		}

		WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<>();
		for (MarketAPI tryMarket : Global.getSector().getEconomy().getMarketsCopy()) {
			if (tryMarket.isHidden() || tryMarket.getPrimaryEntity() == null) {
				continue;
			}
			if (tryMarket.getStarSystem() == null || tryMarket.getPrimaryEntity().getContainingLocation().isHyperspace()) {
				continue;
			}

			StarSystemAPI system = tryMarket.getStarSystem();
			if (system == exiles.getContainingLocation()) {
				continue;
			}

			FactionAPI faction = tryMarket.getFaction();
			float factor = faction.isHostileTo(exiles.getFaction()) || faction.getId().contentEquals("sun_ici") ? 0.1f : 1f;

			systems.add(system, factor);
		}

		destination = systems.pick();
		if (destination == null) {
			Global.getLogger(this.getClass()).error("Exile colony fleet can not find new destination");
		} else {
			Global.getLogger(this.getClass()).info("Exile colony fleet set new destination to: " + destination.getName());

			exiles.clearAssignments();
			exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, destination.getHyperspaceAnchor(), 9999, getTravelString(destination));
		}
	}

	private Random random = new Random();

	private StarSystemAPI pickRemnantWarLocation() {
		WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(random);
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {

			if (system.hasPulsar()) continue;
			if (system.getStar() == null) continue;
			if (system.hasPulsar()) continue;
			if (system.hasBlackHole()) continue;

			if (system.hasTag(Tags.THEME_CORE_POPULATED)) continue;
			if (system.hasTag(Tags.THEME_HIDDEN)) continue;

			float weight = 1f;
			if (system.hasTag(Tags.THEME_REMNANT_MAIN)) {
				weight = 10f;
			}
			if (system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {
				weight = 20f;
			}
			if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
				weight = 100f;
			}
			if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
				weight = 100f;
			}
			if (system.hasTag(Tags.THEME_REMNANT_SUPPRESSED)) {
				weight = 100f;
			}
			if (system.hasTag(Tags.THEME_REMNANT_RESURGENT)) {
				weight = 20f;
			}

			picker.add(system, weight);
		}

		return picker.pick();
	}

	private void spawnRemnantWarEvent(CampaignFleetAPI exiles) {
		new SUN_ICE_ExileRemnantWarIntel(manager, exiles);
	}

	private boolean isSystemValidForSettle(StarSystemAPI system, FactionAPI settler) {
		if (system.hasPulsar()) return false;
		if (system.getStar() == null) return false;
		if (system.hasPulsar()) return false;
		if (system.hasBlackHole()) return false;

		if (system.hasTag(Tags.THEME_CORE_POPULATED)) return false;
		if (system.hasTag(Tags.THEME_HIDDEN)) return false;

		for (CampaignFleetAPI fleet : system.getFleets()) {
			if (fleet.getFaction().isHostileTo(settler) && fleet.isStationMode()) {
				return false;
			}
		}

		return true;
	}

	private boolean isPlanetValidForSettle(PlanetAPI planet) {
		if (planet.isStar()) return false;
		if (planet.getMarket().hasCondition("US_virus")) return false;
		if (planet.getMarket() == null) return false; // ???????
		if (planet.getMarket().isInEconomy()) return false;

		return true;
	}

	private PlanetAPI pickFinalSettleTarget(CampaignFleetAPI exiles) {
		List<StarSystemAPI> systemsWithNoMarket = Global.getSector().getStarSystems();
		List<StarSystemAPI> systemsWithMarket = new ArrayList<>();
		for (MarketAPI tryMarket : Global.getSector().getEconomy().getMarketsCopy()) {
			if (tryMarket.isHidden()) continue;
			if (tryMarket.getPrimaryEntity() == null) continue;

			if (tryMarket.getStarSystem() == null) continue;
			if (tryMarket.getPrimaryEntity().getContainingLocation().isHyperspace()) continue;

			if (systemsWithMarket.contains(tryMarket.getStarSystem())) continue;
			systemsWithMarket.add(tryMarket.getStarSystem());
		}
		systemsWithNoMarket.removeAll(systemsWithMarket);

		WeightedRandomPicker<PlanetAPI> reserved = new WeightedRandomPicker<>();
		for (StarSystemAPI system : systemsWithNoMarket) {
			if (!isSystemValidForSettle(system, exiles.getFaction())) continue;

			for (PlanetAPI planet : system.getPlanets()) {
				if (!isPlanetValidForSettle(planet)) continue;

				float value = getSurveyValue(planet);
				float distantFactor = (float) Math.log(system.getHyperspaceAnchor().getLocation().x + system.getHyperspaceAnchor().getLocation().y + 2f);
				reserved.add(planet, value * distantFactor);
			}
		}

		if (reserved.isEmpty()) {
			for (StarSystemAPI system : systemsWithMarket) {
				if (!isSystemValidForSettle(system, exiles.getFaction())) continue;

				for (PlanetAPI planet : system.getPlanets()) {
					if (!isPlanetValidForSettle(planet)) continue;

					float value = getSurveyValue(planet);
					float distantFactor = (float) Math.log(system.getHyperspaceAnchor().getLocation().x + system.getHyperspaceAnchor().getLocation().y + 2f);
					reserved.add(planet, value * distantFactor);
				}
			}
		}

		return reserved.pick();
	}

	private PlanetAPI pickFinalSettleTargetWithPlayer(CampaignFleetAPI exiles) {
		List<StarSystemAPI> systemsWithNoPlayer = Global.getSector().getStarSystems();
		List<StarSystemAPI> systemsWithPlayer = new ArrayList<>();
		WeightedRandomPicker<MarketAPI> playerMarkets = new WeightedRandomPicker<>();
		for (MarketAPI tryMarket : Global.getSector().getEconomy().getMarketsCopy()) {
			if (tryMarket.isHidden()) continue;
			if (tryMarket.getPrimaryEntity() == null) continue;

			if (tryMarket.getStarSystem() == null) continue;
			if (tryMarket.getPrimaryEntity().getContainingLocation().isHyperspace()) continue;

			if (!tryMarket.isPlayerOwned()) continue;
			if (systemsWithPlayer.contains(tryMarket.getStarSystem())) continue;

			systemsWithPlayer.add(tryMarket.getStarSystem());
			playerMarkets.add(tryMarket, tryMarket.getSize());
		}
		systemsWithNoPlayer.removeAll(systemsWithPlayer);

		WeightedRandomPicker<PlanetAPI> reserved = new WeightedRandomPicker<>();
		Global.getLogger(this.getClass()).info("Exile fleet applied with player.");
		for (StarSystemAPI system : systemsWithPlayer) {
			for (PlanetAPI planet : system.getPlanets()) {
				if (!isPlanetValidForSettle(planet)) continue;

				float value = getSurveyValue(planet);
				reserved.add(planet, value);
			}
		}

		if (reserved.isEmpty()) {
			StarSystemAPI anchor = null;
			if (!playerMarkets.isEmpty()) anchor = playerMarkets.pick().getStarSystem();

			for (StarSystemAPI system : systemsWithNoPlayer) {
				if (!isSystemValidForSettle(system, exiles.getFaction())) continue;

				for (PlanetAPI planet : system.getPlanets()) {
					if (!isPlanetValidForSettle(planet)) continue;

					float value = getSurveyValue(planet);
					float distantFactor = (float) Math.log(system.getHyperspaceAnchor().getLocation().x + system.getHyperspaceAnchor().getLocation().y + 1f);
					if (anchor != null) distantFactor = 1f + MathUtils.getDistance(anchor.getHyperspaceAnchor(), system.getHyperspaceAnchor());

					reserved.add(planet, value / distantFactor);
				}
			}
		}

		return reserved.pick();
	}

	private static final HashMap<String, Integer> INTERESTING_CONDITIONS = new HashMap<>();
	static {
		INTERESTING_CONDITIONS.put("volatiles_trace", 15);
		INTERESTING_CONDITIONS.put("volatiles_diffuse", 20);
		INTERESTING_CONDITIONS.put("volatiles_abundant", 25);
		INTERESTING_CONDITIONS.put("volatiles_plentiful", 45);
		INTERESTING_CONDITIONS.put("ore_sparse", 10);
		INTERESTING_CONDITIONS.put("ore_moderate", 12);
		INTERESTING_CONDITIONS.put("ore_abundant", 15);
		INTERESTING_CONDITIONS.put("ore_rich", 20);
		INTERESTING_CONDITIONS.put("ore_ultrarich", 25);
		INTERESTING_CONDITIONS.put("rare_ore_sparse", 15);
		INTERESTING_CONDITIONS.put("rare_ore_moderate", 20);
		INTERESTING_CONDITIONS.put("rare_ore_abundant", 25);
		INTERESTING_CONDITIONS.put("rare_ore_rich", 30);
		INTERESTING_CONDITIONS.put("rare_ore_ultrarich", 40);
		INTERESTING_CONDITIONS.put("farmland_poor", 15);
		INTERESTING_CONDITIONS.put("farmland_adequate", 25);
		INTERESTING_CONDITIONS.put("farmland_rich", 30);
		INTERESTING_CONDITIONS.put("farmland_bountiful", 40);
		INTERESTING_CONDITIONS.put("organics_trace", 15);
		INTERESTING_CONDITIONS.put("organics_common", 20);
		INTERESTING_CONDITIONS.put("organics_abundant", 25);
		INTERESTING_CONDITIONS.put("organics_plentiful", 35);
		INTERESTING_CONDITIONS.put("habitable", 100);
		INTERESTING_CONDITIONS.put("ruins_scattered", 5);
		INTERESTING_CONDITIONS.put("ruins_widespread", 10);
		INTERESTING_CONDITIONS.put("ruins_extensive", 15);
		INTERESTING_CONDITIONS.put("ruins_vast", 20);
	}

	private float getSurveyValue(PlanetAPI planet) {
		float value = 0f;
		for (MarketConditionAPI mc : planet.getMarket().getConditions()) {
			if (INTERESTING_CONDITIONS.containsKey(mc.getId())) {
				value += INTERESTING_CONDITIONS.get(mc.getId());
			}
		}

		float hazard = planet.getMarket().getHazardValue();
		value -= hazard * 5f;
		return Math.max(value, 0f);
	}

	private float getDaysToOrbit(CampaignFleetAPI exiles) {
		float pts = exiles.getFleetPoints();
		float daysToOrbit = Math.max(Math.min(pts * 0.04f + 2f, 8f), 2f);
		daysToOrbit *= (0.5f + (float) Math.random() * 0.5f);
		return daysToOrbit;
	}

	public static class WardingScript implements Script {
		private final StarSystemAPI system;
		private final CampaignFleetAPI fleet;

		public WardingScript(StarSystemAPI system, CampaignFleetAPI fleet) {
			this.system = system;
			this.fleet = fleet;
		}

		@Override
		public void run() {
			if (system == null || fleet == null || !fleet.isAlive()) {
				return;
			}

			float seed = (float) Math.random();
			if (seed < 0.1f) {
				fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, system.getStar(), 10f, getString("exiled_wander"), this);
			} else {
				WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
				for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
					if (market.isHidden()) continue;
					if (market.getPrimaryEntity() == null) continue;
					if (market.getFaction() == fleet.getFaction()) continue;

					float w = market.getSize();
					if (market.getFaction().isHostileTo(fleet.getFaction()) || market.getFaction().getId().contentEquals("sun_ici")) {
						w *= 0.01f;
					} else if (market.getFaction().isAtWorst(fleet.getFaction(), RepLevel.FAVORABLE)) {
						w *= 2f;
					}

					picker.add(market.getPrimaryEntity(), w);
				}

				SectorEntityToken entity = picker.pick();
				if (entity == null) {
					fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, system.getStar(), 10f, getString("exiled_wander"), this);
					return;
				}

				int days = 5 + (int) (Math.random() * 10);
				fleet.addAssignment(FleetAssignment.RESUPPLY, entity, days, getString("exiled_wander"), this);
			}
		}
	}

	public static class SettleAndBuildColonyScript implements Script {
		private final SUN_ICE_ExileFleetFakeAI fakeAI;
		private final boolean withPlayer;

		public SettleAndBuildColonyScript(SUN_ICE_ExileFleetFakeAI fakeAI, boolean withPlayer) {
			this.fakeAI = fakeAI;
			this.withPlayer = withPlayer;
		}

		@Override
		public void run() {
			fakeAI.manager.setFinishedSettle(true);
			fakeAI.state = ExileState.FINISHED;

			fakeAI.finalSettle.getMarket().removePerson(SUN_ICE_MissionManager.getMissionGiver());
			fakeAI.finalSettle.getMarket().removePerson(SUN_ICE_MissionManager.getPriest());

			List<MarketConditionAPI> conditions = new ArrayList<>();
			if (fakeAI.finalSettle.getMarket() != null) {
				fakeAI.finalSettle.getMarket().setSurveyLevel(MarketAPI.SurveyLevel.FULL);
				conditions.addAll(fakeAI.finalSettle.getMarket().getConditions());
			}

			MarketAPI exileMarket = fakeAI.manager.getExiledFleet().getMarket();
			exileMarket.getConnectedEntities().clear();
			exileMarket.setPrimaryEntity(fakeAI.finalSettle);
			exileMarket.removeCondition("sun_ice_colony_fleet");

			fakeAI.finalSettle.setName(getString("exiled_name_final"));
			fakeAI.finalSettle.setMarket(exileMarket);
			fakeAI.finalSettle.getMarket().setName(getString("exiled_name_final"));
			fakeAI.finalSettle.getMarket().setImmigrationClosed(false);
			fakeAI.finalSettle.getMarket().setSurveyLevel(MarketAPI.SurveyLevel.FULL);
			for (MarketConditionAPI cond : fakeAI.finalSettle.getMarket().getConditions()) {
				cond.setSurveyed(true);
			}

			fakeAI.finalSettle.getMarket().addSubmarket(Submarkets.SUBMARKET_STORAGE);
			for (MarketConditionAPI condition : conditions) {
				fakeAI.finalSettle.getMarket().addCondition(condition);
			}

			fakeAI.finalSettle.getMarket().addIndustry("commerce");
			fakeAI.finalSettle.getMarket().addIndustry("starfortress_high");

			if (withPlayer) {
				setShowICE(false);
				fakeAI.finalSettle.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSpecial(new SpecialItemData("ice_package_bp", null), 1);
				fakeAI.finalSettle.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSpecial(new SpecialItemData("ice_package_rp", null), 1);
				fakeAI.finalSettle.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "sun_ice_shalom_Hull", "Shalom");
				((StoragePlugin) fakeAI.finalSettle.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);

				SUN_ICE_IceUtils.setMarketOwner(fakeAI.finalSettle.getMarket(), Global.getSector().getPlayerFaction());
				if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
					ICEGenWhenNEX.removeICEFactionForNEX();
				}

				Global.getSector().addScript(new SUN_ICE_MissionManager(new SUN_ICE_GetJay()));
			} else {
				SectorEntityToken shalom_station = fakeAI.finalSettle.getStarSystem().addCustomEntity("sun_ice_shalom_station", getString("exiled_name_final"), "sun_ice_idoneus_shalom", "sun_ice");
				shalom_station.setCircularOrbitPointingDown(fakeAI.finalSettle, 245, 500, 300);
				shalom_station.setCustomDescriptionId("sun_ice_shalom_station");

				fakeAI.finalSettle.setFaction("sun_ice");
				fakeAI.finalSettle.getMarket().setFactionId("sun_ice");
				if (!fakeAI.finalSettle.getMarket().hasIndustry("battlestation_high")) {
					fakeAI.finalSettle.getMarket().addIndustry("battlestation_high");
				}

				if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
					ICEGenWhenNEX.spawnICEFactionForNEX();
				}
			}

			fakeAI.finalSettle.getMarket().reapplyConditions();
			fakeAI.finalSettle.getMarket().reapplyIndustries();
		}
	}
}