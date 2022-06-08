package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SUN_ICE_MissionSalvageAICoreIntel extends BaseIntelPlugin {

	private final Random random = new Random();
	private final SectorEntityToken cache;
	private final StarSystemAPI cacheSystem;
	private final MarketAPI market;

	private final int requiredAmount;
	private final int extraAmount;

	private float timeLeft;
	private float repGot = 0f;
	private int reward = 0;
	private boolean looted = false;

	public SUN_ICE_MissionSalvageAICoreIntel(InteractionDialogAPI dialog, int requiredAmount, int extraAmount, float timeOutDuration) {
		this.cache = spawnCache();
		assert cache != null;

		this.cache.getMemoryWithoutUpdate().set("$SUN_ICE_eventRef", this);
		Misc.makeImportant(cache, "SUN_ICE_SalvageAICoreIntel");
		spawnEnemyToInvestigate(cache, 135f);
		spawnEnemyToInvestigate(cache, 70f);

		this.reward = 200000;
		this.timeLeft = timeOutDuration;
		this.requiredAmount = requiredAmount;
		this.extraAmount = extraAmount;

		this.cacheSystem = cache.getStarSystem();
		this.market = dialog.getInteractionTarget().getMarket();

		setImportant(true);
		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
	}

	public static final I18nSection strings = I18nSection.getInstance("Intel", "SUN_ICE_special_cache_");
	public static final I18nSection cacheString = I18nSection.getInstance("Event", "SUN_ICE_");

	private boolean isMissionValid() {
		return market != null && market.isInEconomy() && market.getFaction().getId().contentEquals("sun_ice") && market.getFaction().isAtWorst(Global.getSector().getPlayerFaction(), RepLevel.NEUTRAL) && timeLeft > 0f;
	}

	@Override
	public void advanceImpl(float amount) {
		float days = Global.getSector().getClock().convertToDays(amount);
		timeLeft -= days;

		if (!isMissionValid()) {
			SUN_ICE_MissionManager.doomStages();
			createPenaltyResult();
			endAfterDelay();
		}
	}

	public void simpleFinish(InteractionDialogAPI dialog) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CargoAPI cargo = playerFleet.getCargo();
		int finalReward = 50000;

		cargo.getCredits().add(finalReward);
		AddRemoveCommodity.addCreditsGainText(finalReward, dialog.getTextPanel());

		reward = finalReward;
		repGot = 0f;
		endAfterDelay();
	}

	public void performDelivery(InteractionDialogAPI dialog, boolean extra) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CargoAPI cargo = playerFleet.getCargo();
		int finalRequiredAmount = requiredAmount + (extra ? extraAmount : 0);
		int finalReward = reward * finalRequiredAmount + 50000;

		cargo.removeItems(CargoAPI.CargoItemType.RESOURCES, Commodities.ALPHA_CORE, finalRequiredAmount);
		cargo.getCredits().add(finalReward);
		AddRemoveCommodity.addCommodityLossText(Commodities.ALPHA_CORE, finalRequiredAmount, dialog.getTextPanel());
		AddRemoveCommodity.addCreditsGainText(finalReward, dialog.getTextPanel());

		float repAmount = extra ? 0.05f : 0.03f;
		CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, completionRep, null, dialog.getTextPanel(), true, true), getFactionForUIColors().getId());

		reward = finalReward;
		repGot = repAmount;
		endAfterDelay();
	}

	public boolean hasEnough(boolean extra) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CargoAPI cargo = playerFleet.getCargo();
		int finalRequiredAmount = requiredAmount + (extra ? extraAmount : 0);

		return cargo.getCommodityQuantity(Commodities.ALPHA_CORE) >= finalRequiredAmount;
	}

	private void createPenaltyResult() {
		float repAmount = 0.05f;
		CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_FAILURE, completionRep, null, null, true, false), getFactionForUIColors().getId());
	}

	public boolean isLooted() {
		return looted;
	}

	public void forceFailed() {
		timeLeft = -1f;
	}

	public void delayLimit(float delay) {
		timeLeft += delay;
	}

	@Override
	public boolean callEvent(String ruleId, final InteractionDialogAPI thisDialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		OptionPanelAPI options = thisDialog.getOptionPanel();
		CargoAPI salvage = Global.getFactory().createCargo(true);
		salvage.addCommodity(Commodities.ALPHA_CORE, requiredAmount + extraAmount);

		thisDialog.getVisualPanel().showLoot(cacheString.get("cache_title"), salvage, false, true, true, new CoreInteractionListener() {
			@Override
			public void coreUIDismissed() {
				//thisDialog.dismiss();
				//thisDialog.hideTextPanel();
				//thisDialog.hideVisualPanel();
				thisDialog.getTextPanel().addPara(cacheString.get("cache_salvage_text"));
				Misc.fadeAndExpire(cache);

				looted = true;
			}
		});

		options.clearOptions();
		options.addOption(cacheString.get("leave"), "defaultLeave");
		thisDialog.setPromptText("");
		return true;
	}

	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {

		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;

		if (mode == ListInfoMode.IN_DESC) {
			initPad = opad;
		}
		FactionAPI faction = getFactionForUIColors();

		bullet(info);
		if (isUpdate) {
			// 3 possible updates: de-posted/expired, failed, completed
			if (!isMissionValid()) {
				return;
			} else if (isEnding()) {
				info.addPara(strings.get("received"), initPad, tc, h, Misc.getDGSCredits(reward));
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, 0f);
				}
			}
		} else {
			// either in small description, or in tooltip/intel list
			if (isEnding()) {
				info.addPara(strings.get("received"), initPad, tc, h, Misc.getDGSCredits(reward));
				initPad = 0f;
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, initPad);
				}
			} else if (isMissionValid()) {
				if (mode != ListInfoMode.IN_DESC) {
					info.addPara(strings.get("location"), initPad, tc, h, cacheSystem.getNameWithLowercaseType());
					initPad = 0f;
				}

				LabelAPI label = info.addPara(String.format(strings.get("brief"), market.getName()), initPad, tc, h);
				label.setHighlight(market.getName());
				label.setHighlightColors(market.getFaction().getBaseUIColor());
				info.addPara(strings.get("reward"), 0f, tc, h, Misc.getDGSCredits(reward));
				addDays(info, strings.get("to_respond"), timeLeft, tc, 0f);
			}
		}

		unindent(info);
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getSmallDescriptionTitle(), c, 0f);
		addBulletPoints(info, mode);
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;

		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.ALPHA_CORE);
		info.addImage(spec.getIconName(), width, 80, opad);

		addBulletPoints(info, ListInfoMode.IN_DESC);

		if (!isMissionValid()) {
			info.addPara(strings.get("failed"), opad);
		} else if (isEnding()) {
			info.addPara( strings.get("succeed"), opad);
		} else {
			if (!looted) {
				info.addPara(strings.get("information_1"), opad, tc, h, cacheSystem.getNameWithLowercaseType());
			}

			info.addPara(strings.get("information_2"), opad, tc, h, cacheSystem.getNameWithLowercaseType());
			info.addPara(strings.get("information_3"), opad, tc, market.getFaction().getBaseUIColor(), market.getName());
		}
	}

	@Override
	public String getIcon() {
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.ALPHA_CORE);
		return spec.getIconName();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		tags.add(Tags.INTEL_EXPLORATION);
		return tags;
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		return market.getFaction();
	}

	@Override
	public String getSortString() {
		return strings.get("title");
	}

	@Override
	public String getSmallDescriptionTitle() {
		if (isEnded() || isEnding()) {
			return strings.get("title_finished");
		}
		return getSortString();
	}

	@Override
	public String getName() {
		return getSmallDescriptionTitle();
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (looted) {
			return market.getPrimaryEntity();
		}
		return cache;
	}

	@Override
	public String getCommMessageSound() {
		return "ui_discovered_entity";
	}

	private SectorEntityToken spawnCache() {
		WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(random);
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.hasTag(Tags.THEME_CORE_POPULATED)) continue;
			if (system.hasTag(Tags.THEME_HIDDEN)) continue;

			if (system.hasTag(Tags.THEME_REMNANT_MAIN)) continue;
			if (system.hasTag(Tags.THEME_REMNANT_RESURGENT)) continue;

			picker.add(system);
		}

		int attempt = 0;
		StarSystemAPI system = null;
		EntityLocation loc = null;
		while (loc == null && attempt < 100) {
			system = picker.pick();
			if (system == null) {
				return null;
			}

			loc = BaseThemeGenerator.pickHiddenLocationNotNearStar(random, system, 100f, null);
			attempt++;
		}

		if (loc == null) {
			return null;
		}

		AddedEntity added = BaseThemeGenerator.addEntity(random, system, loc, Entities.SUPPLY_CACHE, Factions.NEUTRAL);
		if (added == null || added.entity == null) {
			return null;
		}

		added.entity.setDiscoverable(null);
		added.entity.setDiscoveryXP(null);
		added.entity.setSensorProfile(null);

		added.entity.addTag(Tags.EXPIRES); // so it doesn't get targeted by "analyze entity" missions
		return added.entity;
	}

	private void spawnEnemyToInvestigate(SectorEntityToken locToken, float fp) {
		PatrolType type;
		if (fp < AssembleStage.FP_SMALL * 1.5f) {
			type = PatrolType.FAST;
		} else if (fp < AssembleStage.FP_MEDIUM * 1.5f) {
			type = PatrolType.COMBAT;
		} else {
			type = PatrolType.HEAVY;
		}

		FleetParamsV3 params = new FleetParamsV3(null, locToken.getLocation(), Factions.LUDDIC_PATH, null, type.getFleetType(), fp, // combatPts
				fp * 0.1f, // freighterPts
				fp * 0.1f, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f // qualityMod
		);

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		if (fleet.isEmpty()) {
			fleet = null;
		}

		if (fleet != null) {
			fleet.addScript(new AutoDespawnScript(fleet));

			fleet.setTransponderOn(false);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_STICK_WITH_PLAYER_IF_ALREADY_TARGET, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);

			Vector2f loc = Misc.getPointAtRadius(locToken.getLocation(), 500f);
			locToken.getContainingLocation().addEntity(fleet);
			fleet.setLocation(loc.x, loc.y);

			fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, locToken, 10000f);
		}
	}
}