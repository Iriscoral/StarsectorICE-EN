package data.scripts.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_ExileFleetIntel;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils;

public class SUN_ICE_ExileFleetManager implements EveryFrameScript {

	private static final float AVG_UPDATE_INTERVAL = 0.3f;
	public static final String EXILE_FLEET_KEY = "$SUN_ICE_ExileFleet";

	private float elapsedDays = 0f;
	private float dayOfNextUpdate = 0f;
	private float dayOfNextExodusCheck = 3f;
	private float lastDayOfLiveColony = -60f;
	private boolean finishedSettle = false;

	private CampaignFleetAPI exiledFleet;
	private SUN_ICE_ExileFleetFakeAI fakeAI;
	private SUN_ICE_ExileFleetIntel newsIntel;

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	public SUN_ICE_ExileFleetManager() {
		fakeAI = new SUN_ICE_ExileFleetFakeAI(this);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void advance(float amount) {
		SectorAPI sector = Global.getSector();
		if (sector.isInNewGameAdvance() || sector.isPaused()) {
			return;
		}

		if (finishedSettle) {
			return;
		}

		float days = sector.getClock().convertToDays(amount);
		elapsedDays += days;

		if (newsIntel != null && !newsIntel.isDone()) {
			newsIntel.advance(amount);
		}

		if (isFleetValid(exiledFleet)) {
			fakeAI.advanceAI(exiledFleet, amount);
		}

		if (isFleetValid(exiledFleet)) {
			lastDayOfLiveColony = elapsedDays;
			SUN_ICE_Data.getICE().setShowInIntelTab(true);
		}

		if (!isMarketSourceValid(SUN_ICE_Data.getIdoneusCitadel(), "sun_ici")) return;
		if (!isMarketValid(SUN_ICE_Data.getIdoneusCitadel())) return;

		if (elapsedDays > dayOfNextUpdate) {
			dayOfNextUpdate = elapsedDays + AVG_UPDATE_INTERVAL * (0.5f + (float) Math.random());
			if (isFleetValid(exiledFleet)) {
				spawnPilgrimsFleet(SUN_ICE_Data.getIdoneusCitadel(), exiledFleet);
			}
		}

		if (elapsedDays > dayOfNextExodusCheck && elapsedDays - lastDayOfLiveColony > 90f) {
			dayOfNextExodusCheck = elapsedDays + 15f + (float) Math.random() * 30f;
			if (!isFleetValid(exiledFleet)) {
				spawnColonyFleet(SUN_ICE_Data.getIdoneusCitadel());
			}
		}
	}

	public SUN_ICE_ExileFleetFakeAI getFakeAI() {
		return fakeAI;
	}

	public FactionAPI getICE() {
		return SUN_ICE_Data.getICE();
	}

	public void setFinishedSettle(boolean value) {
		finishedSettle = value;
	}

	public SUN_ICE_ExileFleetFakeAI.ExileState getCurrentState() {
		return fakeAI.getState();
	}

	public StarSystemAPI getCurrentSystemDestination() {
		return fakeAI.getDestination();
	}

	public SectorEntityToken getFinalSettle() {
		return fakeAI.getFinalSettle();
	}

	public void makeFleetInvalid() {
		exiledFleet = null;
	}

	public CampaignFleetAPI getExiledFleet() {
		return exiledFleet;
	}

	public void killExiledFleet() {
		killFleet(exiledFleet);
	}

	public void killFleet(CampaignFleetAPI fleet) {
		if (fleet == null) return;

		for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
			fleet.removeFleetMemberWithDestructionFlash(member);
		}

		fleet.getFleetData().clear();
		fleet.despawn();
	}

	public boolean isFleetValid() {
		return isFleetValid(exiledFleet);
	}

	public boolean isFleetValid(CampaignFleetAPI fleet) {
		return fleet != null && fleet.isAlive() && !fleet.getMembersWithFightersCopy().isEmpty();
	}

	public static boolean isMarketSourceValid(SectorEntityToken marketSource, String factionId) {
		return marketSource != null && marketSource.getFaction() != null && marketSource.getFaction().getId().contentEquals(factionId);
	}

	public static boolean isMarketValid(SectorEntityToken marketSource) {
		return marketSource.getMarket() != null && marketSource.getMarket().getSize() > 3;
	}

	private void spawnColonyFleet(SectorEntityToken source) {
		LocationAPI loc = source.getContainingLocation();

		CampaignFleetAPI fleet = SUN_ICE_IceUtils.createFleet(300f, 50f, 40f, 40f, 30f, 30f, 30f, null, "sun_ice", "sun_ici", getString("exiled_name"), "Patrol", "$refugees", true);
		fleet.setTransponderOn(true);
		fleet.setNoFactionInName(true);
		fleet.setNoEngaging(99999f);
		fleet.getStats().getFleetwideMaxBurnMod().modifyMult("fleet", 0.9f);
		fleet.getStats().getAccelerationMult().modifyMult("fleet", 1.5f);
		fleet.getStats().getSensorRangeMod().modifyMult("fleet", 10f);
		fleet.getStats().getSensorProfileMod().modifyFlat("fleet", 1000000f);
		fleet.getDetectedRangeMod().modifyFlat("fleet", 1000000f);
		loc.spawnFleet(source, 0f, 0f, fleet);

		fleet.setInteractionImage("illustrations", "cargo_loading");
		SectorEntityToken fakeSource = applyMarketToColonyFleet(fleet);

		Global.getLogger(this.getClass()).info("Exile fleet spawned.");
		fleet.setInflater(new SUN_ICE_ExileFleetInflater());
		fleet.forceSync();

		fakeAI.initAI();
		exiledFleet = fleet;

		fleet.getMemoryWithoutUpdate().set(EXILE_FLEET_KEY, true);
		fakeSource.getMemoryWithoutUpdate().set(EXILE_FLEET_KEY, true);
		fakeSource.getMarket().getMemoryWithoutUpdate().set(EXILE_FLEET_KEY, true);

		newsIntel = new SUN_ICE_ExileFleetIntel(this);
	}

	private void spawnPilgrimsFleet(SectorEntityToken source, CampaignFleetAPI target) {
		float time = Global.getSector().getClock().getElapsedDaysSince(0);

		float wave1 = (0.5f + (float) Math.sin(time / 257) * 0.5f);
		float wave2 = (0.5f + (float) Math.cos(time / 30) * 0.5f);
		float size = 1 - target.getFleetPoints() / 600f;

		float chance = 0.25f * size * wave1 * wave2;
		if (Math.random() > chance) {
			return;
		}

		CampaignFleetAPI pilgrims = SUN_ICE_IceUtils.createFleet(60f, 10f, 5f, 5f, 5f, 5f, 5f, null, "sun_ice", "sun_ici", getString("pilgrims_name"), "Patrol", "$pilgrims", false);
		pilgrims.setNoFactionInName(true);
		pilgrims.setTransponderOn(true);

		Global.getLogger(this.getClass()).info("Pilgrims fleet spawned.");
		source.getContainingLocation().spawnFleet(source, 0f, 0f, pilgrims);
		pilgrims.setInflater(new SUN_ICE_ExileFleetInflater());
		pilgrims.forceSync();

		pilgrims.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 9999, getString("pilgrims_return"), new JoinMotherFleetScript(target, pilgrims));
	}

	private SectorEntityToken applyMarketToColonyFleet(CampaignFleetAPI fleet) {
		int size = SUN_ICE_Data.getIdoneusCitadel().getMarket().getSize() - 3;
		if (size > 4) size = 4;

		MarketAPI newMarket = Global.getFactory().createMarket("sun_ice_colony_fleet_market" + Misc.genUID(), getString("exiled_name"), size);
		newMarket.setFactionId("sun_ice");
		newMarket.addSubmarket("open_market");
		newMarket.addSubmarket("generic_military");
		newMarket.addSubmarket("black_market");

		newMarket.addCondition("population_" + size);
		newMarket.addCondition("regional_capital");
		newMarket.addCondition("free_market");
		newMarket.addCondition("sun_ice_colony_fleet");

		newMarket.addIndustry("population");
		newMarket.addIndustry("megaport");
		newMarket.addIndustry("heavybatteries");
		newMarket.addIndustry("orbitalworks");
		newMarket.addIndustry("highcommand");

		newMarket.getTariff().modifyFlat("sun_ice_colony_fleet_market", 0.3f);

		SectorEntityToken hack = SUN_ICE_Data.getFakeMarketEntity();
		hack.setMarket(newMarket);
		hack.setFaction("sun_ice");

		fleet.setMarket(newMarket);
		newMarket.setPrimaryEntity(fleet);

		PersonAPI person = fleet.getFaction().createRandomPerson();
		person.setRankId(Ranks.CITIZEN);
		person.setPostId(Ranks.POST_ADMINISTRATOR);
		newMarket.getCommDirectory().addPerson(person);
		newMarket.addPerson(person);
		Global.getSector().getImportantPeople().addPerson(person);
		Global.getSector().getImportantPeople().getData(person).getLocation().setMarket(newMarket);
		Global.getSector().getImportantPeople().checkOutPerson(person, "permanent_staff");

		Global.getSector().getEconomy().addMarket(newMarket, false);
		Misc.removeRadioChatter(newMarket);
		return hack;
	}

	public static class JoinMotherFleetScript implements Script {
		private final CampaignFleetAPI motherFleet;
		private final CampaignFleetAPI childFleet;

		public JoinMotherFleetScript(CampaignFleetAPI motherFleet, CampaignFleetAPI childFleet) {
			this.motherFleet = motherFleet;
			this.childFleet = childFleet;
		}

		@Override
		public void run() {
			if (motherFleet == null || childFleet == null || !motherFleet.isAlive()) {
				return;
			}

			motherFleet.getCargo().addSupplies(childFleet.getCargo().getSupplies());
			motherFleet.getCargo().addFuel(childFleet.getCargo().getFuel());
			motherFleet.getCargo().addMarines(childFleet.getCargo().getMarines());
			motherFleet.getCargo().addCrew(childFleet.getCargo().getCrew());

			for (FleetMemberAPI m : childFleet.getFleetData().getMembersListCopy()) {
				motherFleet.getFleetData().addFleetMember(m);
			}

			motherFleet.updateCounts();
			motherFleet.getFleetData().sort();
			motherFleet.forceSync();

			childFleet.despawn(CampaignEventListener.FleetDespawnReason.REACHED_DESTINATION, null);
		}
	}
}