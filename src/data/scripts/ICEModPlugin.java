package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.ai.drone.SUN_ICE_ICEMxDroneAI;
import data.ai.missile.*;
import data.ai.weapon.*;
import data.scripts.campaign.bar.SUN_ICE_EventChainStarterBarEventCreator;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import data.scripts.world.SUN_ICE_CampaignListener;
import data.scripts.world.SUN_ICE_CampaignPlugin;
import data.scripts.world.SUN_ICE_ExileFleetManager;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ICEModPlugin extends BaseModPlugin {
	public static boolean EXERELIN_ENABLED = false;
	public static final boolean SMILE_FOR_CAMERA = false;
	public static final Color HEAL_TEXT_COLOR = new Color(0, 255, 100);

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	@Override
	public void onApplicationLoad() {

		if (!ICEBlackList.getCode().contentEquals("9a5d472831d9924a56940a2ebe783bd")) {
			throw new RuntimeException();
		}

		for (String id : ICEBlackList.getBlackListModId()) {
			if (Global.getSettings().getModManager().isModEnabled(id)) {
				throw new RuntimeException(String.format(strings.get("incMod"), Global.getSettings().getModManager().getModSpec("nbj_ice").getName(), Global.getSettings().getModManager().getModSpec(id).getName()));
			}
		}

		List<String> hullIds = new ArrayList<>();
		List<String> weaponIds = new ArrayList<>();
		for (ShipHullSpecAPI hull : Global.getSettings().getAllShipHullSpecs()) {
			hullIds.add(hull.getHullId());
		}
		for (WeaponSpecAPI weapon : Global.getSettings().getAllWeaponSpecs()) {
			weaponIds.add(weapon.getWeaponId());
		}
		if (!intersectionConfirm(ICEBlackList.getBlackListShipId(), hullIds) || !intersectionConfirm(ICEBlackList.getBlackListWeaponId(), weaponIds)) {
			throw new RuntimeException(Global.getSettings().getModManager().getModSpec("nbj_ice").getName() + " " + strings.get("imMod"));
		}

		if (Global.getSettings().getModManager().isModEnabled("ungp")) {
			ModSpecAPI spec = Global.getSettings().getModManager().getModSpec("ungp");
			if (Integer.parseInt(spec.getVersionInfo().getMinor()) < 6) {
				throw new RuntimeException("Your UNGP is too old, get a new one in fossic.org!");
			}
		}

		ShaderLib.init();
		LightData.readLightDataCSV("data/lights/SUN_ICE_light_data.csv");
		TextureData.readTextureDataCSV("data/lights/SUN_ICE_texture_data.csv");

		EXERELIN_ENABLED = Global.getSettings().getModManager().isModEnabled("nexerelin");
	}

	@Override
	public void onGameLoad(boolean newGame) {
		if (EXERELIN_ENABLED) {
			ICEGenWhenNEX.setNotTransfer();
		}

		if (EXERELIN_ENABLED && !ICEGenWhenNEX.checkIfCorvus()) {
			Global.getLogger(this.getClass()).info("In NEX without corvus, skipping ICE event spawn");
			return;
		}

		BarEventManager manager = BarEventManager.getInstance();
		if (!manager.hasEventCreator(SUN_ICE_EventChainStarterBarEventCreator.class)) {
			Global.getLogger(this.getClass()).info("Bar event manager registered");
			manager.addEventCreator(new SUN_ICE_EventChainStarterBarEventCreator());
		}
	}

	@Override
	public void onNewGame() {

		if (EXERELIN_ENABLED) {
			new ICEGenWhenNEX().generate(Global.getSector());
		} else {
			new ICEGenNormal().generate(Global.getSector());
		}

		Global.getSector().registerPlugin(new SUN_ICE_CampaignPlugin());
		Global.getSector().addListener(new SUN_ICE_CampaignListener(true));
		Global.getSector().addScript(new SUN_ICE_ExileFleetManager());
	}

	@Override
	public PluginPick<ShipAIPlugin> pickDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
		String id = drone.getHullSpec().getHullId();

		if (id.contentEquals("sun_ice_drone_mx")) {
			return new PluginPick<ShipAIPlugin>(new SUN_ICE_ICEMxDroneAI(drone, mothership, system), CampaignPlugin.PickPriority.MOD_SET);
		}

		return super.pickDroneAI(drone, mothership, system);
	}

	@Override
	public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		String id = missile.getProjectileSpecId();

		switch (id) {
			case "sun_ice_scatterpd":
				return new PluginPick<MissileAIPlugin>(new SUN_ICE_ScatterPdMissileAI(missile), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_boomerang":
				return new PluginPick<MissileAIPlugin>(new SUN_ICE_BoomerangMissileAI(missile), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_gandiva":
				return new PluginPick<MissileAIPlugin>(new SUN_ICE_GandivaMissileAI(missile), CampaignPlugin.PickPriority.MOD_SET);
			// case "sun_ice_spitfire":
				// return new PluginPick<MissileAIPlugin>(new SUN_ICE_SpitfireMissileAI(missile), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_mine_pod":
				return new PluginPick<MissileAIPlugin>(new SUN_ICE_MinePodAI(missile), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_mine":
				return new PluginPick<MissileAIPlugin>(new SUN_ICE_MineAI(missile), CampaignPlugin.PickPriority.MOD_SET);
		}

		return super.pickMissileAI(missile, launchingShip);
	}

	@Override
	public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
		String id = weapon.getId();

		switch (id) {
			case "sun_ice_mobiusray":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_MobiusRayAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_hypermassdriver":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_HypermassDriverAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_nova":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_NovaDischargerAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_fissiondrill":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_FissionDrillAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_nos":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_NosAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
			case "sun_ice_chupacabra":
				return new PluginPick<AutofireAIPlugin>(new SUN_ICE_ChupacabraAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_SET);
		}

		return super.pickWeaponAutofireAI(weapon);
	}

	private static boolean intersectionConfirm(List<String> listA, List<String> listB) {
		listA.retainAll(listB);
		return listA.isEmpty();
	}
}