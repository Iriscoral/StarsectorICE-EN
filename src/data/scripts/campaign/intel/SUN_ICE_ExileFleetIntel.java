package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import data.scripts.world.SUN_ICE_ExileFleetFakeAI.ExileState;
import data.scripts.world.SUN_ICE_ExileFleetManager;

import java.awt.Color;
import java.util.Set;

public class SUN_ICE_ExileFleetIntel extends BaseIntelPlugin {

	private static final String TRACK_KEY = "SUN_ICE_ExileFleetIntel_Track";
	private final SUN_ICE_ExileFleetManager manager;
	private final FactionAPI faction;

	private float duration = 60f;
	private ExileState lastState = ExileState.START;
	private StarSystemAPI startingSystem;
	private StarSystemAPI currentSystem;

	public SUN_ICE_ExileFleetIntel(SUN_ICE_ExileFleetManager manager) {
		this.manager = manager;
		this.faction = manager.getICE();
		this.startingSystem = SUN_ICE_Data.getIdoneusCitadel().getStarSystem();
		this.currentSystem = startingSystem;

		Global.getSector().getIntelManager().queueIntel(this);
		Global.getLogger(this.getClass()).info("Exile Fleet Intel Active.");
	}

	@Override
	public void reportMadeVisibleToPlayer() {
		if (!isEnding() && !isEnded()) {
			duration = Math.max(duration * 0.5f, Math.min(duration * 2f, 60f));
		}
	}

	@Override
	public void advanceImpl(float amount) {
		ExileState state = manager.getCurrentState();

		if (state == null || state.getId() == 0) {
			return;
		}
		if (state == ExileState.START) {
			currentSystem = SUN_ICE_Data.getIdoneusCitadel().getStarSystem();
		} else {
			currentSystem = manager.getCurrentSystemDestination();
		}

		if (lastState != state) {
			Global.getLogger(this.getClass()).info("Exile Fleet Intel State Change from " + lastState + " to " + state);
			lastState = state;

			if (lastState.shouldDisplayBy(faction)) {
				sendUpdateIfPlayerHasIntel(new Object(), false);
			}

			if (lastState.isEnd()) {
				Global.getLogger(this.getClass()).info("Exile Fleet Intel Done.");

				manager.getICE().getMemoryWithoutUpdate().set("$SUN_ICE_isMissionOnGoing", false);
				endAfterDelay();
			}
		}
	}

	@Override
	protected void notifyEnding() {
		super.notifyEnding();
		Global.getSector().getListenerManager().removeListener(this);
	}

	@Override
	public String getSmallDescriptionTitle() {
		ExileState stateForInfo = lastState;
		if (!stateForInfo.shouldDisplayBy(faction)) {
			stateForInfo = ExileState.START;
		}

		return strings.get("title" + stateForInfo.getId());
	}

	@Override
	public String getName() {
		return getSmallDescriptionTitle();
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color h = Misc.getHighlightColor();
		Color t = Misc.getTextColor();
		Color c = getTitleColor(mode);
		float pad = 3f;

		info.addPara(getSmallDescriptionTitle(), c, 0f);

		ExileState stateForInfo = lastState;
		if (!stateForInfo.shouldDisplayBy(faction)) {
			stateForInfo = ExileState.START;
		}

		String key = "intel" + stateForInfo.getId();
		switch (stateForInfo) {
			case START:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle());
				break;
			case STAY:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle(), manager.getCurrentSystemDestination().getName());
				break;
			case TRAVELING:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle(), manager.getCurrentSystemDestination().getName());
				break;
			case DEAD:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle());
				break;
			case SETTLE:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle(), manager.getFinalSettle().getName());
				break;
			case FINISHED:
				info.addPara(strings.get(key), pad, t, h, manager.getICE().getDisplayNameWithArticle(), manager.getFinalSettle().getName());
				break;
			default:
				break;
		}
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		float opad = 10f;

		info.addImage(manager.getICE().getLogo(), width, 128, opad);

		FactionAPI player = Global.getSector().getPlayerFaction();
		ExileState stateForInfo = lastState;
		if (!stateForInfo.shouldDisplayBy(faction)) {
			stateForInfo = ExileState.START;
		}

		String key = "des" + stateForInfo.getId();
		switch (stateForInfo) {
			case START:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				if (player.isAtWorst(faction, SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE1)) {
					info.addPara(strings.get(key + "_high"), opad, h, manager.getCurrentSystemDestination().getName());
				}
				break;
			case STAY:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				break;
			case TRAVELING:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				break;
			case DEAD:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				break;
			case SETTLE:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				break;
			case FINISHED:
				info.addPara(strings.get(key), opad, h, currentSystem.getName());
				break;
			default:
				break;
		}

		if (stateForInfo.isEnd()) return;
		if (player.isAtWorst(faction, SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE2)) {
			info.addPara(strings.get("rep"), opad * 2f, h, faction.getDisplayName(), faction.getRelationshipLevel(player).getDisplayName(), manager.getExiledFleet().getName());

			ButtonAPI button = info.addButton(strings.get("track"), TRACK_KEY, 200f, 25f, opad);
			button.setEnabled(manager.getExiledFleet() != null && manager.getExiledFleet().isAlive());
		}

	}

	public static final I18nSection strings = I18nSection.getInstance("Intel", "SUN_ICE_fleet_");

	@Override
	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == TRACK_KEY) {
			Global.getSector().layInCourseFor(manager.getExiledFleet());
		}
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		FactionAPI player = Global.getSector().getPlayerFaction();
		if (player.isAtWorst(faction, SUN_ICE_Data.REP_FOR_FLEET_INFO_STAGE1)) {
			return currentSystem.getHyperspaceAnchor();
		}

		return startingSystem.getHyperspaceAnchor();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		tags.add(getFactionForUIColors().getId());
		return tags;
	}

	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("intel", "sun_ice_exile");
	}
}
