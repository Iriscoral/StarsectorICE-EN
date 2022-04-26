package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_Data;

import java.awt.*;
import java.util.Random;
import java.util.Set;

public class SUN_ICE_GoToFleetIntel extends BaseIntelPlugin {

	private final Random random = new Random();
	private final MarketAPI market;

	private float timeLeft;

	public SUN_ICE_GoToFleetIntel(InteractionDialogAPI dialog, float timeOutDuration) {

		this.timeLeft = timeOutDuration;
		this.market = SUN_ICE_Data.getExileManager().getExiledFleet().getMarket();

		setImportant(true);
		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
	}

	private String getString(String key) {
		return Global.getSettings().getString("Intel", "SUN_ICE_fleet_call_" + key);
	}

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

	public void performDelivery() {
		endAfterDelay();
	}

	private void createPenaltyResult() {
		float repAmount = 0.05f;
		CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_FAILURE, completionRep, null, null, true, false), getFactionForUIColors().getId());
	}

	public void forceFailed() {
		timeLeft = -1f;
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
				return;
			}
		} else {
			// either in small description, or in tooltip/intel list
			if (isEnding()) {
				initPad = 0f;
			} else if (isMissionValid()) {
				if (mode != ListInfoMode.IN_DESC) {
					info.addPara(getString("location"), initPad, tc, h, market.getContainingLocation().getName());
					initPad = 0f;
				}

				LabelAPI label = info.addPara(String.format(getString("brief"), market.getPrimaryEntity().getName()), initPad, tc, h);
				label.setHighlight(market.getPrimaryEntity().getName());
				label.setHighlightColors(market.getFaction().getBaseUIColor());
				addDays(info, getString("to_respond"), timeLeft, tc, 0f);
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

		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.CREW);
		info.addImage(spec.getIconName(), width, 80, opad);

		addBulletPoints(info, ListInfoMode.IN_DESC);

		if (!isMissionValid()) {
			info.addPara(getString("failed"), opad);
		} else if (isEnding()) {
			info.addPara(getString("succeed"), opad);
		} else {
			info.addPara(getString("information_1"), opad, tc, h, market.getPrimaryEntity().getName());
		}

	}

	@Override
	public String getIcon() {
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.CREW);
		return spec.getIconName();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		return tags;
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		return market.getFaction();
	}

	@Override
	public String getSortString() {
		return getString("title");
	}

	@Override
	public String getSmallDescriptionTitle() {
		if (isEnded() || isEnding()) {
			return getString("title_finished");
		}
		return getSortString();
	}

	@Override
	public String getName() {
		return getSmallDescriptionTitle();
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return market.getPrimaryEntity();
	}
}