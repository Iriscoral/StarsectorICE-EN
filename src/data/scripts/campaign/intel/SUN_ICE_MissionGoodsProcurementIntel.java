package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.MissionCompletionRep;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.awt.Color;
import java.util.*;

public class SUN_ICE_MissionGoodsProcurementIntel extends BaseIntelPlugin {

	private final Map<String, Integer> requiredCommodity;
	private final MarketAPI market;

	private final Map<String, Integer> providedCommodity;
	private float timeLeft;
	private float repGot = 0f;
	private int reward = 0;

	public SUN_ICE_MissionGoodsProcurementIntel(InteractionDialogAPI dialog, Map<String, Integer> requiredCommodity, float timeOutDuration) {
		this.requiredCommodity = new HashMap<>(requiredCommodity);
		this.providedCommodity = new HashMap<>(requiredCommodity);
		for (String key : this.providedCommodity.keySet()) {
			this.providedCommodity.put(key, 0);
		}

		this.market = dialog.getInteractionTarget().getMarket();
		for (String id : requiredCommodity.keySet()) {
			int amount = requiredCommodity.get(id);
			this.reward += (int) (Global.getSettings().getCommoditySpec(id).getBasePrice() * amount * 1.5f);
		}

		this.timeLeft = timeOutDuration;

		setImportant(true);
		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
	}

	public static final I18nSection strings = I18nSection.getInstance("Intel", "SUN_ICE_goods_procurement_");

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

	public void deliver(String cargo, int amount) {
		providedCommodity.put(cargo, providedCommodity.get(cargo) + amount);
	}

	public Set<String> getRequirements() {
		return requiredCommodity.keySet();
	}

	public int getRequired(String cargo) {
		return requiredCommodity.get(cargo);
	}

	public int getProvided(String cargo) {
		return providedCommodity.get(cargo);
	}

	public int getShortage(String cargo) {
		return getRequired(cargo) - getProvided(cargo);
	}

	public boolean isAllDelivered() {
		for (String key : requiredCommodity.keySet()) {
			if (getShortage(key) > 0) return false;
		}

		return true;
	}

	public void performDelivery(InteractionDialogAPI dialog, boolean extra) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CargoAPI cargo = playerFleet.getCargo();

		if (extra) {
			reward /= 1.5f;
			reward /= 2f;
		}

		cargo.getCredits().add(reward);
		AddRemoveCommodity.addCreditsGainText(reward, dialog.getTextPanel());

		float repAmount = extra ? 0.05f : 0.03f;
		MissionCompletionRep completionRep = new MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.MISSION_SUCCESS, completionRep, null, dialog.getTextPanel(), true, true), getFactionForUIColors().getId());

		repGot = repAmount;
		endAfterDelay();
	}

	private void createPenaltyResult() {
		float repAmount = 0.05f;
		MissionCompletionRep completionRep = new MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.MISSION_FAILURE, completionRep, null, null, true, false), getFactionForUIColors().getId());
	}

	@Override
	public boolean runWhilePaused() {
		return false;
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
					info.addPara(strings.get("faction"), initPad, tc, faction.getBaseUIColor(), faction.getDisplayName());
					initPad = 0f;
				}

				LabelAPI label = info.addPara(String.format(strings.get("brief"), market.getName()), initPad, tc, h);
				label.setHighlight(market.getName());
				label.setHighlightColors(market.getFaction().getBaseUIColor());
				for (String id : requiredCommodity.keySet()) {
					int provided = getProvided(id);
					int required = getRequired(id);
					info.addPara(strings.get("brief_sub"), 0f, tc, h, String.valueOf(provided), String.valueOf(required), market.getCommodityData(id).getCommodity().getLowerCaseName());
				}

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
	public FactionAPI getFactionForUIColors() {
		return market.getFaction();
	}

	private final List<CommodityOnMarketAPI> com = new ArrayList<>();

	private List<CommodityOnMarketAPI> getCommodities() {
		if (com.isEmpty()) {
			for (String id : requiredCommodity.keySet()) {
				com.add(market.getCommodityData(id));
			}
		}

		return com;
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;

		FactionAPI faction = getFactionForUIColors();

		List<String> iconNames = new ArrayList<>();
		for (CommodityOnMarketAPI com : getCommodities()) {
			iconNames.add(com.getCommodity().getIconName());
		}

		List<String> front = iconNames.subList(0, iconNames.size() / 2);
		List<String> back = iconNames.subList(iconNames.size() / 2, iconNames.size());

		String[] arrayFront = front.toArray(new String[0]);
		String[] arrayBack = back.toArray(new String[0]);
		info.addImages(width, 80, opad, opad * 2f, market.getFaction().getCrest());
		info.addImages(width, 80, opad, opad * 2f, arrayFront);
		info.addImages(width, 80, opad, opad * 2f, arrayBack);

		info.addPara(strings.get("detail"), opad, faction.getBaseUIColor(), market.getName());
		if (!isMissionValid()) {
			info.addPara(strings.get("failed"), opad);
		} else if (isEnding()) {
			info.addPara(strings.get("succeed"), opad);
		} else {
			addBulletPoints(info, ListInfoMode.IN_DESC);

			info.addPara(strings.get("information_1"), opad, faction.getBaseUIColor(), market.getName());
		}
	}

	@Override
	public String getIcon() {
		return getCommodities().get(0).getCommodity().getIconName();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		tags.add(Tags.INTEL_TRADE);
		tags.add(getFactionForUIColors().getId());
		return tags;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return market.getPrimaryEntity();
	}
}