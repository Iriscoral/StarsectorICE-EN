package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.MissionCompletionRep;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SUN_ICE_MissionRecruitmentShipsIntel extends BaseIntelPlugin {

	private final MarketAPI market;
	private final String commodityId = Commodities.SHIPS;
	private final List<FleetMemberAPI> selectedShips = new ArrayList<>();
	private final List<FleetMemberAPI> selectedExtra = new ArrayList<>();
	private final int requiredPts;
	private final int morePts;

	private float timeLeft;
	private float repGot = 0f;
	private int reward = 0;

	public SUN_ICE_MissionRecruitmentShipsIntel(InteractionDialogAPI dialog, int requiredPts, int morePts, float timeOutDuration) {
		this.market = dialog.getInteractionTarget().getMarket();
		this.requiredPts = requiredPts;
		this.morePts = morePts;

		this.timeLeft = timeOutDuration;

		setImportant(true);
		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
	}

	private String getString(String key) {
		return Global.getSettings().getString("Intel", "SUN_ICE_ship_recruitment_" + key);
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

	public boolean performDelivery(InteractionDialogAPI dialog) {

		boolean extra = !selectedExtra.isEmpty();
		Random random = new Random();

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CampaignFleetAPI exiled = (CampaignFleetAPI) market.getPrimaryEntity();
		for (FleetMemberAPI member : selectedShips) {
			exiled.getFleetData().addFleetMember(member);
			member.getStats().getDynamic().getMod("NO_RANDOM").modifyFlat("NO_RANDOM", 1f);

			reward += member.getBaseValue() * 1.5f;
			if (Math.random() > 0.5) {
				int level = MathUtils.getRandomNumberInRange(2, 4);
				OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);
				PersonAPI officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), level, pref, random);
				member.setCaptain(officer);
			}
		}

		for (FleetMemberAPI member : selectedExtra) {
			exiled.getFleetData().addFleetMember(member);
			member.getStats().getDynamic().getMod("NO_RANDOM").modifyFlat("NO_RANDOM", 1f);

			if (Math.random() > 0.5) {
				int level = MathUtils.getRandomNumberInRange(2, 4);
				OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);
				PersonAPI officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), level, pref, random);
				member.setCaptain(officer);
			}
		}

		FleetMemberAPI abraxas = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_abraxas_Assault");
		abraxas.setCaptain(SUN_ICE_MissionManager.getPriest());
		abraxas.setShipName("ICS Kick Boxing");
		abraxas.getRepairTracker().setCR(1f);
		exiled.getFleetData().addFleetMember(abraxas);

		CargoAPI cargo = playerFleet.getCargo();
		cargo.getCredits().add(reward);
		AddRemoveCommodity.addCreditsGainText(reward, dialog.getTextPanel());

		float repAmount = extra ? 0.05f : 0.03f;

		MissionCompletionRep completionRep = new MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.MISSION_SUCCESS, completionRep, null, dialog.getTextPanel(), true, true), getFactionForUIColors().getId());

		selectedShips.clear();
		repGot = repAmount;
		endAfterDelay();

		exiled.updateCounts();
		exiled.getFleetData().sort();
		exiled.forceSync();

		return extra;
	}

	public boolean tryDeliverExtra(List<FleetMemberAPI> toDeliver, float requiredPts, float morePts) {
		selectedExtra.clear();

		int pts = 0;
		for (FleetMemberAPI member : toDeliver) {
			pts += member.getBaseDeploymentCostSupplies();
			Global.getLogger(this.getClass()).info("Deliver extra check:" + member.getHullId() + ", pts:" + member.getBaseDeploymentCostSupplies());
		}

		if (pts < requiredPts) {
			return false;
		}
		if (pts > requiredPts + morePts) {
			return false;
		}

		selectedExtra.addAll(toDeliver);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		for (FleetMemberAPI member : selectedExtra) {
			if (member.getCaptain() != null) {
				member.setCaptain(null);
			}
			playerFleet.getFleetData().removeFleetMember(member);
		}

		return true;
	}

	public boolean tryDeliver(List<FleetMemberAPI> toDeliver) {
		selectedShips.clear();

		int pts = 0;
		for (FleetMemberAPI member : toDeliver) {
			pts += member.getBaseDeploymentCostSupplies();
			Global.getLogger(this.getClass()).info("Deliver check:" + member.getHullId() + ", pts:" + member.getBaseDeploymentCostSupplies());
		}

		if (pts < requiredPts) {
			return false;
		}
		if (pts > requiredPts + morePts) {
			return false;
		}

		selectedShips.addAll(toDeliver);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		for (FleetMemberAPI member : selectedShips) {
			if (member.getCaptain() != null) {
				member.setCaptain(null);
			}
			playerFleet.getFleetData().removeFleetMember(member);
		}

		return true;
	}

	public boolean checkedShipReadyToDeliver() {
		return !selectedShips.isEmpty();
	}

	private void createPenaltyResult() {
		float repAmount = 0.05f;
		MissionCompletionRep completionRep = new MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.MISSION_FAILURE, completionRep, null, null, true, false), getFactionForUIColors().getId());
	}

	public void delayLimit(float delay) {
		timeLeft += delay;
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
				info.addPara(getString("received"), initPad, tc, h, Misc.getDGSCredits(reward));
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, 0f);
				}
			}
		} else {
			// either in small description, or in tooltip/intel list
			if (isEnding()) {
				info.addPara(getString("received"), initPad, tc, h, Misc.getDGSCredits(reward));
				initPad = 0f;
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, initPad);
				}
			} else if (isMissionValid()) {
				if (mode != ListInfoMode.IN_DESC) {
					info.addPara(getString("faction"), initPad, tc, faction.getBaseUIColor(), faction.getDisplayName());
					initPad = 0f;
				}

				LabelAPI label = info.addPara(String.format(getString("brief"), "" + requiredPts, market.getName()), initPad, tc, h);
				label.setHighlight("" + requiredPts, market.getName());
				label.setHighlightColors(h, market.getFaction().getBaseUIColor());
				info.addPara(getString("brief_sub"), 0f, tc, h, "" + (requiredPts + morePts));
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
	public FactionAPI getFactionForUIColors() {
		return market.getFaction();
	}

	private CommodityOnMarketAPI getCommodity() {
		return market.getCommodityData(commodityId);
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;

		FactionAPI faction = getFactionForUIColors();

		CommodityOnMarketAPI com = getCommodity();
		info.addImages(width, 80, opad, opad * 2f, com.getCommodity().getIconName(), market.getFaction().getCrest());

		info.addPara(getString("detail"), opad, faction.getBaseUIColor(), market.getName());
		if (!isMissionValid()) {
			info.addPara(getString("failed"), opad);
		} else if (isEnding()) {
			info.addPara(getString("succeed"), opad);
		} else {
			addBulletPoints(info, ListInfoMode.IN_DESC);

			info.addPara(getString("information_1"), opad, faction.getBaseUIColor(), market.getName());
		}
	}

	@Override
	public String getIcon() {
		return getCommodity().getCommodity().getIconName();
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